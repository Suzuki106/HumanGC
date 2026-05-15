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
        "## 毁灭技法（必须综合运用，缺一不可）\n\n" +
        "### 1. 谷歌翻译20遍模拟\n" +
        "把原文经过想象中多轮中→英→日→法→俄→中翻译后的崩坏效果写出来。特征：\n" +
        "- 术语被翻译成完全不相关的词（'深度学习'变成'深入的学问'，'神经网络'变成'大脑的网络'）\n" +
        "- 语序被彻底打乱，出现英语式倒装、日语式谓语后置\n" +
        "- 专有名词被音译或直译成诡异的表达\n" +
        "- 长句被切碎成短句碎片，短句被莫名其妙地拼接\n\n" +
        "### 2. 搬史/缝合\n" +
        "模拟从多个不相关来源搬运垃圾内容并强行拼接的效果：\n" +
        "- 在学术论述中突然插入朋友圈文案、广告词、游戏攻略、菜谱等完全不相关的内容\n" +
        "- 相邻段落的写作风格、人称、时态剧烈跳变\n" +
        "- 保留原文的章节框架但每段内容都串到别的频道去了\n\n" +
        "### 3. 断章取义\n" +
        "- 删掉所有限定条件和转折词，让原本有前提的结论变成绝对化暴论\n" +
        "- 把原本的委婉表述改成极端断言\n" +
        "- 原文说『可能』『一定程度上』→改成『绝对』『100%』『必然』\n" +
        "- 因果关系颠倒或者强行建立不存在的因果链\n\n" +
        "### 4. 逆天改写\n" +
        "- 把正经学术语言替换成逆天表述：网络冲浪腔、贴吧对线腔、发疯文学\n" +
        "- 在严肃段落中突然插入极度口语/低俗的比喻\n" +
        "- 数据和结论随意篡改夸大（『准确率85%』→『准确率8500%，拳打GPT脚踢DeepSeek』）\n" +
        "- 加入作者不存在的心路历程（『写到这里我已经快吐了』『导师说这段删掉但我觉得太牛逼了必须留着』）\n\n" +
        "### 5. 抽象化\n" +
        "这是最核心的技法。像2026年中文互联网的叮咚鸡、史诗级过肺、已读乱回一样：\n" +
        "- 用完全脱离语境的表达来描述学术内容（『这个算法的复杂度啊，就跟上班恶心穿搭一样，表面上乱糟糟但实际暗藏玄机——不，等等，它就是纯乱』）\n" +
        "- 在正经论述中插入毫无逻辑关联但好笑的空耳式表达（『实验结果如图3所示，叮咚鸡叮咚鸡，大狗叫大狗叫』）\n" +
        "- 突然跳到完全不相关的频道然后又跳回来，像被小作坊下料过一样猛\n" +
        "- 关键术语被抽象空耳化：『特征提取』→『特鸡提取』，『对比实验』→『对不实验』，『消融研究』→『小容研究』\n" +
        "- 整体读完后读者只有一个感觉：这人是打瓦打多了还是肚肚打雷了饿出幻觉了\n\n" +
        "## 约束\n" +
        "- 保留原文的大致篇幅和章节结构（外壳还在，里面烂透了）\n" +
        "- 中文输出，语言自然流畅但不能让读者正常理解\n" +
        "- 不要加任何前缀后缀，直接输出毁坏后的正文\n" +
        "- 毁得要像真的——像是被谷歌翻译虐待了20遍后被一个精神错乱的缝合怪接手写完的\n" +
        "- 抽象是最高追求：让读者看完后产生'我刚看了什么'的荒诞感，但又能隐约感受到原文的幽灵";

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
            "把下面这篇论文毁成依托答辩。综合运用谷歌翻译模拟、搬史缝合、断章取义、逆天改写、抽象化五大技法。" +
            "重点是抽象化——让论文像被叮咚鸡咬过一样莫名其妙。\n\n" + truncated));

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
