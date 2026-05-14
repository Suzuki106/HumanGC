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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HumanRateService {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private PaperFeatureMapper paperFeatureMapper;

    // Feature definitions with name and weight
    private static final Map<String, Double> FEATURE_WEIGHTS = new LinkedHashMap<>();
    static {
        FEATURE_WEIGHTS.put("错别字/typo", 0.6);
        FEATURE_WEIGHTS.put("半截句", 0.7);
        FEATURE_WEIGHTS.put("官话堆砌", 0.8);
        FEATURE_WEIGHTS.put("口语化", 0.5);
        FEATURE_WEIGHTS.put("长难句", 0.7);
        FEATURE_WEIGHTS.put("逻辑跳跃", 0.6);
        FEATURE_WEIGHTS.put("排版混乱", 0.5);
        FEATURE_WEIGHTS.put("引用混乱", 0.5);
        FEATURE_WEIGHTS.put("术语堆砌", 0.6);
    }

    // Common Chinese character confusions for typo detection
    private static final List<String[]> TYPO_PAIRS = Arrays.asList(
            new String[]{"的", "地"},
            new String[]{"的", "得"},
            new String[]{"在", "再"},
            new String[]{"做", "作"},
            new String[]{"了", "啦"},
            new String[]{"那", "哪"},
            new String[]{"么", "吗"},
            new String[]{"他", "她"},
            new String[]{"它", "他"},
            new String[]{"象", "像"},
            new String[]{"长", "常"},
            new String[]{"须", "需"},
            new String[]{"即", "既"},
            new String[]{"到", "道"},
            new String[]{"己", "已"}
    );

    // Academic cliché patterns
    private static final List<String> ACADEMIC_CLICHES = Arrays.asList(
            "综上所述", "值得注意的是", "具有重要意义", "不可否认",
            "显而易见", "众所周知", "换言之", "进一步而言",
            "基于此", "由此可见", "从根本上说", "必须指出",
            "毋庸置疑", "概而言之", "总体而言", "从某种意义上说",
            "不言而喻", "显而易见地", "毫无疑问", "特别值得关注的是",
            "需要强调的是", "在此背景下", "有鉴于此", "究其原因"
    );

    // Colloquial words
    private static final List<String> COLLOQUIAL_WORDS = Arrays.asList(
            "就是说", "然后然后", "好吧", "反正", "那个",
            "的话", "什么的", "嘛", "啦", "呀",
            "呗", "哦", "呃", "嗯", "哈",
            "挺", "蛮", "超", "真的", "其实吧",
            "说实话", "说白了", "讲真", "老实说", "你懂的",
            "差不多", "那种", "这样子", "基本上", "大概吧"
    );

    // Transition words (for logic jump detection)
    private static final List<String> TRANSITION_WORDS = Arrays.asList(
            "因此", "所以", "然而", "但是", "此外",
            "另外", "同时", "另一方面", "首先", "其次",
            "最后", "接着", "进而", "不仅如此", "尽管如此",
            "反之", "相比之下", "总之", "综上", "换言之",
            "也就是说", "具体来说", "例如", "比如", "其中"
    );

    // Citation patterns that are malformed/mixed
    private static final Pattern CITATION_PATTERN_1 = Pattern.compile("\\[\\d+\\]");
    private static final Pattern CITATION_PATTERN_2 = Pattern.compile("\\([^)]*\\d{4}[^)]*\\)");
    private static final Pattern CITATION_PATTERN_3 = Pattern.compile("\\[\\d+[,\\-]\\s*\\d+\\]");

    // Jargon density terms
    private static final List<String> JARGON_TERMS = Arrays.asList(
            "范式", "维度", "赋能", "闭环", "抓手", "底层逻辑",
            "顶层设计", "颗粒度", "方法论", "认知", "迭代",
            "矩阵", "生态", "链路", "路径", "架构",
            "模型", "框架", "机制", "体系", "因子",
            "变量", "参数", "阈值", "权重", "耦合"
    );

    @Transactional
    public DetectResponse calculateHumanRate(Long paperId) {
        log.info("Calculating human rate for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("Paper not found: " + paperId);
        }

        String text = paper.getOriginalText();
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Paper text is empty");
        }

        // Split text into sentences and paragraphs
        List<String> sentences = splitSentences(text);
        List<String> paragraphs = splitParagraphs(text);

        // Detect features
        List<FeatureResult> features = new ArrayList<>();
        double totalScore = 0;
        double maxPossibleScore = 0;

        for (Map.Entry<String, Double> entry : FEATURE_WEIGHTS.entrySet()) {
            String featureName = entry.getKey();
            double weight = entry.getValue();

            int triggerCount = detectFeature(featureName, text, sentences, paragraphs);
            double featureScore = weight * triggerCount;
            totalScore += featureScore;
            // max possible: weight * max reasonable triggers (estimate: sentence count * upper bound)
            maxPossibleScore += weight * Math.max(sentences.size(), 10);

            features.add(new FeatureResult(featureName, triggerCount, BigDecimal.valueOf(featureScore).setScale(2, RoundingMode.HALF_UP)));
        }

        // Normalize to 0-100%. Invert: more triggers = more "human"
        double humanRate = Math.min((totalScore / maxPossibleScore) * 100, 99.9);
        humanRate = Math.max(humanRate, 0.1);
        BigDecimal rate = BigDecimal.valueOf(humanRate).setScale(1, RoundingMode.HALF_UP);

        // Generate summary
        String summary = generateSummary(rate, features);

        // Update paper with human rate
        paper.setHumanRate(rate);
        paperMapper.updateById(paper);

        // Save feature results
        LambdaQueryWrapper<PaperFeature> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PaperFeature::getPaperId, paperId);
        paperFeatureMapper.delete(deleteWrapper);

        for (FeatureResult fr : features) {
            PaperFeature pf = new PaperFeature();
            pf.setPaperId(paperId);
            pf.setFeatureName(fr.getFeatureName());
            pf.setTriggerCount(fr.getTriggerCount());
            pf.setScore(fr.getScore());
            paperFeatureMapper.insert(pf);
        }

        log.info("Human rate calculated: {}% for paper id={}", rate, paperId);

        return new DetectResponse(paperId, rate, features, summary);
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split("[。！？；\\n]+");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                sentences.add(part);
            }
        }
        return sentences;
    }

    private List<String> splitParagraphs(String text) {
        List<String> paragraphs = new ArrayList<>();
        String[] parts = text.split("\\n\\s*\\n");
        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                paragraphs.add(part);
            }
        }
        return paragraphs;
    }

    private int detectFeature(String featureName, String text, List<String> sentences, List<String> paragraphs) {
        switch (featureName) {
            case "错别字/typo":
                return detectTypos(text);
            case "半截句":
                return detectHalfSentences(sentences);
            case "官话堆砌":
                return detectOfficialJargon(text);
            case "口语化":
                return detectColloquial(text);
            case "长难句":
                return detectLongSentences(sentences);
            case "逻辑跳跃":
                return detectLogicJumps(sentences);
            case "排版混乱":
                return detectFormattingIssues(text, paragraphs);
            case "引用混乱":
                return detectCitationChaos(text);
            case "术语堆砌":
                return detectJargonDensity(paragraphs);
            default:
                return 0;
        }
    }

    /**
     * Detects common Chinese character confusions.
     */
    private int detectTypos(String text) {
        int count = 0;
        for (String[] pair : TYPO_PAIRS) {
            // Check if either character appears in contexts where it might be wrong
            // We count occurrences as potential typos
            for (int i = 0; i < text.length() - 1; i++) {
                String charStr = String.valueOf(text.charAt(i));
                if (charStr.equals(pair[0]) || charStr.equals(pair[1])) {
                    // Only count a fraction to avoid over-counting
                    count++;
                }
            }
        }
        // Normalize: divide by a factor to get reasonable trigger count
        return Math.min(count / 20, 50);
    }

    /**
     * Detects sentences that end abruptly without completion.
     */
    private int detectHalfSentences(List<String> sentences) {
        int count = 0;
        for (String sentence : sentences) {
            // Sentences that are very short and end with commas or without ending punctuation
            if (sentence.length() < 15 || sentence.endsWith("，") || sentence.endsWith(",")
                    || sentence.endsWith("、") || sentence.endsWith("等等")
                    || sentence.endsWith("…") || sentence.endsWith("...")) {
                count++;
            }
            // Sentences that start with "比如", "例如" but are incomplete
            if ((sentence.startsWith("比如") || sentence.startsWith("例如") || sentence.startsWith("像"))
                    && sentence.length() < 20) {
                count++;
            }
        }
        return count;
    }

    /**
     * Detects academic cliché phrases.
     */
    private int detectOfficialJargon(String text) {
        int count = 0;
        for (String cliche : ACADEMIC_CLICHES) {
            int idx = 0;
            while ((idx = text.indexOf(cliche, idx)) != -1) {
                count++;
                idx += cliche.length();
            }
        }
        return count;
    }

    /**
     * Detects colloquial expressions.
     */
    private int detectColloquial(String text) {
        int count = 0;
        for (String word : COLLOQUIAL_WORDS) {
            int idx = 0;
            while ((idx = text.indexOf(word, idx)) != -1) {
                count++;
                idx += word.length();
            }
        }
        return count;
    }

    /**
     * Detects long/complex sentences (>100 chars or multiple nested clauses).
     */
    private int detectLongSentences(List<String> sentences) {
        int count = 0;
        for (String sentence : sentences) {
            if (sentence.length() > 100) {
                count++;
            } else if (sentence.length() > 60) {
                // Check for nested clauses: multiple commas indicating complexity
                long commaCount = sentence.chars().filter(ch -> ch == '，' || ch == ',').count();
                if (commaCount >= 3) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Detects logic jumps between consecutive sentences.
     */
    private int detectLogicJumps(List<String> sentences) {
        int count = 0;
        for (int i = 0; i < sentences.size() - 1; i++) {
            String current = sentences.get(i).trim();
            String next = sentences.get(i + 1).trim();

            // Check if there are NO transition words between consecutive sentences
            boolean hasTransitionInCurrent = false;
            boolean hasTransitionInNext = false;
            for (String tw : TRANSITION_WORDS) {
                if (current.contains(tw)) hasTransitionInCurrent = true;
                if (next.contains(tw)) hasTransitionInNext = true;
            }

            // If neither sentence has a transition word, it's a potential logic jump
            if (!hasTransitionInCurrent && !hasTransitionInNext) {
                // Extra check: if the sentences are very different in length or topic-indicating words
                int lengthDiff = Math.abs(current.length() - next.length());
                if (lengthDiff > 50 || (current.length() < 10 || next.length() < 10)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Detects formatting issues: irregular paragraph lengths, mixed punctuation.
     */
    private int detectFormattingIssues(String text, List<String> paragraphs) {
        int count = 0;

        // Check for mixed Chinese/English punctuation
        boolean hasChineseComma = text.contains("，");
        boolean hasEnglishComma = text.contains(",");
        if (hasChineseComma && hasEnglishComma) {
            count += 2;
        }

        boolean hasChinesePeriod = text.contains("。");
        boolean hasEnglishPeriod = text.contains(".");
        if (hasChinesePeriod && hasEnglishPeriod) {
            count += 2;
        }

        // Check for irregular paragraph lengths
        if (paragraphs.size() > 1) {
            double avgLength = paragraphs.stream().mapToInt(String::length).average().orElse(0);
            for (String para : paragraphs) {
                if (para.length() > avgLength * 2.5 || para.length() < avgLength * 0.3) {
                    count++;
                }
            }
        }

        // Check for consecutive line breaks or strange spacing
        if (text.contains("\n\n\n") || text.contains("   ") || text.contains("\t")) {
            count++;
        }

        return count;
    }

    /**
     * Detects malformed or mixed citation styles.
     */
    private int detectCitationChaos(String text) {
        int count = 0;

        Matcher m1 = CITATION_PATTERN_1.matcher(text);
        while (m1.find()) count++;

        Matcher m2 = CITATION_PATTERN_2.matcher(text);
        while (m2.find()) count++;

        Matcher m3 = CITATION_PATTERN_3.matcher(text);
        while (m3.find()) count++;

        // Also check for English-style in-text citations mixed with Chinese style
        boolean hasBracketStyle = text.matches(".*\\[\\d+\\].*");
        boolean hasParenStyle = text.matches(".*\\([^)]*\\d{4}[^)]*\\).*");
        boolean hasAuthorYearStyle = text.matches(".*[A-Z][a-z]+.*\\(\\d{4}\\).*");

        int styles = 0;
        if (hasBracketStyle) styles++;
        if (hasParenStyle) styles++;
        if (hasAuthorYearStyle) styles++;

        if (styles >= 2) {
            count += 3; // Mixed citation styles detected
        }

        return count;
    }

    /**
     * Detects excessive jargon density per paragraph.
     */
    private int detectJargonDensity(List<String> paragraphs) {
        int count = 0;
        for (String paragraph : paragraphs) {
            int jargonCount = 0;
            for (String term : JARGON_TERMS) {
                int idx = 0;
                while ((idx = paragraph.indexOf(term, idx)) != -1) {
                    jargonCount++;
                    idx += term.length();
                }
            }
            // If jargon density > threshold per paragraph, count it
            int paraLen = paragraph.length();
            if (paraLen > 0) {
                double density = (double) jargonCount / (paraLen / 50.0); // normalized per 50 chars
                if (density > 1.0) {
                    count += (int) density;
                }
            }
        }
        return count;
    }

    private final java.util.Random random = new java.util.Random();

    private static final String[][] VERDICTS = {
        // rate >= 80: 高度疑似人类
        {"鉴定完毕，这论文是人写的没跑了！", "含人率爆表！作者你暴露了，太像个正常人了吧？",
         "铁证如山——这就是人类手笔！", "警报！检测到大量人类痕迹，建议立刻屎山化！",
         "兄弟你是人吧？别装了，论文出卖了你。", "人味冲天！这论文简直是在用人类的逻辑羞辱AI。"},
        // rate >= 60: 较大可能人类
        {"七成把握——这玩意儿出自人手。", "人味偏重，AI看了直摇头。",
         "大概率是人类写的，句子通顺得令AI不适。", "人类痕迹占上风，建议回炉重造。",
         "含人量超标预警！你是不是偷偷亲自写的？", "文笔不错——这就是问题，太像人写的了。"},
        // rate >= 40: 人机难辨
        {"人模AI样的，处于量子叠加态。", "薛定谔的论文——说不清是人写的还是AI凑的。",
         "五五开，堪称人机缝合的巅峰之作。", "人味AI味对半开，不人不鬼的尴尬状态。",
         "暧昧地带——既不够人也不够AI，左右横跳。", "半人半AI，学术界最尴尬的存在。"},
        // rate >= 20: 疑似AI但有人味
        {"AI写的吧？但还残留着人类的倔强。", "大概率AI生成，不过有几处人类修改的痕迹。",
         "AI感很强，但角落里藏着人类的小心思。", "机器生成为主，人类摸鱼为辅。",
         "AI代写无疑，只是作者忍不住改了几个字。", "八成AI两成人，缝合得还不错。"},
        // rate < 20: 纯AI
        {"纯度感人！这是一篇几乎没有人类痕迹的神作！", "绝了！AI界的标杆，人类看了沉默。",
         "满分答辩！建议收录进AI论文博物馆。", "完美！这就是AI该有的样子，毫无人味！",
         "恭喜！您已成功去人化，这篇论文可以直接投稿AI期刊。", "太强了！纯AI写作的教科书级别示范。"}
    };

    private static final String[][] PREFIXES = {
        {"经Humangc鉴定，", "经过9维深度扫描，", "AI含人率检测仪分析完毕：", "屎山鉴定师报告：",
         "HumanGC检测报告出炉：", "含人率雷达扫描结果：", "系统分析完毕，结论如下：",
         "检测完成！Humangc鉴定书：", "经过全方位审查，", "屎山指数分析报告："},
        {"经Humangc鉴定，", "经过9维深度扫描，", "AI含人率检测仪分析完毕：", "屎山鉴定师报告：",
         "HumanGC检测报告出炉：", "含人率雷达扫描结果：", "系统分析完毕，结论如下：",
         "检测完成！Humangc鉴定书：", "经过全方位审查，", "屎山指数分析报告："},
        {"经Humangc鉴定，", "经过9维深度扫描，", "AI含人率检测仪分析完毕：", "屎山鉴定师报告：",
         "HumanGC检测报告出炉：", "含人率雷达扫描结果：", "系统分析完毕，结论如下：",
         "检测完成！Humangc鉴定书：", "经过全方位审查，", "屎山指数分析报告："},
        {"经Humangc鉴定，", "经过9维深度扫描，", "AI含人率检测仪分析完毕：", "屎山鉴定师报告：",
         "HumanGC检测报告出炉：", "含人率雷达扫描结果：", "系统分析完毕，结论如下：",
         "检测完成！Humangc鉴定书：", "经过全方位审查，", "屎山指数分析报告："},
        {"经Humangc鉴定，", "经过9维深度扫描，", "AI含人率检测仪分析完毕：", "屎山鉴定师报告：",
         "HumanGC检测报告出炉：", "含人率雷达扫描结果：", "系统分析完毕，结论如下：",
         "检测完成！Humangc鉴定书：", "经过全方位审查，", "屎山指数分析报告："}
    };

    private static final String[][] FEATURE_INTROS = {
        {" 其中，", " 罪魁祸首是：", " 主要扣分项：", " 关键证据：",
         " 破案线索：", " 人味来源：", " 重点标注：", " 细节披露：",
         " 突出表现为：", " 数据说话："},
    };

    private String pickRandom(String[] arr) {
        return arr[random.nextInt(arr.length)];
    }

    private String generateSummary(BigDecimal rate, List<FeatureResult> features) {
        double r = rate.doubleValue();

        int tier;
        if (r >= 80) tier = 0;
        else if (r >= 60) tier = 1;
        else if (r >= 40) tier = 2;
        else if (r >= 20) tier = 3;
        else tier = 4;

        List<FeatureResult> sortedFeatures = new ArrayList<>(features);
        sortedFeatures.sort((a, b) -> Integer.compare(b.getTriggerCount(), a.getTriggerCount()));

        StringBuilder sb = new StringBuilder();
        sb.append(pickRandom(PREFIXES[tier]));
        sb.append(pickRandom(VERDICTS[tier]));

        if (!sortedFeatures.isEmpty()) {
            sb.append(pickRandom(FEATURE_INTROS[0]));
            int shown = 0;
            for (int i = 0; i < Math.min(4, sortedFeatures.size()); i++) {
                FeatureResult fr = sortedFeatures.get(i);
                if (fr.getTriggerCount() > 0) {
                    if (shown > 0) sb.append("、");
                    sb.append(fr.getFeatureName()).append("×").append(fr.getTriggerCount());
                    shown++;
                }
            }
            if (shown > 0) sb.append("。");
        }

        return sb.toString();
    }
}
