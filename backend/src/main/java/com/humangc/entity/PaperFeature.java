package com.humangc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("paper_features")
public class PaperFeature {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paperId;

    private String featureName;

    private Integer triggerCount;

    private BigDecimal score;
}
