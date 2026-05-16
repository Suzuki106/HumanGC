package com.humangc.service;

import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * HumanGC 含人率统计算法。
 *
 * 使用 Jieba 中文分词（非逐字拆分），基于 AIGC 检测方法的逆向设计：
 *   - AIGC 检测：低困惑度 + 低突发性 → AI 写的
 *   - HumanGC：  高困惑度 + 高突发性 → 人写的
 *
 * 参考论文：
 *   - Fast-DetectGPT (ICLR 2024): 条件概率曲率
 *   - GLTR (Harvard/IBM 2019): 词概率分布可视化
 *   - Feature-Based Detection of AIGT (2025): 45 特征 + 随机森林
 */
@Slf4j
@Component
public class HumanRateCalculator {

    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    // ── 子维度权重（总和 1.0，针对中文词级分词校准） ──
    private static final double W_BURSTINESS     = 0.24;  // 句子突发性
    private static final double W_PERPLEXITY     = 0.14;  // 困惑度代理
    private static final double W_LEXICAL        = 0.22;  // 词汇多样性 (TTR)
    private static final double W_READABILITY    = 0.12;  // 平均句长
    private static final double W_PUNCTUATION    = 0.10;  // 标点多样性
    private static final double W_SENTENCE_VAR   = 0.10;  // 句长波动
    private static final double W_HAPAX          = 0.08;  // 罕用词比例

    /**
     * 对整个文本计算含人率（0-100）。
     */
    public BigDecimal calculate(String text) {
        if (text == null || text.isBlank()) return BigDecimal.ZERO;

        // 归一化文本
        String cleaned = text.replaceAll("\\s+", " ").trim();
        List<String> sentences = splitSentences(cleaned);
        List<String> words = tokenize(cleaned);

        if (sentences.isEmpty() || words.size() < 10) {
            // 文本太短，无法可靠估计，返回固定中等值
            return BigDecimal.valueOf(50.0);
        }

        double score = 0.0;

        score += W_BURSTINESS   * calcBurstiness(sentences);
        score += W_PERPLEXITY   * calcPerplexityProxy(cleaned, words);
        score += W_LEXICAL      * calcLexicalDiversity(words);
        score += W_READABILITY  * calcReadabilityInverse(cleaned, sentences, words);
        score += W_PUNCTUATION  * calcPunctuationDiversity(text);
        score += W_SENTENCE_VAR * calcSentenceLengthVar(sentences);
        score += W_HAPAX        * calcHapaxRatio(words);

        // 映射到 0-100
        int rate = (int) Math.round(Math.max(0, Math.min(100, score)));
        log.info("HumanRate calculated: rate={}, burstiness={}, perplexity={}, lexical={}, readability={}, punct={}, sentVar={}, hapax={}",
                rate,
                round(W_BURSTINESS * calcBurstiness(sentences)),
                round(W_PERPLEXITY * calcPerplexityProxy(cleaned, words)),
                round(W_LEXICAL * calcLexicalDiversity(words)),
                round(W_READABILITY * calcReadabilityInverse(cleaned, sentences, words)),
                round(W_PUNCTUATION * calcPunctuationDiversity(text)),
                round(W_SENTENCE_VAR * calcSentenceLengthVar(sentences)),
                round(W_HAPAX * calcHapaxRatio(words)));

        return BigDecimal.valueOf(rate);
    }

    // ═══════════════════════════════════════════════
    // 1. 句子突发性 (Burstiness)
    // ═══════════════════════════════════════════════
    // 定义：句子长度的变异系数 CV = σ / μ
    // AI 文本句子长度趋于均匀（低 CV），人类文本参差不齐（高 CV）
    private double calcBurstiness(List<String> sentences) {
        double[] lens = sentences.stream().mapToDouble(s -> s.length()).toArray();
        double mean = Arrays.stream(lens).average().orElse(1);
        double variance = Arrays.stream(lens).map(l -> (l - mean) * (l - mean)).average().orElse(0);
        double cv = Math.sqrt(variance) / mean;

        // cv 通常在 0.2~1.5 之间，映射到 0~100
        // 参考值：AI ≈ 0.3~0.5, 人类 ≈ 0.6~1.2
        return clamp(0, 100, cv * 80);
    }

    // ═══════════════════════════════════════════════
    // 2. 困惑度代理 (Perplexity Proxy)
    // ═══════════════════════════════════════════════
    // 使用字符级 3-gram 熵作为困惑度近似值（参考 KenLM N-gram 思路）
    // 熵越高 → 文本越不可预测 → 越像人写的
    private double calcPerplexityProxy(String text, List<String> words) {
        // 2a. 字符级 trigram 熵
        Map<String, Integer> trigrams = new HashMap<>();
        int total = 0;
        for (int i = 0; i < text.length() - 3; i++) {
            String tri = text.substring(i, i + 3);
            trigrams.merge(tri, 1, Integer::sum);
            total++;
        }
        double charEntropy = 0;
        for (int c : trigrams.values()) {
            double p = (double) c / total;
            charEntropy -= p * Math.log(p);
        }
        // 归一化：最大可能熵 ≈ log(total)，实际范围 3~8
        double charScore = clamp(0, 100, charEntropy * 12);

        // 2b. Zipf 偏差 — 人类文本偏离 Zipf 定律更多
        Map<String, Integer> wordFreq = new HashMap<>();
        for (String w : words) wordFreq.merge(w.toLowerCase(), 1, Integer::sum);
        List<Integer> sortedFreq = wordFreq.values().stream()
                .sorted(Comparator.reverseOrder()).collect(Collectors.toList());

        // Zipf: rank 1 的词频率应该接近 total/1, rank 2 → total/2, ...
        // 人类文本偏离更大
        double totalWords = words.size();
        double zipfDev = 0;
        int count = Math.min(sortedFreq.size(), 50);
        for (int i = 0; i < count; i++) {
            double expected = totalWords / (i + 1.0);
            double actual = sortedFreq.get(i);
            zipfDev += Math.abs(actual - expected) / expected;
        }
        zipfDev /= count;
        double zipfScore = clamp(0, 100, zipfDev * 50);

        return (charScore + zipfScore) / 2.0;
    }

