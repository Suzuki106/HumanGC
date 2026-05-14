package com.humangc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShitsifyResponse {
    private Long paperId;
    private String originalText;
    private String shitsifiedText;
    private String style;
}
