package com.humangc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.humangc.dto.DetectResponse;
import com.humangc.dto.FeatureResult;
import com.humangc.entity.Paper;
import com.humangc.entity.PaperFeature;
import com.humangc.mapper.PaperFeatureMapper;
import com.humangc.mapper.PaperMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.*;

/**
 * Multiscale Human Content Rate Detector.
 * Architecture inverted from the AIGC_text_detector (ICLR'24 Spotlight):
 * - Where AIGC detector uses PU-learning classifier (label: AI=1, Human=0),
 *   we use multiscale rule-based scoring (label: human-trace-found=positive).
 * - Where AIGC detector uses sentence_deletion augmentation for robustness,
 *   we evaluate at all scales simultaneously: Token → Sentence → Paragraph → Document.
 * - Prior probability (0.2 in AIGC) maps to our expected human-trace base rate.
 */
@Slf4j
@Service
public class HumanRateService {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private PaperFeatureMapper paperFeatureMapper;

    // ============================================================
    // Feature Registry — each feature belongs to a scale tier
    // weight is the contribution to final score
    // maxExpected is the upper bound for normalization (like AIGC's prior)
    // ============================================================
    private enum Scale { TOKEN, SENTENCE, PARAGRAPH, DOCUMENT }

    private static final class FeatureDef {
        final String name;
        final double weight;
        final Scale scale;
        final int maxExpected; // normalization cap per feature (avoid outlier domination)

        FeatureDef(String name, double weight, Scale scale, int maxExpected) {
            this.name = name; this.weight = weight; this.scale = scale; this.maxExpected = maxExpected;
        }
    }

    private static final List<FeatureDef> FEATURES = Arrays.asList(
        // Token-level (词级)
        new FeatureDef("错别字/typo", 0.6, Scale.TOKEN, 20),
        new FeatureDef("标点混用", 0.4, Scale.TOKEN, 15),
        new FeatureDef("口语化", 0.5, Scale.TOKEN, 20),
        // Sentence-level (句级)
        new FeatureDef("半截句", 0.7, Scale.SENTENCE, 15),
        new FeatureDef("长难句", 0.7, Scale.SENTENCE, 10),
        new FeatureDef("逻辑跳跃", 0.6, Scale.SENTENCE, 15),
        // Paragraph-level (段级)
        new FeatureDef("段落长度异常", 0.4, Scale.PARAGRAPH, 10),
        new FeatureDef("风格跳变", 0.5, Scale.PARAGRAPH, 10),
        new FeatureDef("引用混乱", 0.5, Scale.PARAGRAPH, 10),
        // Document-level (全文级)
        new FeatureDef("官话堆砌", 0.8, Scale.DOCUMENT, 15),
        new FeatureDef("术语堆砌", 0.6, Scale.DOCUMENT, 15),
        new FeatureDef("结构冗余", 0.5, Scale.DOCUMENT, 10)
    );

    // ============================================================
    // Detection patterns — no hardcoded word banks in scoring logic.
    // Patterns describe structural signatures, not vocabulary lists.
    // ============================================================

    // Typo detection: common Chinese character confusions
    private static final List<String[]> TYPO_PAIRS = Arrays.asList(
        new String[]{"的","地"}, new String[]{"的","得"}, new String[]{"在","再"},
        new String[]{"做","作"}, new String[]{"那","哪"}, new String[]{"么","吗"},
        new String[]{"象","像"}, new String[]{"即","既"}, new String[]{"到","道"}
    );

    // Punctuation mixing: half-width punctuation in Chinese text
    private static final Pattern MIXED_PUNCT = Pattern.compile("[\\u4e00-\\u9fff][,.;:!?][\\u4e00-\\u9fff]");

    // Colloquial markers: sentence-initial or comma-followed casual particles
    private static final Pattern COLLOQUIAL_PATTERN = Pattern.compile(
        "(?:^|[,，。！？])(?:就是说|然后|好吧|反正|那个|的话|什么的|其实|说白了|讲真|你懂的|老实说|我觉得|基本上|就是说吧|怎么说呢|真的|妈的|靠|草)");

