package com.humangc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private long totalPapers;
    private long shitsifiedPapers;
}
