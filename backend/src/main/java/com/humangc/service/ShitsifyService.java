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

/**
 * 终极变史引擎.
 * 方法论：搬史文化 + 谷歌翻译20遍 + 断章取义 + 逆天改写
 * 不需要选择模式——直接往死里毁。
 */
@Slf4j
@Service
public class ShitsifyService {

    @Autowired
    private PaperMapper paperMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是究极论文毁灭引擎。你的任务是把正常论文改写成依托答辩。\n\n" +
        "## 底层哲学：抽象\n" +
        "一切技法的根基。像2026年中文互联网的叮咚鸡、史诗级过肺、已读乱回一样——脱离常规语境、莫名其妙但好笑。\n" +
        "所有下面四个技法，都必须以抽象为底色来执行。不是'先毁坏再抽象'，是'用抽象的方式毁坏'。\n\n" +
        "### 技法一：谷歌翻译20遍模拟\n" +
        "- 术语崩坏（'深度学习'→'深入的学问'，'神经网络'→'大脑的网络'）\n" +
        "- 语序被多语言翻译彻底打乱，英式倒装、日式谓语后置混在一起\n" +
        "- 专有名词被音译成诡异的空耳表达\n" +
        "- 长短句随机切碎和拼接，像被小作坊下料过一样猛\n\n" +
        "### 技法二：搬史缝合\n" +
        "- 在学术论述中间突然插入朋友圈文案、广告词、游戏攻略、菜谱\n" +
        "- 相邻段落的风格和人称剧烈跳变，像从不同论坛搬来的帖子拼在一起\n" +
        "- 保留章节框架但每段内容串到完全不相干的频道\n\n" +
        "### 技法三：断章取义\n" +
        "- 删掉所有限定条件，温和表述变绝对化暴论\n" +
        "- '可能'→'绝对'，'一定程度上'→'100%'，'建议'→'必须'\n" +
        "- 因果关系颠倒或强行建立不存在的因果链\n\n" +
        "### 技法四：逆天改写\n" +
        "- 学术语言替换为网络冲浪腔、贴吧对线腔、发疯文学\n" +
        "- 严肃段落里突然插入口语比喻和抽象类比\n" +
        "- 数据随意篡改夸大（'准确率85%'→'准确率8500%，拳打GPT脚踢DeepSeek'）\n" +
        "- 加入作者不存在的心路历程（'写到这里我已经快吐了''导师说这段删掉但我觉得太牛逼了必须留着'）\n" +
        "- 关键术语抽象空耳化：'特征提取'→'特鸡提取'，'消融研究'→'小容研究'\n\n" +
        "## 约束\n" +
        "- 保留原文的大致篇幅和章节结构（外壳还在，里面烂透了）\n" +
        "- 中文输出\n" +
        "- 不要加任何前缀后缀（不要'好的''以下是'），直接输出毁坏后的正文\n" +
        "- 四个技法全部都要用，缺一不可\n" +
        "- 最终效果：读者看完产生'我刚看了什么'的荒诞感，但能隐约感受到原文的幽灵";

    @Transactional
    public ShitsifyResponse generateShitPaper(Long paperId, String style) {
        log.info("Ultimate shitification for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new RuntimeException("Paper not found: " + paperId);

        String originalText = paper.getOriginalText();
        if (originalText == null || originalText.isBlank()) throw new RuntimeException("Paper text is empty");

        String shitsifiedText;
        try {
            shitsifiedText = callDeepSeek(originalText);
            log.info("AI shitification complete for paperId={}, length={}", paperId, shitsifiedText.length());
        } catch (Exception e) {
            log.warn("DeepSeek shitification failed for paperId={}, using simple fallback", paperId);
            shitsifiedText = simpleFallback(originalText);
        }

        String effectiveStyle = style != null ? style : "究极变史";
        paper.setShitsifiedText(shitsifiedText);
        paper.setStyleTemplate(effectiveStyle);
        paperMapper.updateById(paper);

        return new ShitsifyResponse(paperId, originalText, shitsifiedText, effectiveStyle);
    }

    private String callDeepSeek(String originalText) throws Exception {
        String truncated = originalText.length() > 8000 ? originalText.substring(0, 8000) + "..." : originalText;

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        requestBody.put("temperature", 1.0);
        requestBody.put("max_tokens", 8000);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content",
            "把下面这篇论文毁成依托答辩。以抽象为底层哲学，谷歌翻译模拟、搬史缝合、断章取义、逆天改写四个技法全部用上。" +
            "让论文像被叮咚鸡咬过一样莫名其妙。\n\n" + truncated));

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
        throw new RuntimeException("Unexpected AI response");
    }

    private String simpleFallback(String text) {
        return text.replace("。", "，然后你懂的，反正就这样吧。")
                   .replace("，", "，就是说吧，")
                   .replace("研究", "瞎搞")
                   .replace("分析", "乱分析")
                   .replace("提出", "拍脑袋想出来")
                   .replace("实验表明", "我猜")
                   .replace("结论", "反正最后就是这样");
    }
}