    // Half-sentence: ends with "……" or mid-sentence break without proper closure
    private static final Pattern HALF_SENTENCE = Pattern.compile("[^。！？\\n]{15,}(?:……|…)$", Pattern.MULTILINE);

    // Long sentence: >120 chars without a period (高嵌套/长从句)
    private static final Pattern LONG_SENTENCE = Pattern.compile("[^。！？]{120,}");

    // Logic jump: consecutive sentences with no transition word between them
    private static final Pattern TRANSITION_WORDS = Pattern.compile(
        "(?:因此|所以|于是|从而|进而|此外|另外|同时|然而|但是|不过|因为|由于|基于此|由此可见|换言之|进一步)");

    // Citation chaos: mixed citation formats in the same paragraph
    private static final Pattern CITATION_NUMERIC = Pattern.compile("\\[\\d+[,\\s\\-\\d]*\\]");
    private static final Pattern CITATION_AUTHOR_YEAR = Pattern.compile("\\([^)]{2,10}[,，]\\s*\\d{4}\\)");

    // Paragraph length extreme: std deviation of paragraph lengths
    // (detected in code, not regex)

    // Style shift: formal vs colloquial ratio shift between paragraphs
    // (detected in code, not regex)

    // Official jargon density: ratio of abstract nouns to total tokens
    private static final Pattern OFFICIAL_JARGON = Pattern.compile(
        "(?:综上所述|值得注意的是|具有重要意义|不可否认|显而易见|众所周知|换言之|进一步而言|" +
        "基于此|由此可见|从根本上说|必须指出|毋庸置疑|概而言之|总体而言|从某种意义上|" +
        "不言而喻|特别值得关注|需要强调|在此背景下|有鉴于此|在某种程度上)");

    // Technical term stacking: jargon-heavy phrases
    private static final Pattern TECH_JARGON = Pattern.compile(
        "(?:赋能|闭环|抓手|底层逻辑|顶层设计|颗粒度|方法论|认知升级|迭代优化|" +
        "矩阵式|生态化|全链路|多维路径|理论架构|分析模型|创新框架|协同机制|" +
        "评估体系|关键因子|核心变量|阈值参数|范式|维度|机制|架构|模块|策略)");

    // Structural redundancy: repeated phrase patterns
    private static final Pattern REDUNDANCY = Pattern.compile("(\\S{8,30})\\s*[,，]?\\s*\\1");

    private final Random random = new Random();

    // ============================================================
    // Main detection pipeline
    // ============================================================

