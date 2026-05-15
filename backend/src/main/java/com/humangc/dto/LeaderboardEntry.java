package com.humangc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private Integer rank;
    private String name;
    private BigDecimal avgHumanRate;
    private Integer paperCount;
    private Long paperId;

    public LeaderboardEntry(Integer rank, String name, BigDecimal avgHumanRate, Integer paperCount) {
        this.rank = rank;
        this.name = name;
        this.avgHumanRate = avgHumanRate;
        this.paperCount = paperCount;
    }
}
