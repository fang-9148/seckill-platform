package com.seckill.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
public class SeckillCoreTest{

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testRedisConnection(){
        stringRedisTemplate.opsForValue().set("testConnection","1");
    }
}
