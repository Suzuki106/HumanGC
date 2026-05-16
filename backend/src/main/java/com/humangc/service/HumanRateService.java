package com.humangc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.humangc.dto.DetectResponse;
import com.humangc.entity.Paper;
import com.humangc.mapper.PaperMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 含人率检测引擎。
 *
 * 混合方案：
 *   1. HumanRateCalculator   — 7 维度统计特征给定基础分（基于 AIGC 检测算法逆向）
 *   2. DeepSeek API          — AI 定性评估 + 生成中文总结
 *   3. 最终含人率 = 统计分 × 0.5 + AI 分 × 0.5
 *
 * 统计部分参考：
 *   - Fast-DetectGPT (ICLR 2024): 困惑度 / 条件概率曲率
 *   - 45-Feature AIGT Detection (2025): 风格计量 + 统计特征
 *   - GLTR (2019): 词概率分布可视化
 */
@Slf4j
@Service
public class HumanRateService {

    @Autowired
    private PaperMapper paperMapper;

    @Autowired
    private HumanRateCalculator calculator;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String REVIEW_PROMPT =
        "你是HumanGC含人率评估引擎。\n\n" +
        "你需要先给一个0-100的含人率分数，再写中文锐评。\n\n" +
        "评分锚定（严格遵循）：\n" +
        "- 0-20分：明显AI生成（逻辑严密如教科书、结构模板化、语言过于通顺规范）\n" +
        "- 20-40分：疑似AI（工整得不像人写的，缺少人类写作的随意感）\n" +
        "- 40-60分：人机难辨\n" +
        "- 60-80分：有人类写作痕迹（句式多样、偶有废话或跑题）\n" +
        "- 80-100分：明显人类（逻辑跳跃、语句不通、格式随心所欲，论文味越淡越好）\n\n" +
        "重要：你的aiRate数字必须与summary文字完全一致！\n" +
        "如果summary说含人率低，aiRate必须在0-40；说含人率高，aiRate必须在60-100。\n\n" +
        "输出格式（严格JSON，不要其他任何内容）：\n" +
        "{\"aiRate\": <0到100的整数>,\"summary\": \"<150字以内的中文锐评，风格参考2026中文互联网>\"}";

    @Transactional
    public DetectResponse calculateHumanRate(Long paperId) {
        log.info("Hybrid human-rate evaluation for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new RuntimeException("Paper not found: " + paperId);

        String text = paper.getOriginalText();
        if (text == null || text.isBlank()) throw new RuntimeException("Paper text is empty");

        // ── 1. 统计层：7 维度特征分析 ──
        Map<String, Double> dimensions = new LinkedHashMap<>();
        BigDecimal statRate = calculator.calculateWithDimensions(text, dimensions);
        log.info("Statistical human rate: {}%", statRate);

        // ── 2. AI 层：DeepSeek 定性评估 ──
        BigDecimal aiRate = BigDecimal.valueOf(50);
        String summary = "";
        boolean aiAvailable = true;

        try {
            String result = callDeepSeek(text);
            JsonNode root = objectMapper.readTree(result);
            aiRate = BigDecimal.valueOf(root.path("aiRate").asInt(50));
            summary = root.path("summary").asText("");

            if (summary.isBlank()) {
                summary = generateFallbackSummary(statRate);
            }

            // 纠正 LLM 嘴炮不一致：summary 说低分但 aiRate 给高分的情况
            aiRate = alignAiRateWithSummary(aiRate, summary);
        } catch (Exception e) {
            log.warn("DeepSeek unavailable, using statistical score only: {}", e.getMessage());
            aiAvailable = false;
            summary = generateFallbackSummary(statRate);
        }

        // ── 3. 混合：统计 50% + AI 50% ──
        BigDecimal finalRate;
        if (aiAvailable) {
            finalRate = statRate.multiply(BigDecimal.valueOf(0.5))
                    .add(aiRate.multiply(BigDecimal.valueOf(0.5)))
                    .setScale(1, RoundingMode.HALF_UP);
        } else {
            finalRate = statRate;
        }

        // 存库
        paper.setHumanRate(finalRate);
        paperMapper.updateById(paper);

        log.info("HumanRate final: paperId={}, statRate={}, aiRate={}, finalRate={}",
                paperId, statRate, aiRate, finalRate);

        return new DetectResponse(paperId, finalRate, summary, dimensions);
    }

    private String callDeepSeek(String text) throws Exception {
        String truncated = text.length() > 6000 ? text.substring(0, 6000) + "..." : text;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 400);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", REVIEW_PROMPT));
        messages.add(Map.of("role", "user", "content",
                "请评估以下论文含人率并给出锐评，按JSON格式输出：\n\n" + truncated));

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
        ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0).path("message").path("content").asText().trim();
            }
        }
        throw new RuntimeException("Unexpected AI response format");
    }

    /**
     * 纠正 LLM 的文字和数字不一致。
     * 如果 summary 明显是差评（含"低""AI""模板"等关键词）但 aiRate > 40，clamp 到 40。
     * 同理 summary 明显好评但 aiRate < 60，clamp 到 60。
     */
    private BigDecimal alignAiRateWithSummary(BigDecimal aiRate, String summary) {
        String s = summary.toLowerCase();
        boolean isNegative = s.contains("低") || s.contains("尘埃") || s.contains("一眼ai")
                || s.contains("假") || s.contains("模板") || s.contains("答辩")
                || s.contains("ai写") || s.contains("ai生成") || s.contains("像ai")
                || s.contains("没人类") || s.contains("没人味") || s.contains("太工整")
                || s.contains("过于通顺") || s.contains("教科书");
        boolean isPositive = s.contains("高") && (s.contains("含人") || s.contains("人味"))
                || s.contains("混亂") || s.contains("混乱") || s.contains("废话")
                || s.contains("真正的");

        if (isNegative && !isPositive && aiRate.doubleValue() > 40) {
            log.info("AI summary is negative but aiRate={}, clamping to 40", aiRate);
            return BigDecimal.valueOf(35);
        }
        if (isPositive && !isNegative && aiRate.doubleValue() < 60) {
            log.info("AI summary is positive but aiRate={}, clamping to 60", aiRate);
            return BigDecimal.valueOf(65);
        }
        return aiRate;
    }

    private String generateFallbackSummary(BigDecimal rate) {
        double r = rate.doubleValue();
        if (r < 20) {
            return "含人率极低，逻辑严密如AI，建议增加混乱度和随意感以提升人味。";
        } else if (r < 40) {
            return "含人率偏低，文本整体偏工整，可适当引入口语化和格式不统一。";
        } else if (r < 60) {
            return "含人率中等，兼具人类和AI特征，还有优化空间。";
        } else if (r < 80) {
            return "含人率较高，有明显的随意写作痕迹，结构松散，人味十足。";
        } else {
            return "含人率极高，逻辑跳跃、句式混乱、格式随心所欲，堪称人类写作典范。";
        }
    }
}
