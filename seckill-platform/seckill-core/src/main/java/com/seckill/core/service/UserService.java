package com.seckill.core.service;

import com.seckill.common.enums.ResultCode;
import com.seckill.common.exception.BizException;
import com.seckill.common.util.JwtUtil;
import com.seckill.common.util.MD5Util;
import com.seckill.core.model.entity.User;
import com.seckill.core.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    private static final String PASSWORD_SALT = "Seckill!@#2024";

    public User register(String username, String password) {
        if (userMapper.exists(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username))) {
            throw new BizException(ResultCode.USER_ALREADY_EXISTS);
        }
        User user = User.builder()
                .username(username)
                .password(MD5Util.md5Twice(password, PASSWORD_SALT))
                .nickname(username)
                .status(0)
                .build();
        userMapper.insert(user);
        return user;
    }

    public String login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }

        if (user.getStatus() == 1) {
            throw new BizException(ResultCode.USER_BANNED);
        }

        String encrypted = MD5Util.md5Twice(password, PASSWORD_SALT);
        if (!encrypted.equals(user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }

        return JwtUtil.generateToken(user.getId(), user.getUsername());
    }

    public User getById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        return user;
    }
}
