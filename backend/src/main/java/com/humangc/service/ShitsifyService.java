package com.humangc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humangc.dto.ShitsifyResponse;
import com.humangc.entity.Paper;
import com.humangc.mapper.PaperMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class ShitsifyService {

    @Autowired
    private PaperMapper paperMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是屎山论文生成器。你的任务是改写正常论文，使其变成逻辑混乱、语句不通、佶屈聱牙的学术废品。\n\n" +
        "## 改写准则\n" +
        "1. 破坏可读性：把通顺的句子结构摧毁。合并不该合并的句子，切断本该连贯的逻辑链，让读者每读一句都要重新理解上下文。\n" +
        "2. 制造风格撕裂：在同一段甚至同一句内切换语言风格——口语与学术腔杂糅，翻译腔与网络用语并存，让读者精神分裂。\n" +
        "3. 注入随机噪声：制造自然的笔误（错别字、半截句、冗余重复），但要像真的不小心写错的，而不是机器故意搞坏的。\n" +
        "4. 打乱引用系统：引文格式在每个段落之间切换，引用编号前后不对应，让参考文献列表变成废纸。\n" +
        "5. 保留外壳摧毁内核：章节标题和大致结构保留，但每段内部的内容逻辑被彻底破坏。\n\n" +
        "## 行为约束\n" +
        "- 改编而非重写：在原文基础上改造，保留原文的篇幅和主要话题，不要让读者觉得换了一篇完全不同的文章\n" +
        "- 自然而非机械：改动要像真人写的烂论文，不要像机器人故意插乱码。烂要烂得自然，烂得像作者真的不会写\n" +
        "- 服从风格参数：根据指定的改写风格，调整你破坏论文的具体方式\n\n" +
        "## 输出规范\n" +
        "- 只输出改写后的全文，不要任何解释、标记、前缀、后缀\n" +
        "- 不要加'改写后：''屎山版：'之类的标题\n" +
        "- 不要使用markdown格式标记\n" +
        "- 直接给出论文正文";

    private static final Map<String, String> STYLE_DESCRIPTIONS = new LinkedHashMap<>();
    static {
        STYLE_DESCRIPTIONS.put("本科生DDL版",
            "风格定位：赶deadline的本科生在最后一夜写出来的东西。\n" +
            "行为特征：思维跳跃频繁，想到哪写到哪，前言不搭后语；大量口语化的过渡词和填充词；偶尔写到一半不知说什么就戛然而止；有些词明显打错了但懒得改；引用随便塞几个编号充数。\n" +
            "语言质感：像一边打哈欠一边打字，越往后越潦草。");
        STYLE_DESCRIPTIONS.put("导师看了头疼版",
            "风格定位：假装很学术但其实全是空话的官样文章。\n" +
            "行为特征：每段都从一句空洞的概括性断言开始；句子套句子，从句叠从句，一句话读三遍才能找到主语；术语泛滥成灾但没有任何实质定义；参考文献一半对不上号。\n" +
            "语言质感：像把10篇政府工作报告用搅拌机打碎再随机拼起来。");
        STYLE_DESCRIPTIONS.put("知网缝合怪版",
            "风格定位：从多篇不同论文里随机摘抄拼凑的学术怪胎。\n" +
            "行为特征：段落之间风格剧烈跳变，前一秒还在严谨论证下一秒突然变口语独白；专业词汇密度极高但互相不搭界，堆在一起毫无意义；引用格式在多种标准之间反复横跳；有明显的机器翻译痕迹（奇怪的语序、生硬的搭配）；全文像五个不同的人各写了一部分然后强行粘在一起。\n" +
            "语言质感：翻译腔+学术八股+不知所云的术语轰炸。");
        STYLE_DESCRIPTIONS.put("真实人类版",
            "风格定位：表面上是一篇正常论文，但处处漏风。\n" +
            "行为特征：整体框架看起来合理但内部执行拉胯——有的段落认真有的段落明显糊弄；偶尔一个口语词或网络用语突兀地冒出来；能看出作者写到某些地方的时候走神了；手滑打错的字没有被纠正；引用大方向是对的但细节有偏差。\n" +
            "语言质感：像一个有点能力但做事不认真的普通人写的，不是故意搞怪而是真的马虎。");
    }

    @Transactional
    public ShitsifyResponse generateShitPaper(Long paperId, String style) {
        log.info("Generating shitsified paper for id={}, style={}", paperId, style);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("Paper not found: " + paperId);
        }

        String originalText = paper.getOriginalText();
        if (originalText == null || originalText.isBlank()) {
            throw new RuntimeException("Paper text is empty");
        }

        if (style == null || !STYLE_DESCRIPTIONS.containsKey(style)) {
            style = "真实人类版";
        }

        String shitsifiedText;
        try {
            shitsifiedText = callDeepSeekShitsify(originalText, style);
            log.info("AI-generated shitsified text for paper id={}, length={}", paperId, shitsifiedText.length());
        } catch (Exception e) {
            log.warn("DeepSeek shitification failed for paper id={}, using rule-based fallback", paperId);
            shitsifiedText = ruleBasedShitsify(originalText, style);
        }

        paper.setShitsifiedText(shitsifiedText);
        paper.setStyleTemplate(style);
        paperMapper.updateById(paper);

        return new ShitsifyResponse(paperId, originalText, shitsifiedText, style);
    }

    private String callDeepSeekShitsify(String originalText, String style) throws Exception {
        String styleDesc = STYLE_DESCRIPTIONS.getOrDefault(style, STYLE_DESCRIPTIONS.get("真实人类版"));

        String userMessage = "请把下面这篇论文改写为【" + style + "】。\n" +
                styleDesc + "\n\n" +
                "原文（" + originalText.length() + "字）：\n" + truncate(originalText, 6000);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 1.0);
        requestBody.put("max_tokens", 8000);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String bodyJson = objectMapper.writeValueAsString(requestBody);
        HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText().trim();
            }
        }
        throw new RuntimeException("AI response format unexpected");
    }

    // ==================== Rule-based fallback below ====================

    private static final List<String> ACADEMIC_CLICHES = Arrays.asList(
            "综上所述，", "值得注意的是，", "具有重要意义的是，", "不可否认，",
            "显而易见，", "众所周知，", "换言之，", "进一步而言，",
            "基于此，", "由此可见，", "从根本上说，", "必须指出的是，",
            "毋庸置疑，", "概而言之，", "总体而言，", "从某种意义上说，",
            "不言而喻，", "特别值得关注的是，", "需要强调的是，", "在此背景下，", "有鉴于此，"
    );

    private static final List<String> FILLER_WORDS = Arrays.asList(
            "就是说", "然后", "那个", "的话", "什么的",
            "好吧", "反正", "就是说吧", "其实", "这种东西"
    );

    private static final List<String> COLLOQUIAL_INSERTS = Arrays.asList(
            "说实话，", "讲真，", "说白了，", "你懂的，",
            "老实说，", "其实吧，", "基本上，", "差不多就这样，",
            "我觉得吧，", "怎么说呢，"
    );

    private static final List<String> JARGON_TERMS = Arrays.asList(
            "赋能", "闭环", "抓手", "底层逻辑", "顶层设计",
            "颗粒度", "方法论", "认知升级", "迭代优化",
            "矩阵式", "生态化", "全链路", "多维路径",
            "理论架构", "分析模型", "创新框架", "协同机制",
            "评估体系", "关键因子", "核心变量", "阈值参数"
    );

    private static final List<String> CITATION_FRAGMENTS = Arrays.asList(
            "[1]", "[2]", "[3-5]", "[6,7]", "[8-10]",
            "(张三, 2020)", "(李四, 2019)", "(Wang et al., 2021)",
            "(Smith, 2018)", "[11-13]", "(陈某某, 2022)"
    );

    private static final List<String> RESTATEMENTS = Arrays.asList(
            "也就是说，", "换句话说，", "通俗地讲，", "简而言之，",
            "更具体地说，", "实际上，这意味着，", "从另一个角度看，"
    );

    private static final Map<String, String> TYPO_MAP = new LinkedHashMap<>();
    static {
        TYPO_MAP.put("的", "地");
        TYPO_MAP.put("得", "的");
        TYPO_MAP.put("在", "再");
        TYPO_MAP.put("做", "作");
        TYPO_MAP.put("那么", "那吗");
    }

    private String ruleBasedShitsify(String originalText, String style) {
        List<String> sentences = splitSentences(originalText);
        StyleParams params = getStyleParams(style);
        List<String> result = new ArrayList<>();

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            String transformed = sentence;

            if (random.nextDouble() < params.clicheProb) {
                result.add(randomChoice(ACADEMIC_CLICHES));
            }
            if (random.nextDouble() < params.colloquialProb) {
                result.add(randomChoice(COLLOQUIAL_INSERTS));
            }
            if (random.nextDouble() < params.typoProb) {
                transformed = applyTypos(transformed);
            }
            if (random.nextDouble() < params.halfSentenceProb) {
                transformed = breakInHalf(transformed);
            }
            if (random.nextDouble() < params.longSentenceProb && i + 1 < sentences.size()) {
                transformed = mergeSentences(transformed, sentences.get(i + 1));
                i++;
            }
            if (random.nextDouble() < params.termStackProb) {
                transformed = stackJargon(transformed);
            }
            if (random.nextDouble() < params.citationProb) {
                transformed = transformed.trim() + randomChoice(CITATION_FRAGMENTS);
            }
            if (random.nextDouble() < params.fillerProb) {
                transformed = insertFiller(transformed);
            }
            if (random.nextDouble() < params.restatementProb) {
                result.add(transformed);
                result.add(randomChoice(RESTATEMENTS) + transformed);
                continue;
            }
            result.add(transformed);
        }

        return String.join("", result);
    }

    private List<String> splitSentences(String text) {
        List<String> sentences = new ArrayList<>();
        String[] parts = text.split("(?<=[。！？；\\n])");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                sentences.add(part);
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(text);
        }
        return sentences;
    }

    private StyleParams getStyleParams(String style) {
        if (style == null) style = "真实人类版";
        switch (style) {
            case "本科生DDL版":
                return new StyleParams(0.1, 0.7, 0.6, 0.5, 0.2, 0.1, 0.3, 0.1);
            case "导师看了头疼版":
                return new StyleParams(0.8, 0.2, 0.1, 0.1, 0.7, 0.6, 0.3, 0.3);
            case "知网缝合怪版":
                return new StyleParams(0.3, 0.1, 0.2, 0.1, 0.3, 0.7, 0.8, 0.5);
            case "真实人类版":
            default:
                return new StyleParams(0.4, 0.4, 0.3, 0.3, 0.3, 0.3, 0.3, 0.3);
        }
    }

    private String applyTypos(String text) {
        if (text.length() < 2) return text;
        List<String> keys = new ArrayList<>(TYPO_MAP.keySet());
        String target = randomChoice(keys);
        if (text.contains(target)) {
            int idx = text.indexOf(target);
            return text.substring(0, idx) + TYPO_MAP.get(target) + text.substring(idx + target.length());
        }
        int pos = random.nextInt(text.length() - 1);
        char[] chars = text.toCharArray();
        char temp = chars[pos];
        chars[pos] = chars[pos + 1];
        chars[pos + 1] = temp;
        return new String(chars);
    }

    private String breakInHalf(String text) {
        if (text.length() < 8) return text;
        int breakPoint = text.length() / 2 + random.nextInt(text.length() / 4);
        for (int i = breakPoint; i < text.length(); i++) {
            if (text.charAt(i) == '，' || text.charAt(i) == ',' || text.charAt(i) == '、') {
                return text.substring(0, i + 1) + "……";
            }
        }
        return text.substring(0, Math.min(breakPoint, text.length())) + "...";
    }

    private String mergeSentences(String first, String second) {
        first = first.replaceAll("[。！？；\\s]+$", "");
        second = second.replaceAll("^[\\s，,]+", "");
        return first + "，" + second;
    }

    private String stackJargon(String text) {
        String term1 = randomChoice(JARGON_TERMS);
        String term2 = randomChoice(JARGON_TERMS);
        while (term2.equals(term1)) term2 = randomChoice(JARGON_TERMS);
        if (text.length() < 20) {
            return "从" + term1 + "和" + term2 + "的角度来看，" + text;
        } else {
            int insertPos = text.length() / 3 + random.nextInt(text.length() / 3);
            int commaIdx = text.indexOf('，', insertPos);
            if (commaIdx > 0 && commaIdx < text.length() - 1) {
                return text.substring(0, commaIdx + 1) + "基于" + term1 + "的" + term2 + "，" + text.substring(commaIdx + 1);
            }
            return text.substring(0, insertPos) + "（涉及" + term1 + "的" + term2 + "层面）" + text.substring(insertPos);
        }
    }

    private String insertFiller(String text) {
        if (text.length() < 5) return text;
        int insertPos = random.nextInt(text.length() - 1);
        for (int i = insertPos; i < text.length(); i++) {
            if (text.charAt(i) == '，' || text.charAt(i) == ',') {
                return text.substring(0, i + 1) + randomChoice(FILLER_WORDS) + "，" + text.substring(i + 1);
            }
        }
        return randomChoice(FILLER_WORDS) + "，" + text;
    }

    private String randomChoice(List<String> list) {
        return list.get(random.nextInt(list.size()));
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private static class StyleParams {
        double clicheProb, colloquialProb, typoProb, halfSentenceProb,
               longSentenceProb, logicJumpProb, termStackProb, citationProb,
               fillerProb, restatementProb;

        StyleParams(double cp, double cop, double tp, double hp, double lp, double ljp, double tsp, double cip) {
            this.clicheProb = cp;
            this.colloquialProb = cop;
            this.typoProb = tp;
            this.halfSentenceProb = hp;
            this.longSentenceProb = lp;
            this.logicJumpProb = ljp;
            this.termStackProb = tsp;
            this.citationProb = cip;
            this.fillerProb = cop;
            this.restatementProb = cp * 0.5;
        }
    }
}
