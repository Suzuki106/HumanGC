package com.humangc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("leaderboard")
public class Leaderboard {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String type;

    private Integer rankNum;

    private String entityName;

    private BigDecimal avgHumanRate;

    private Integer paperCount;

    private LocalDateTime updatedAt;
}
