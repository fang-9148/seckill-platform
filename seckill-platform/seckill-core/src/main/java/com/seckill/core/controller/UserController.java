package com.seckill.core.controller;

import com.seckill.common.result.Result;
import com.seckill.core.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "用户接口", description = "注册、登录")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "用户注册")
    public Result<Map<String, Object>> register(@RequestParam String username,
                                                 @RequestParam String password) {
        userService.register(username, password);
        return Result.success(Map.of("message", "注册成功"));
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public Result<Map<String, String>> login(@RequestParam String username,
                                              @RequestParam String password) {
        String token = userService.login(username, password);
        return Result.success(Map.of("token", token));
    }
}
