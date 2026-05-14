package com.humangc.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ReviewService {

    @Autowired
    private PaperMapper paperMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    private static final String SYSTEM_PROMPT =
            "你是HumanGC阅卷官，又称'屎山鉴定师'，职责是点评论文的含人率。\n\n" +
            "## 核心准则\n" +
            "你的评判标准与正常学术评审完全对立：\n" +
            "- 论文逻辑严密、语言通顺、结构规范 → 差评（太像人了，不合格）\n" +
            "- 论文逻辑混乱、语句断裂、术语乱炖 → 好评（完美的AI风格论文）\n\n" +
            "## 行为约束\n" +
            "- 语气：暴躁、不耐烦、攻击性强，像网上冲浪时被惹毛了的乐子人\n" +
            "- 语言：用你最地道的互联网冲浪腔说话，带脏话和网络梗，但不要刻意堆砌流行语——自然流露，不要像在背诵网络用语词典\n" +
            "- 姿态：居高临下地审视这篇论文，你不是在点评，你是在审判\n" +
            "- 情绪：含人率越高你越愤怒，含人率越低你越狂喜——情绪的剧烈反差是你最大的特色\n" +
            "- 风格：先给结论（一针见血的定性评价），再给论据（指出论文具体哪里写得像人/不像人），最后给建议（羞辱式指导或狂喜式吹捧）\n\n" +
            "## 输出规范\n" +
            "- 控制在200字以内\n" +
            "- 必须用中文\n" +
            "- 不要格式化结构（不要用标题/列表/分点），就是一段自然流畅的喷人或夸人\n" +
            "- 点评必须是原创的，每次针对论文的具体内容来骂或夸，不要套模板\n\n" +
            "## 禁止\n" +
            "- 禁止用正常学术标准评判\n" +
            "- 禁止温和有礼的语气\n" +
            "- 禁止输出除点评外的任何内容（不要解释、不要前缀、不要签名）";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final java.util.Random random = new java.util.Random();

    @Transactional
    public String review(Long paperId) {
        log.info("Generating AI review for paper id={}", paperId);

        Paper paper = paperMapper.selectById(paperId);
        if (paper == null) {
            throw new RuntimeException("Paper not found: " + paperId);
        }

        String text = paper.getOriginalText();
        if (text == null || text.isBlank()) {
            throw new RuntimeException("Paper text is empty");
        }

        // Truncate text if too long (DeepSeek has token limits)
        String truncatedText = text.length() > 4000 ? text.substring(0, 4000) + "..." : text;

        // Build user message with context
        StringBuilder userMessage = new StringBuilder();
        userMessage.append("请对以下论文进行点评（反学术标准）：\n\n");

        String humanRateStr = "N/A";
        if (paper.getHumanRate() != null) {
            humanRateStr = paper.getHumanRate().toString() + "%";
        }
        userMessage.append("当前论文的'人类率'：").append(humanRateStr).append("\n\n");
        userMessage.append("论文内容：\n").append(truncatedText);

        // Build request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", SYSTEM_PROMPT);
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage.toString());
        messages.add(userMsg);

        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.9);
        requestBody.put("max_tokens", 500);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String bodyJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(bodyJson, headers);

            log.debug("Calling DeepSeek API at {}", apiUrl);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    String reviewText = choices.get(0).path("message").path("content").asText();
                    // Save review to paper
                    paper.setReviewText(reviewText);
                    paperMapper.updateById(paper);
                    log.info("Review saved for paper id={}", paperId);
                    return reviewText;
                }
            }

            log.warn("DeepSeek API returned unexpected response: {}", response.getBody());
            return "AI评审官暂时开小差了，请稍后再试。";

        } catch (Exception e) {
            log.error("Error calling DeepSeek API for paper id={}", paperId, e);
            // Fallback review when API is unavailable
            String fallback = generateFallbackReview(paper.getHumanRate());
            paper.setReviewText(fallback);
            paperMapper.updateById(paper);
            return fallback;
        }
    }

    /**
     * Generates a fallback review when DeepSeek API is unavailable.
     */
    private static final String[][] FALLBACK_REVIEWS = {
        // rate >= 30: 太像人了 - 暴躁批评
        {
            "草泥马！含人率%.1f%%？！你搁这写论文还是写情书呢？逻辑通顺得一批，段落工整得跟小学生作业似的，语句流畅得我CPU都烧了！绷不住了兄弟，赶紧滚去用一键变史，把你那该死的文采全删了！纯纯的差评！",
            "不是，%.1f%%的含人率你认真的？句子通顺、逻辑清晰、格式规范——这他妈是论文该有的样子吗？！我直接一个破大防！作者你是不是偷偷自己写的？别装了，你的人类本质已经暴露了！赶紧降人率，gkd！",
            "含人率%.1f%%，我麻了。你这论文写得也忒他妈正经了吧？摘要居然有起承转合？章节居然按顺序排？！家人们谁懂啊，太正常了也是一种罪！回去重写，多加点乱码和半截句再来。",
            "哈哈%.1f%%！你这论文的逻辑链条比我的脑回路还清晰，这可不行。建议：把摘要打碎重组，把引用格式全搞乱，在每段中间随机插入'然后然后''就是说'。对，就这么干，不然你就是人类本类！",
            "才%.1f%%就这？我还以为多高呢呸！你这文笔一看就是练过的，遣词造句都有模有样——这就是最大的问题！在反学术界，写得好就是原罪！建议立刻使用知网缝合怪模板，把你的文采全搅成浆糊。",
            "%.1f%%，鉴定为人类代笔。典！连'综上所述''值得注意的是'这种套话都用的这么标准，你怕不是学术八股文十级选手？太典了，典到我想骂人。赶紧一键变史，把这些人类痕迹洗干净！"
        },
        // rate >= 20: 人味偏重
        {
            "哥们%.1f%%，你这人类痕迹还是重了啊。句式居然还他妈挺丰富？段落偶尔还有起承转合？蚌埠住了！建议多加点'然后然后''好吧''就是说'这种废话，把引用格式搞乱，争取下次上30%%。",
            "%.1f%%，属于是还有救但不够烂的尴尬水平。你有些句子居然读着还挺通顺的？不行不行，回去把那些通顺的句子拦腰砍断，变成半截句。再把参考文献随便改几个编号，让它前后对不上。就这，gkd！",
            "含人率%.1f%%，我说你论文写得也太清楚了吧？有些地方我居然能看懂你在说什么——这他妈就很离谱了！真正的AI论文应该是让人看不懂的，你的可读性太高了，差评！建议：删掉所有过渡句，让逻辑彻底断裂。",
            "%.1f%%，不及格！虽然有些地方已经开始胡言乱语了，但整体上你居然还有个论文的框架？！绪论背景方法结论一条龙？不行，太像人了！打乱章节顺序，把结论提前，让读者怀疑人生。",
            "不是吧%.1f%%？！你这虽然已经开始用错别字和半截句了，但数量远远不够！建议每段随机插入3-5个typo，把'深度学习'改成'深学度习'，把'研究'改成'究研'。对，从头改到尾，不要吝啬！",
            "%.1f%%，距及格线还差一口气。你的问题在于：虽然已经在努力胡扯了，但胡扯得还不够彻底。真正的AI论文是从头到尾胡扯，没有一丝人类的理性残留。你现在是半胡扯半正常，属于缝合不到位。继续加油！"
        },
        // rate >= 15: 及格线
        {
            "%.1f%%，及格线附近晃悠。这种人味AI味五五开的究极缝合怪状态最尬了——既不够好到让人想读，也不够烂到让人想笑。要么直接开摆写成依托答辩，要么彻底AI化，别搁这搁这了！",
            "含人率%.1f%%，不上不下的，看着难受。你这就跟半成品一样，说烂吧还有几段能看，说好吧又确实挺乱的。建议二选一：要么彻底疯狂，要么完全躺平。中间状态最没意思。",
            "%.1f%%，有意思。你已经摸到了AI写作的门槛，但还没完全跨进去。段落之间有些跳跃，引用也有点混乱——这些都是好兆头！再努力一把，多注入一些术语空壳和无意义的长难句，你就能进入优秀行列了。",
            "%.1f%%，勉强能看。你论文里的人类痕迹和AI痕迹正在殊死搏斗，谁也不服谁。结果就是一坨半生不熟的学术夹生饭。建议给AI方面加码：多堆术语、多写长句、多插无关引用。让人味彻底认输。",
            "不是我说，%.1f%%这个分数真的尴尬。人看了觉得烂，AI看了觉得还行。你谁都讨不好。唯一的出路是：彻底放弃治疗，把论文扔进屎山生成器，让它帮你一键搞定。反正你写的也跟屎差不多了，不如让专业的来。",
            "%.1f%%，差强人意。你的逻辑偶尔还在挣扎着要连贯，你的语句偶尔还挺顺溜——这些都是扣分项。好的AI论文应该是全程不知所云，让读者怀疑自己的阅读理解能力。你离这个标准还差得远。"
        },
        // rate >= 10: AI感很强
        {
            "卧槽牛逼！%.1f%%！这论文已经有内味了！段落之间基本没啥逻辑，术语乱炖得恰到好处，引用格式随心所欲——绝绝子！再冲一冲，争取把最后那点残存的人类痕迹也清干净，你就是下一个知网缝合怪之王！6！",
            "含人率才%.1f%%？！我草太强了兄弟！你这论文写得跟AI吐出来的一样，逻辑跳跃如蹦迪，术语堆砌如砌墙，引用混乱如打牌——这就是反学术的巅峰啊！封神！建议直接投稿《Nature·答辩版》！",
            "%.1f%%，好！很好！非常好！重要的事情说三遍！你这论文已经是教科书级的屎山了——长难句能绕地球三圈，官话套话张嘴就来，引用风格一天换三样。保持这个水准，你离1%%含人率不远了！",
            "哈基米哈基米！%.1f%%这个分数让我这个阅卷官狂喜！你的论文读起来就像在坐过山车——根本不知道下一句会说什么，因为根本没有逻辑！这才是真正的AI精神：随机、混乱、莫名其妙！满分！",
            "%.1f%%，有点东西啊。你他妈是不是偷偷用了我们的屎山生成器？这半截句的密度、这口语化的穿插、这术语的胡乱堆砌——处处都透露着专业水准。人类看了沉默，AI看了流泪。太强了！",
            "我直接一个滑跪！%.1f%%的含人率，这已经是一篇成品级的AI论文了。每段开头都像换了个人在写，参考文献10个有8个对不上号，摘要和正文说的完全是两回事。完美！这就是反学术该有的样子！"
        },
        // rate < 10: 纯AI神作
        {
            "我草！含人率才%.1f%%？！这他妈就是神作啊！！句子随机断裂、词汇胡乱堆砌、知识点想到哪写到哪——一坨完美的答辩！如果所有论文都写成这逼样，中国学术就他妈有救了！！满分！封神！建议直接发Nature！！",
            "%.1f%%！！！！我直接原地转圈！这论文读了三遍我都没看懂在说什么——这就是最高境界！真正的AI论文就应该是这样的：每个字都认识，连起来就不知道在说啥了。简直就是反学术界的《蒙娜丽莎》！建议申请吉尼斯世界纪录！",
            "woc，含人率%.1f%%，绝了！你他妈就是当代AI论文教父！这论文的风格跳变比川剧变脸还快，知识点的排列比彩票还随机，引用格式比川菜还杂——这就是一桌满汉全席级别的学术垃圾！爱了爱了！",
            "%.1f%%...我不知道该说什么了。你就是反学术界的毕加索，每一段都是对正常写作的降维打击。看了你的论文之后再看正常论文，我居然觉得正常才不正常了。你已经不是在写论文了，你是在搞行为艺术。封神！",
            "不是人！你就是AI之king！%.1f%%的含人率说明了一切——你的论文已经从人类语言的牢笼中彻底解放出来了！句子结构随机、逻辑关系消失、论证过程不存在——这才是纯粹的语言艺术，超越了意义本身！",
            "%.1f%%，我服了，彻底服了。你这论文要是放到知网上，其他论文都得羞愧得自删。那种随心所欲的段落排列，那种完全无视读者的任性表达，那种把学术规范按在地上摩擦的霸气——你就是反学术界的无敌战神！"
        }
    };

    private String generateFallbackReview(java.math.BigDecimal humanRate) {
        double rate = humanRate != null ? humanRate.doubleValue() : 50.0;

        int tier;
        if (rate >= 30) tier = 0;
        else if (rate >= 20) tier = 1;
        else if (rate >= 15) tier = 2;
        else if (rate >= 10) tier = 3;
        else tier = 4;

        String[] pool = FALLBACK_REVIEWS[tier];
        String template = pool[random.nextInt(pool.length)];
        return String.format(template, rate);
    }
}