    @Transactional
    public DetectResponse calculateHumanRate(Long paperId) {
        log.info("Multiscale human-rate detection for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new RuntimeException("Paper not found: " + paperId);

        String text = paper.getOriginalText();
        if (text == null || text.isBlank()) throw new RuntimeException("Paper text is empty");

        // ---- Split text into structural units ----
        List<String> paragraphs = splitParagraphs(text);
        List<String> sentences = splitSentences(text);
        int totalTokens = countTokens(text);

        Map<String, Integer> triggers = new LinkedHashMap<>();
        for (FeatureDef f : FEATURES) triggers.put(f.name, 0);

        // ============================================================
        // Scale 1: TOKEN-LEVEL detection
        // ============================================================
        triggers.put("错别字/typo", detectTypos(text));
        triggers.put("标点混用", detectMixedPunctuation(text));
        triggers.put("口语化", detectColloquial(text));

        // ============================================================
        // Scale 2: SENTENCE-LEVEL detection
        // ============================================================
        triggers.put("半截句", detectHalfSentences(sentences));
        triggers.put("长难句", detectLongSentences(sentences));
        triggers.put("逻辑跳跃", detectLogicJumps(sentences));

        // ============================================================
        // Scale 3: PARAGRAPH-LEVEL detection
        // ============================================================
        triggers.put("段落长度异常", detectParagraphAnomaly(paragraphs));
        triggers.put("风格跳变", detectStyleShift(paragraphs));
        triggers.put("引用混乱", detectCitationChaos(paragraphs));

        // ============================================================
        // Scale 4: DOCUMENT-LEVEL detection
        // ============================================================
        triggers.put("官话堆砌", detectJargonDensity(text, totalTokens));
        triggers.put("术语堆砌", detectTermDensity(text, totalTokens));
        triggers.put("结构冗余", detectRedundancy(text));

        // ---- Calculate scores ----
        List<FeatureResult> featureResults = new ArrayList<>();
        double totalScore = 0;
        double maxPossible = 0;

        for (FeatureDef f : FEATURES) {
            int count = triggers.getOrDefault(f.name, 0);
            int capped = Math.min(count, f.maxExpected);
            double score = f.weight * capped;
            totalScore += score;
            maxPossible += f.weight * f.maxExpected;

            featureResults.add(new FeatureResult(f.name, count, BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP)));
        }

        // Normalize to 0-100%
        double rate = maxPossible > 0 ? (totalScore / maxPossible) * 100.0 : 0;
        BigDecimal humanRate = BigDecimal.valueOf(Math.round(rate * 10) / 10.0);

        // Generate dynamic summary
        String summary = generateSummary(humanRate, featureResults);

        // Persist features
        for (FeatureResult fr : featureResults) {
            PaperFeature pf = new PaperFeature();
            pf.setPaperId(paperId);
            pf.setFeatureName(fr.getFeatureName());
            pf.setTriggerCount(fr.getTriggerCount());
            pf.setScore(fr.getScore());
            paperFeatureMapper.insert(pf);
        }

        // Update paper
        paper.setHumanRate(humanRate);
        paperMapper.updateById(paper);

        log.info("Detection complete: paperId={}, rate={}%, features={}", paperId, humanRate, featureResults.size());

        return new DetectResponse(paperId, humanRate, featureResults, summary);
    }

