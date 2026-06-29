package com.seckill.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.core.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