    // ═══════════════════════════════════════════════
    // 3. 词汇多样性 (Lexical Diversity — TTR)
    // ═══════════════════════════════════════════════
    // Type-Token Ratio = 唯一词数 / 总词数
    // AI 倾向于重复用词（低 TTR），人类更丰富（高 TTR）
    private double calcLexicalDiversity(List<String> words) {
        Set<String> unique = new HashSet<>();
        for (String w : words) unique.add(w.toLowerCase());
        double ttr = (double) unique.size() / words.size();

        // TTR 通常在 0.15~0.6 之间，映射到 0~100
        return clamp(0, 100, ttr * 150);
    }

    // ═══════════════════════════════════════════════
    // 4. 平均句长 (Chinese complexity proxy)
    // ═══════════════════════════════════════════════
    // 平均句长（字/句）是中文文本复杂度的合理代理指标
    // 长句多分句 → AI 学术腔；短句碎片化 → 人类口语化
    // 映射：15 字/句 → 18分, 80 字/句 → 96分
    private double calcReadabilityInverse(String text, List<String> sentences, List<String> words) {
        double chars = text.replaceAll("\\s", "").length();
        double sentCount = sentences.size();

        if (chars == 0 || sentCount == 0) return 50;

        double avgSentLen = chars / sentCount;
        double score = avgSentLen * 1.2;
        return clamp(0, 100, score);
    }

    // ═══════════════════════════════════════════════
    // 5. 标点多样性
    // ═══════════════════════════════════════════════
    // 人类使用更丰富的标点组合，AI 偏向规范标点（句号逗号为主）
    private double calcPunctuationDiversity(String text) {
        String punct = text.replaceAll("[^，。！？、；：\"\"''（）《》…—\\-?!,.;:\\\"'()\\[\\]{}]", "");
        if (punct.length() < 3) return 30;

        // 统计不同标点种类
        Map<Character, Integer> punctFreq = new HashMap<>();
        for (char c : punct.toCharArray()) punctFreq.merge(c, 1, Integer::sum);

        int variety = punctFreq.size();
        double density = (double) punct.length() / text.length();

        // 标点种类多 + 密度适中 = 人类特征
        double varietyScore = clamp(0, 100, variety * 10);         // 0~10 种
        double densityScore = clamp(0, 100, density * 800);        // 密度 0.02~0.12

        return (varietyScore + densityScore) / 2.0;
    }

    // ═══════════════════════════════════════════════
    // 6. 句长波动 (Sentence Length Variation)
    // ═══════════════════════════════════════════════
    // 不仅看 CV，还看极端值（极长句/极短句并存 → 人类特征）
    private double calcSentenceLengthVar(List<String> sentences) {
        double[] lens = sentences.stream().mapToDouble(s -> s.length()).toArray();
        double max = Arrays.stream(lens).max().orElse(1);
        double min = Arrays.stream(lens).min().orElse(1);
        double mean = Arrays.stream(lens).average().orElse(1);

        // 极差比 = max/min，人类通常有更大的极差
        double rangeRatio = max / Math.max(min, 1);

        // 异常句比例：长度偏离均值 2σ 的句子占比
        double variance = Arrays.stream(lens).map(l -> (l - mean) * (l - mean)).average().orElse(0);
        double std = Math.sqrt(variance);
        long outlierCount = Arrays.stream(lens).filter(l -> Math.abs(l - mean) > 2 * std).count();
        double outlierRatio = (double) outlierCount / lens.length;

        double rangeScore = clamp(0, 100, rangeRatio * 6);       // 极差比 5~20
        double outlierScore = clamp(0, 100, outlierRatio * 500); // 异常比例 0~0.2

        return (rangeScore + outlierScore) / 2.0;
    }

    // ═══════════════════════════════════════════════
    // 7. 罕用词比例 (Hapax Legomena Ratio)
    // ═══════════════════════════════════════════════
    // 只出现一次的词占比。人类文本有更多一次性的独特用词
    private double calcHapaxRatio(List<String> words) {
        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) freq.merge(w.toLowerCase(), 1, Integer::sum);

        long hapax = freq.values().stream().filter(c -> c == 1).count();
        double ratio = (double) hapax / freq.size();

        // 通常在 0.3~0.7，越高越像人
        return clamp(0, 100, ratio * 160);
    }

    // ═══════════════════════════════════════════════
    // 工具方法
    // ═══════════════════════════════════════════════

    private List<String> splitSentences(String text) {
        return Arrays.stream(text.split("[。！？\\.!\\?\n]+"))
                .map(String::trim)
                .filter(s -> s.length() > 0)
                .collect(Collectors.toList());
    }

    private List<String> tokenize(String text) {
        List<String> rawTokens = SEGMENTER.sentenceProcess(text);
        List<String> tokens = new ArrayList<>(rawTokens.size());
        for (String token : rawTokens) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private double clamp(double min, double max, double val) {
        return Math.max(min, Math.min(max, val));
    }

    private double round(double val) {
        return Math.round(val * 10) / 10.0;
    }
}
