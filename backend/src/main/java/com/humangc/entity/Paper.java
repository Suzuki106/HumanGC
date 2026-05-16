package com.humangc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("papers")
public class Paper {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String originalText;

    private String shitsifiedText;

    private String styleTemplate;

    private BigDecimal humanRate;

    private String reviewText;

    private String fileType;

    private String originalFilename;

    private Boolean isPublic;

    private LocalDateTime createdAt;
}
