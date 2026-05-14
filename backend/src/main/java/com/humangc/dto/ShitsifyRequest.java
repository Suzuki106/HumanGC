package com.humangc.dto;

import lombok.Data;

@Data
public class ShitsifyRequest {
    private Long paperId;
    /** 本科生DDL版 / 导师看了头疼版 / 知网缝合怪版 / 真实人类版 */
    private String style;
}
