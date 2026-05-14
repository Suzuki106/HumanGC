package com.humangc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.humangc.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
