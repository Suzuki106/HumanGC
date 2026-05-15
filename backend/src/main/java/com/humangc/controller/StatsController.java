package com.humangc.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.humangc.dto.StatsResponse;
import com.humangc.entity.Paper;
import com.humangc.mapper.PaperMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class StatsController {

    @Autowired
    private PaperMapper paperMapper;

    @GetMapping("/stats")
    public StatsResponse stats() {
        long totalPapers = paperMapper.selectCount(null);
        LambdaQueryWrapper<Paper> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Paper::getShitsifiedText);
        long shitsifiedPapers = paperMapper.selectCount(wrapper);
        return new StatsResponse(totalPapers, shitsifiedPapers);
    }
}
