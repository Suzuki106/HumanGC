package com.humangc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerStatusResponse {
    private boolean running;
    private Long daysRemaining;
    private Long hoursRemaining;
    private BigDecimal progressPercent;
    private BigDecimal totalCost;
    private BigDecimal donatedAmount;
}