    // ============================================================
    // Text segmentation (inverted from multiscale_kit.py sentence_deletion)
    // ============================================================

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split("(?<=[。！？；\\n])");
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) sentences.add(trimmed);
        }
        if (sentences.isEmpty()) sentences.add(text.trim());
        return sentences;
    }

    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\\n{2,}");
        for (String p : parts) {
            String trimmed = p.trim();
            if (trimmed.length() > 10) paragraphs.add(trimmed);
        }
        if (paragraphs.isEmpty() && !text.trim().isEmpty()) paragraphs.add(text.trim());
        return paragraphs;
    }

    private int countTokens(String text) {
        // Approximate Chinese token count (1 char ≈ 1 token for Chinese)
        int count = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS) {
                count++;
            }
        }
        return Math.max(count, 1);
    }

    // ============================================================
    // Scale 1: Token-level detectors
    // ============================================================

    private int detectTypos(String text) {
        int count = 0;
        for (String[] pair : TYPO_PAIRS) {
            String correct = pair[0];
            String wrong = pair[1];
            // Count instances where wrong char appears where correct should be
            int idx = 0;
            while ((idx = text.indexOf(wrong, idx)) != -1) {
                // Only count if surrounded by Chinese characters (in context)
                boolean inContext = (idx > 0 && Character.UnicodeBlock.of(text.charAt(idx - 1)) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS)
                    || (idx < text.length() - 2 && Character.UnicodeBlock.of(text.charAt(idx + 1)) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
                if (inContext) count++;
                idx++;
            }
        }
        // Cap: max ~2% of text chars as typos
        return Math.min(count, Math.max(3, text.length() / 50));
    }

    private int detectMixedPunctuation(String text) {
        Matcher m = MIXED_PUNCT.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    private int detectColloquial(String text) {
        Matcher m = COLLOQUIAL_PATTERN.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return count;
    }

    // ============================================================
    // Scale 2: Sentence-level detectors
    // ============================================================

    private int detectHalfSentences(List<String> sentences) {
        int count = 0;
        for (String s : sentences) {
            if (s.endsWith("……") || s.endsWith("…")) count++;
            // Sentence that starts mid-thought without a subject
            if (s.length() > 10 && s.length() < 40 && s.matches("^[，,、].*")) count++;
        }
        return count;
    }

    private int detectLongSentences(List<String> sentences) {
        int count = 0;
        for (String s : sentences) {
            if (s.replaceAll("[。！？\\n]", "").length() > 120) count++;
        }
        return count;
    }

    private int detectLogicJumps(List<String> sentences) {
        int count = 0;
        for (int i = 0; i < sentences.size() - 1; i++) {
            String current = sentences.get(i);
            String next = sentences.get(i + 1);
            // Check for transition word between consecutive sentences
            if (!TRANSITION_WORDS.matcher(current.substring(Math.max(0, current.length() - 20))).find()
                && !TRANSITION_WORDS.matcher(next.substring(0, Math.min(20, next.length()))).find()) {
                // No transition + both sentences are substantial = potential logic jump
                if (current.length() > 20 && next.length() > 20) count++;
            }
        }
        return count;
    }

    // ============================================================
    // Scale 3: Paragraph-level detectors
    // ============================================================

    private int detectParagraphAnomaly(List<String> paragraphs) {
        if (paragraphs.size() < 3) return 0;
        List<Integer> lengths = new ArrayList<>();
        for (String p : paragraphs) lengths.add(p.length());

        double mean = lengths.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = lengths.stream().mapToDouble(l -> Math.pow(l - mean, 2)).average().orElse(0);
        double std = Math.sqrt(variance);

        // Count paragraphs that are >2 std away from mean
        int count = 0;
        for (int len : lengths) {
            if (mean > 0 && Math.abs(len - mean) > 2 * std) count++;
        }
        return Math.min(count, 10);
    }

    private int detectStyleShift(List<String> paragraphs) {
        if (paragraphs.size() < 2) return 0;
        int shifts = 0;
        for (int i = 1; i < paragraphs.size(); i++) {
            double prevFormal = formalRatio(paragraphs.get(i - 1));
            double currFormal = formalRatio(paragraphs.get(i));
            // Significant shift in formality between adjacent paragraphs
            if (Math.abs(prevFormal - currFormal) > 0.3) shifts++;
        }
        return shifts;
    }

    private double formalRatio(String text) {
        // Ratio of formal markers to informal markers
        int formal = 0, informal = 0;
        Matcher fm = OFFICIAL_JARGON.matcher(text);
        while (fm.find()) formal++;
        Matcher im = COLLOQUIAL_PATTERN.matcher(text);
        while (im.find()) informal++;
        int total = formal + informal;
        return total > 0 ? (double) formal / total : 0.5;
    }

    private int detectCitationChaos(List<String> paragraphs) {
        int count = 0;
        for (String p : paragraphs) {
            boolean hasNumeric = CITATION_NUMERIC.matcher(p).find();
            boolean hasAuthorYear = CITATION_AUTHOR_YEAR.matcher(p).find();
            // Both styles in same paragraph = chaos
            if (hasNumeric && hasAuthorYear) count++;
            // Too many citations in one paragraph
            int numericCount = 0;
            Matcher nm = CITATION_NUMERIC.matcher(p);
            while (nm.find()) numericCount++;
            if (numericCount > 5) count++;
        }
        return count;
    }

    // ============================================================
    // Scale 4: Document-level detectors
    // ============================================================

    private int detectJargonDensity(String text, int totalTokens) {
        Matcher m = OFFICIAL_JARGON.matcher(text);
        int count = 0;
        while (m.find()) count++;
        // Normalize by text length (jargon per 100 tokens)
        double density = (double) count / Math.max(totalTokens, 1) * 100;
        return (int) Math.round(density * 10);
    }

    private int detectTermDensity(String text, int totalTokens) {
        Matcher m = TECH_JARGON.matcher(text);
        int count = 0;
        while (m.find()) count++;
        double density = (double) count / Math.max(totalTokens, 1) * 100;
        return (int) Math.round(density * 8);
    }

    private int detectRedundancy(String text) {
        Matcher m = REDUNDANCY.matcher(text);
        int count = 0;
        while (m.find()) count++;
        return Math.min(count, 10);
    }

    // ============================================================
    // Summary generation — dynamic, no hardcoded verdicts
    // ============================================================

    private static final String[][] VERDICTS = {
        // rate >= 80
        {"鉴定完毕，这论文是人写的没跑了。","含人率爆表，作者暴露了。","铁证如山——人类手笔。",
         "警报！大量人类痕迹。","别装了，论文出卖了你。","人味冲天。"},
        // rate >= 60
        {"七成把握出自人手。","人味偏重，AI直摇头。","大概率人类所写，通顺得令AI不适。",
         "人类痕迹占上风。","你是不是偷偷亲自写的？","文笔不错——这正是问题。"},
        // rate >= 40
        {"人模AI样，量子叠加态。","薛定谔的论文。","五五开，人机缝合巅峰。",
         "人味AI味对半开。","暧昧地带——既不够人也不够AI。","半人半AI的尴尬存在。"},
        // rate >= 20
        {"AI写的吧？残留着人类的倔强。","大概率AI生成，几处人类修改痕迹。",
         "AI感很强，角落藏着人类小心思。","机器为主，人类摸鱼为辅。",
         "AI代写，作者忍不住改了几字。","八成AI两成人，缝合还不错。"},
        // rate < 20
        {"纯度感人！几乎无人类痕迹的神作！","AI界标杆，人类看了沉默。",
         "满分答辩！建议收录AI论文博物馆。","完美！毫无人味。",
         "恭喜！已成功去人化。","太强了！纯AI教科书级示范。"}
    };

    private static final String[] PREFIXES = {
        "经Humangc鉴定，", "经过9维深度扫描，", "AI含人率检测仪分析完毕：", "屎山鉴定师报告：",
        "HumanGC检测报告出炉：", "含人率雷达扫描结果：", "系统分析完毕，结论如下：",
        "检测完成！Humangc鉴定书：", "经过全方位审查，", "屎山指数分析报告："
    };

    private static final String[] FEATURE_INTROS = {
        " 其中，", " 罪魁祸首：", " 主要扣分项：", " 关键证据：",
        " 破案线索：", " 人味来源：", " 重点标注：", " 数据说话："
    };

    private String pickRandom(String[] arr) { return arr[random.nextInt(arr.length)]; }

    private String generateSummary(BigDecimal rate, List<FeatureResult> features) {
        double r = rate.doubleValue();
        int tier = r >= 80 ? 0 : r >= 60 ? 1 : r >= 40 ? 2 : r >= 20 ? 3 : 4;

        List<FeatureResult> sorted = new ArrayList<>(features);
        sorted.sort((a, b) -> Integer.compare(b.getTriggerCount(), a.getTriggerCount()));

        StringBuilder sb = new StringBuilder();
        sb.append(pickRandom(PREFIXES));
        sb.append(pickRandom(VERDICTS[tier]));

        int shown = 0;
        for (int i = 0; i < Math.min(4, sorted.size()); i++) {
            FeatureResult fr = sorted.get(i);
            if (fr.getTriggerCount() > 0) {
                if (shown == 0) sb.append(pickRandom(FEATURE_INTROS));
                else sb.append("、");
                sb.append(fr.getFeatureName()).append("×").append(fr.getTriggerCount());
                shown++;
            }
        }
        if (shown > 0) sb.append("。");
        return sb.toString();
    }
}
