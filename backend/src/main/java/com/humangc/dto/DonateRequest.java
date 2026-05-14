package com.humangc.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DonateRequest {
    private BigDecimal amount;
    private String anonymousId;
}
