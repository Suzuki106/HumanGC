package com.humangc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetectResponse {
    private Long paperId;
    private BigDecimal humanRate;
    private String summary;
    private Map<String, Double> dimensions;
}
