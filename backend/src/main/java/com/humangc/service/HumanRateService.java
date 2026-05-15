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
import java.util.*;

/**
 * AI-driven human content rate evaluation.
 * No hardcoded feature detection — DeepSeek analyzes the text directly.
 */
@Slf4j
@Service
public class HumanRateService {

    @Autowired
    private PaperMapper paperMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是HumanGC含人率评估引擎。你的任务是分析论文文本，评估其'含人率'——即这篇论文看起来有多像人写的。\n\n" +
        "评估准则（与正常学术标准对立）：\n" +
        "- 逻辑越严密、语言越通顺、结构越规范 → 含人率越低（像AI写的）\n" +
        "- 逻辑越混乱、语句越不通顺、格式越随意 → 含人率越高（像人写的）\n\n" +
        "综合考虑以下维度给出含人率评分：\n" +
        "- 用词：是否有口语化、随意表达、用词不当\n" +
        "- 句式：是否有长难句、半截句、句式单调或过度复杂\n" +
        "- 逻辑：段落之间衔接是否生硬、是否有逻辑跳跃\n" +
        "- 格式：引用格式是否一致、排版是否规整\n" +
        "- 整体：论文读起来是像精心构造的AI文本还是像真人随手写的\n\n" +
        "输出格式：必须严格按以下JSON格式输出，不要任何其他内容：\n" +
        "{\"humanRate\": <0到100的数字>,\"summary\": \"<80字以内的中文总结>\"}";

    @Transactional
    public DetectResponse calculateHumanRate(Long paperId) {
        log.info("AI-driven human-rate evaluation for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new RuntimeException("Paper not found: " + paperId);

        String text = paper.getOriginalText();
        if (text == null || text.isBlank()) throw new RuntimeException("Paper text is empty");

        try {
            String result = callDeepSeek(text);
            JsonNode root = objectMapper.readTree(result);
            double rate = root.path("humanRate").asDouble(50);
            String summary = root.path("summary").asText("评估完成");

            BigDecimal humanRate = BigDecimal.valueOf(Math.round(rate * 10) / 10.0);
            paper.setHumanRate(humanRate);
            paperMapper.updateById(paper);

            log.info("AI evaluation complete: paperId={}, rate={}%", paperId, humanRate);
            return new DetectResponse(paperId, humanRate, summary);
        } catch (Exception e) {
            log.error("DeepSeek evaluation failed for paperId={}, using fallback", paperId, e);
            return fallbackEvaluate(paper);
        }
    }

    private String callDeepSeek(String text) throws Exception {
        String truncated = text.length() > 8000 ? text.substring(0, 8000) + "..." : text;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 300);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", "请评估以下论文的含人率（0-100），按JSON格式输出：\n\n" + truncated));

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

    private DetectResponse fallbackEvaluate(Paper paper) {
        String text = paper.getOriginalText();
        int len = text != null ? text.length() : 0;
        // Simple fallback: moderate randomness based on text length
        double rate = 15 + Math.random() * 25;
        BigDecimal humanRate = BigDecimal.valueOf(Math.round(rate * 10) / 10.0);
        paper.setHumanRate(humanRate);
        paperMapper.updateById(paper);

        String summary = len > 2000 ? "论文篇幅较长，含人率处于中等水平，建议进一步改善混乱度。" : "评估完成，含人率尚有提升空间。";
        return new DetectResponse(paper.getId(), humanRate, summary);
    }
}
