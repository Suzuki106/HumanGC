package com.humangc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {
    private String type;
    private List<LeaderboardEntry> entries;
    private Integer total;
    private Integer page;
    private Integer size;
}
