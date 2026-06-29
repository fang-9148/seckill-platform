package com.seckill.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.seckill"})
@EnableScheduling
public class SeckillAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillAdminApplication.class, args);
        System.out.println("""

                ╔══════════════════════════════════════════════╗
                ║  ⚡ Seckill Admin — 管理后台启动成功 ⚡     ║
                ║  API 文档: http://localhost:9090/doc.html   ║
                ╚══════════════════════════════════════════════╝
                """);
    }
}
