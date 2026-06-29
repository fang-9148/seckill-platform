# ⚡ Seckill Platform — 高并发秒杀系统

> **面试亮点项目** | Spring Boot 3.2 + Redis + JUC 并发编程 + Redisson + MyBatis-Plus

一个面向 **1000 QPS** 设计的高并发秒杀系统，使用 **BlockingQueue + 线程池** 替代 RabbitMQ 实现削峰填谷，覆盖从 Java 基础到分布式架构的全栈技能点。

---

## 🏗️ 项目架构

```
               ┌─────────────┐
               │   Nginx     │  ← 反向代理 + 限流
               └──────┬──────┘
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │ Core API │ │ Core API │ │ Core API │  ← 水平扩展
    │ ┌──────┐ │ │          │ │          │
    │ │队列  │ │ │  每个实例内部运行：       │
    │ │线程池│ │ │  LinkedBlockingQueue    │
    │ └──────┘ │ │  + ThreadPoolExecutor  │
    └────┬─────┘ └────┬─────┘ └────┬─────┘
         │             │             │
         └─────────────┼─────────────┘
                       │
               ┌───────┼───────┐
               ▼               ▼
         ┌─────────┐    ┌──────────┐
         │  Redis  │    │  MySQL   │
         │ (缓存+锁)│    │ (持久化) │
         └─────────┘    └──────────┘
```

## 📁 项目结构

```
seckill-platform/
├── seckill-common/              # 公共模块
│   └── enums/       ResultCode 统一状态码
│   └── exception/   全局异常处理 + 业务异常
│   └── result/      Result<T> 统一响应体
│   └── util/        JWT、Redis、MD5 工具
│   └── constant/    Redis Key、MQ 常量
│
├── seckill-core/                # 核心业务（秒杀引擎）
│   └── controller/  用户、秒杀、订单接口
│   └── service/     秒杀核心逻辑 + 用户 + 订单
│   └── repository/  JPA 仓储接口
│   └── mapper/      MyBatis Mapper（复杂SQL）
│   └── model/       Entity、VO、DTO
│   └── mq/producer/  BlockingQueue 订单队列 + 线程池消费者（零 MQ 依赖）
│   └── aop/         限流切面、日志切面
│   └── scheduler/   库存预热定时任务
│   └── config/      Redis、线程池、Caffeine
│
├── seckill-admin/               # 管理后台
│   └── controller/  商品管理、活动配置、库存预热
│
└── docker/                      # Docker 部署
    └── docker-compose.yml      MySQL + Redis
    └── init.sql                数据库初始化
```

## 🔥 核心技术亮点

### 1. 防超卖：Redis Lua 脚本原子扣库存
```lua
-- 一次网络往返，原子执行判断+扣减
if redis.call('get', KEYS[1]) > 0 then
    redis.call('decr', KEYS[1])
    return 1
else
    return 0
end
```

### 2. 削峰填谷：BlockingQueue + 线程池（零 MQ 依赖）

```java
// SeckillOrderQueue:
private final LinkedBlockingQueue<SeckillOrderTask> taskQueue = new LinkedBlockingQueue<>(10000);

// 秒杀请求进队列 → 立即返回
taskQueue.offer(task);
// 后台线程池慢慢消费 → 落库不冲击 MySQL
```
秒杀请求进入内存队列 → **立即返回"排队中"** → 线程池异步落库
- 纯 Java 并发实现，零外部依赖
- 代码量仅 ~200 行，充分展示 JUC 功底
- 队列满自动降级为同步执行，不丢单

### 3. 三级缓存策略
```
Caffeine(本地 10s) → Redis(分布式) → MySQL(持久层)
```
热点商品提前预热到 Caffeine 本地缓存，Redis 不可用时自动降级。

### 4. 分布式锁：Redisson
库存预热、活动状态变更使用 Redisson 分布式锁，保证集群环境下只执行一次。

### 5. 接口防刷：动态秒杀路径 + 限流
- 每次请求前先获取动态 MD5 路径（60s 过期）
- Guava RateLimiter 令牌桶算法 + AOP 注解限流
- 用户级重复下单检测（Redis + DB 双重校验）

### 6. 乐观锁兜底
MySQL `version` 字段 + `WHERE stock > 0 AND version = ?` 保证最终一致性。

## 🚀 快速开始

### 1. 启动核心服务
```bash
cd seckill-core
mvn spring-boot:run
# API 文档: http://localhost:8080/doc.html
```

### 2. 启动管理后台
```bash
cd seckill-admin
mvn spring-boot:run
# API 文档: http://localhost:8081/doc.html
```

### 3. 秒杀流程
```bash
# 1. 登录获取 Token
curl -X POST "http://localhost:8080/api/user/login?username=test&password=123456"

# 2. 获取秒杀路径（防刷）
curl "http://localhost:8080/api/seckill/path?userId=1&productId=1&activityId=1"

# 3. 执行秒杀
curl -X POST "http://localhost:8080/api/seckill/execute?userId=1" \
  -H "Content-Type: application/json" \
  -d '{"activityId":1,"productId":1,"seckillPath":"xxxx"}'

# 4. 轮询结果
curl "http://localhost:8080/api/seckill/result?userId=1&activityId=1"
```

## 📊 技能点覆盖清单

| 分类 | 技能 | 状态 |
|------|------|------|
| **Java 核心** | OOP、集合、泛型、Lambda/Stream、异常处理 | ✅ |
| **JUC 并发** | BlockingQueue、线程池、ConcurrentHashMap、原子类 | ✅ |
| **Spring** | Boot、MVC、AOP、Security(JWT)、Cache | ✅ |
| **持久层** | MyBatis-Plus、乐观锁、事务 | ✅ |
| **Redis** | 缓存策略、Lua 脚本原子操作、分布式锁 | ✅ |
| **削峰方案** | LinkedBlockingQueue + ThreadPoolExecutor | ✅ |
| **性能** | 三级缓存、Guava 令牌桶限流、连接池调优 | ✅ |
| **工具** | Maven 多模块、Docker、JMeter 压测 | ✅ |
| **设计模式** | 策略、模板方法、单例、工厂、代理(AOP) | ✅ |
| **工程化** | 全局异常处理、统一响应、日志规范、Knife4j | ✅ |

## 🛠️ 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.0 | 基础框架 |
| MyBatis-Plus | 3.5.5 | ORM + 复杂 SQL |
| Spring Data JPA | - | 简单 CRUD |
| Redis | 7.x | 缓存 + 原子扣库存 |
| Redisson | 3.25.2 | 分布式锁 |
| LinkedBlockingQueue | JDK | 异步削峰（内存队列替代 MQ） |
| Caffeine | 3.1.8 | 本地缓存 |
| Guava | 33.0.0 | 令牌桶限流 |
| Knife4j | 4.5.0 | API 文档 |
| MySQL | 8.0 | 数据持久化 |
| Docker | - | 环境部署 |
| JMeter | - | 压力测试 |

---
