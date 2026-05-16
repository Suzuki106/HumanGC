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
 *   3. 最终含人率 = 统计分 × 0.7 + AI 分 × 0.3
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
        "你是HumanGC含人率评估引擎的定性分析模块。\n\n" +
        "你需要对论文进行幽默辛辣的点评，风格参考2026中文互联网（小红书/微博/贴吧腔）。\n\n" +
        "评估准则（与正常学术标准对立）：\n" +
        "- 逻辑严密 → 差评（像AI写的答辩）\n" +
        "- 语言通顺 → 差评（没有人类味儿）\n" +
        "- 格式规范 → 差评（一眼AI）\n" +
        "- 逻辑混乱、语句不通、格式随意 → 好评（真正的人类写作）\n\n" +
        "输出格式：严格按JSON输出，不要任何其他内容：\n" +
        "{\"aiRate\": <0到100的整数>,\"summary\": \"<150字以内的中文锐评>\"}";

    @Transactional
    public DetectResponse calculateHumanRate(Long paperId) {
        log.info("Hybrid human-rate evaluation for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new RuntimeException("Paper not found: " + paperId);

        String text = paper.getOriginalText();
        if (text == null || text.isBlank()) throw new RuntimeException("Paper text is empty");

        // ── 1. 统计层：7 维度特征分析 ──
        BigDecimal statRate = calculator.calculate(text);
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
        } catch (Exception e) {
            log.warn("DeepSeek unavailable, using statistical score only: {}", e.getMessage());
            aiAvailable = false;
            summary = generateFallbackSummary(statRate);
        }

        // ── 3. 混合：统计 70% + AI 30% ──
        BigDecimal finalRate;
        if (aiAvailable) {
            finalRate = statRate.multiply(BigDecimal.valueOf(0.7))
                    .add(aiRate.multiply(BigDecimal.valueOf(0.3)))
                    .setScale(1, RoundingMode.HALF_UP);
        } else {
            finalRate = statRate;
        }

        // 存库
        paper.setHumanRate(finalRate);
        paperMapper.updateById(paper);

        log.info("HumanRate final: paperId={}, statRate={}, aiRate={}, finalRate={}",
                paperId, statRate, aiRate, finalRate);

        return new DetectResponse(paperId, finalRate, summary);
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
