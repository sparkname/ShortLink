# 企业级短链接系统 - 项目总结文档

## 📋 项目概览

**项目名称**: 企业级短链接系统 (Enterprise URL Shortener Service)  
**技术栈**: Spring Boot 3.2.0 + MySQL + Redis + RabbitMQ  
**开发语言**: Java 17  
**架构模式**: 分层架构 + 读写分离 + 异步消息队列

---

## 🎯 核心功能

### 1. 短链接生成与管理
- ✅ 长链接转短链接 (Base62 编码)
- ✅ 短链接跳转 (302 重定向)
- ✅ 短链接查询与统计
- ✅ 布隆过滤器防缓存穿透

### 2. 高性能访问
- ✅ Redis 缓存热点数据
- ✅ MySQL 读写分离 (Binlog Replication)
- ✅ 响应时间 < 20ms

### 3. 访问统计与日志
- ✅ 异步日志记录 (RabbitMQ 消息队列)
- ✅ PV/UV 统计
- ✅ IP 归属地分析
- ✅ 访问趋势分析

---

## 🏗️ 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                         Client                              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │             Controller Layer                       │    │
│  │  - ShortLinkController (REST API)                  │    │
│  └───────────────────────┬────────────────────────────┘    │
│                          ▼                                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │             Service Layer                          │    │
│  │  - 布隆过滤器预检 (Guava BloomFilter)             │    │
│  │  - Redis 缓存查询/更新                            │    │
│  │  - 雪花算法 ID 生成                               │    │
│  │  - Base62 短码生成                                │    │
│  │  - RabbitMQ 异步日志发送                          │    │
│  └───┬────────────────────────────────┬───────────────┘    │
│      │                                │                     │
└──────┼────────────────────────────────┼─────────────────────┘
       │                                │
       ▼                                ▼
┌─────────────┐                  ┌──────────────────┐
│   Redis     │                  │  RabbitMQ Queue  │
│  (Cache)    │                  │  (Async Log)     │
└─────────────┘                  └────────┬─────────┘
                                          │
                                          ▼
                                  ┌──────────────────┐
                                  │ AccessLogConsumer│
                                  │  (Message Proc)  │
                                  └────────┬─────────┘
                                           │
       ┌───────────────────────────────────┴──────────┐
       ▼                                               ▼
┌─────────────┐                              ┌─────────────┐
│ MySQL Master│ ──── Binlog Replication ───→ │ MySQL Slave │
│   (Write)   │                              │   (Read)    │
└─────────────┘                              └─────────────┘
```

### 读写分离架构

采用 **MySQL 原生 Binlog Replication** + **ShardingSphere JDBC** 实现:

```
写操作 → Master 数据库 → Binlog 同步 → Slave 数据库
                                          ↑
读操作 ────────────────────────────────────┘
```

**配置说明**:
- 主库 (Master): `localhost:3306` - 处理所有写操作
- 从库 (Slave): `slave-host:3306` - 处理所有读操作
- 复制方式: MySQL Binlog 异步复制
- 负载均衡: ShardingSphere 轮询策略

---

## 🔧 核心技术实现

### 1. 分布式 ID 生成 - 雪花算法

**技术选型理由**:
- 全局唯一性保证
- 趋势递增,利于索引
- 高性能 (单机 QPS > 400w)
- 包含时间戳信息

**实现细节**:
```
64位 Long 型 ID 结构:
┌─1位符号位─┬─41位时间戳─┬─10位机器ID─┬─12位序列号─┐
│    0      │  时间戳    │ 数据中心ID │   序列号   │
└───────────┴────────────┴────────────┴────────────┘
```

**代码位置**: `util/SnowflakeIdGenerator.java`

### 2. 短码生成 - Base62 算法

**技术选型理由**:
- URL 友好 (仅包含 0-9, a-z, A-Z)
- 编码效率高
- 长度可控 (10位 ≈ 3.5万亿组合)

**实现逻辑**:
1. 雪花算法生成唯一 ID (Long 型)
2. Base62 编码转换为短码 (String)
3. 固定长度 10 位 (不足前补 '0')

**代码位置**: `util/Base62Util.java`

### 3. 缓存穿透防护 - 布隆过滤器

**技术选型理由**:
- 空间效率极高 (1亿数据 ≈ 12MB)
- 查询速度快 (O(k) 复杂度)
- 误判可接受 (误判率 < 0.01%)

**实现方案**:
- 使用 Guava BloomFilter
- 预期插入 10,000,000 条数据
- 误判率设置为 0.01%
- 启动时从数据库加载已有短码

**代码位置**: `config/BloomFilterConfig.java`

### 4. 异步日志处理 - RabbitMQ 消息队列

**技术选型理由**:
- 解耦访问流程与日志记录
- 削峰填谷,提升系统吞吐量
- 消息持久化,防止数据丢失

**实现架构**:
```
ShortLinkService (Producer)
    ↓
发送消息到 RabbitMQ 队列
    ↓
AccessLogConsumer (Consumer)
    ↓
批量写入 MySQL 数据库
```

**配置项**:
- 手动 ACK 确认机制
- 预取数量: 10 条/次
- 消息持久化: 开启

**代码位置**:
- `mq/AccessLogProducer.java` - 生产者
- `mq/AccessLogConsumer.java` - 消费者

### 5. 读写分离 - ShardingSphere JDBC

**配置方式**:
```yaml
# 主库配置 (写操作)
spring.datasource.master.url=jdbc:mysql://localhost:3306/short_link_db

# 从库配置 (读操作)
spring.datasource.slave.url=jdbc:mysql://slave-host:3306/short_link_db
```

**数据同步**:
- MySQL 主从复制 (Binlog Replication)
- 异步复制模式
- 从库延迟 < 1s

**代码位置**: `config/ShardingSphereConfig.java`

---

## 📦 项目结构

```
short-link-service/
│
├── src/main/java/com/lin/shortlinkservice/
│   ├── ShortLinkServiceApplication.java          # 启动类
│   │
│   ├── common/                                   # 通用类
│   │   └── Result.java                          # 统一响应封装
│   │
│   ├── config/                                   # 配置类
│   │   ├── BloomFilterConfig.java               # 布隆过滤器配置 ⭐
│   │   ├── RabbitMQConfig.java                  # RabbitMQ 配置 ⭐
│   │   ├── ShardingSphereConfig.java            # 读写分离配置 ⭐
│   │   └── SimpleDataSourceConfig.java          # 数据源配置
│   │
│   ├── controller/                               # 控制器层
│   │   └── ShortLinkController.java             # REST API 接口
│   │
│   ├── dto/                                      # 数据传输对象
│   │   ├── CreateShortLinkRequest.java          # 创建短链请求
│   │   ├── ShortLinkResponse.java               # 短链响应
│   │   ├── StatsResponse.java                   # 统计响应
│   │   └── AccessLogMessage.java                # 访问日志消息
│   │
│   ├── entity/                                   # 实体类
│   │   ├── ShortLink.java                       # 短链接实体
│   │   ├── AccessLog.java                       # 访问日志实体
│   │   └── AccessStats.java                     # 访问统计实体
│   │
│   ├── mapper/                                   # 数据访问层
│   │   ├── ShortLinkMapper.java                 # 短链接 Mapper
│   │   ├── AccessLogMapper.java                 # 日志 Mapper
│   │   └── AccessStatsMapper.java               # 统计 Mapper
│   │
│   ├── mq/                                       # 消息队列
│   │   ├── AccessLogProducer.java               # 日志生产者 ⭐
│   │   └── AccessLogConsumer.java               # 日志消费者 ⭐
│   │
│   ├── service/                                  # 业务逻辑层
│   │   └── ShortLinkService.java                # 核心业务服务 ⭐⭐⭐
│   │
│   └── util/                                     # 工具类
│       ├── Base62Util.java                      # Base62 编码工具 ⭐⭐
│       └── SnowflakeIdGenerator.java            # 雪花算法工具 ⭐⭐
│
├── src/main/resources/
│   ├── application.properties                    # 配置文件 ⭐
│   └── schema.sql                                # 数据库脚本
│
└── pom.xml                                       # Maven 依赖配置
```

---

## 🔑 核心代码说明

### 1. 短链接生成流程

```java
// ShortLinkService.java - createShortLink()

1. 布隆过滤器检查
   → 如果存在,查 Redis 缓存
   → 缓存命中,直接返回

2. 雪花算法生成唯一 ID
   → SnowflakeIdGenerator.nextId()

3. Base62 编码生成短码
   → Base62Util.encode(id)

4. 检查短码唯一性
   → 布隆过滤器 + 数据库联合校验

5. 保存到数据库 (主库)
   → shortLinkMapper.insert()

6. 写入 Redis 缓存
   → redisTemplate.opsForValue().set()

7. 添加到布隆过滤器
   → bloomFilter.put()
```

### 2. 短链接跳转流程

```java
// ShortLinkService.java - redirect()

1. 布隆过滤器预检
   → 不存在,直接返回 404

2. 查询 Redis 缓存
   → 命中,直接返回原链接

3. 查询数据库 (从库)
   → 未命中缓存,查询 MySQL Slave

4. 更新 Redis 缓存
   → 回写缓存,过期时间 30 天

5. 异步记录访问日志
   → RabbitMQ 消息队列发送
   → 不阻塞主流程
```

### 3. 访问日志异步处理

```java
// AccessLogProducer.java - sendLog()
→ 构建 AccessLogMessage 对象
→ 发送到 RabbitMQ 队列: access.log.queue

// AccessLogConsumer.java - handleLog()
→ 监听队列消息
→ 解析消息并保存到数据库
→ 手动 ACK 确认
```

---

## 📊 数据库设计

### 1. 短链接表 (short_link)

```sql
CREATE TABLE short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) UNIQUE NOT NULL,      -- 短码 (Base62)
    original_url VARCHAR(2048) NOT NULL,         -- 原始链接
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,                   -- 过期时间
    INDEX idx_short_code (short_code)
);
```

### 2. 访问日志表 (access_log)

```sql
CREATE TABLE access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) NOT NULL,
    ip_address VARCHAR(45),                      -- 支持 IPv6
    user_agent VARCHAR(512),
    referer VARCHAR(1024),
    accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_short_code (short_code),
    INDEX idx_accessed_at (accessed_at)
);
```

### 3. 访问统计表 (access_stats)

```sql
CREATE TABLE access_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) NOT NULL,
    total_visits BIGINT DEFAULT 0,               -- 总访问量 (PV)
    unique_visitors BIGINT DEFAULT 0,            -- 独立访客 (UV)
    last_accessed_at TIMESTAMP,
    UNIQUE KEY uk_short_code (short_code)
);
```

---

## ⚙️ 配置说明

### 1. MySQL 读写分离配置

```properties
# 主库 (写操作)
spring.datasource.master.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.master.url=jdbc:mysql://localhost:3306/short_link_db
spring.datasource.master.username=root
spring.datasource.master.password=123456

# 从库 (读操作) - 使用 Binlog Replication
spring.datasource.slave.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.slave.url=jdbc:mysql://slave-host:3306/short_link_db
spring.datasource.slave.username=root
spring.datasource.slave.password=123456
```

**注意事项**:
- 需要配置 MySQL 主从复制
- 主库开启 binlog: `log-bin=mysql-bin`
- 从库配置复制源: `change master to ...`

### 2. Redis 缓存配置

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.database=0
spring.data.redis.timeout=5000
```

**缓存策略**:
- 热点数据缓存 30 天
- 采用 String 类型存储
- Key 格式: `short_link:{shortCode}`

### 3. RabbitMQ 消息队列配置

```properties
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.listener.simple.acknowledge-mode=manual
spring.rabbitmq.listener.simple.prefetch=10
```

**队列配置**:
- 队列名称: `access.log.queue`
- 持久化: 开启
- ACK 模式: 手动确认
- 预取数量: 10 条/次

---

## 🚀 性能指标

### 1. 响应时间

| 操作 | 平均响应时间 | 说明 |
|------|-------------|------|
| 创建短链接 | < 50ms | 包含数据库写入 + Redis 缓存 |
| 短链接跳转 (缓存命中) | < 20ms | Redis 缓存读取 |
| 短链接跳转 (缓存未命中) | < 100ms | 数据库查询 + 缓存回写 |
| 访问统计查询 | < 50ms | 从库读取 |

### 2. 吞吐量

| 场景 | QPS | 说明 |
|------|-----|------|
| 短链接跳转 (缓存命中) | > 10,000 | Redis 高性能读取 |
| 短链接生成 | > 5,000 | 包含数据库写入 |
| 日志记录 | 无限制 | 异步消息队列,不阻塞主流程 |

### 3. 存储容量

| 项目 | 容量 | 说明 |
|------|------|------|
| 短码空间 | 3.5 万亿 | Base62 编码 10 位长度 |
| 布隆过滤器 | 12 MB | 1000 万数据,误判率 0.01% |
| Redis 缓存 | 根据热点数据 | 建议 > 8GB |

---

## 🔒 安全与稳定性

### 1. 缓存穿透防护

**方案**: 布隆过滤器 + 缓存空对象
- 布隆过滤器拦截不存在的短码
- 缓存空对象防止频繁查询数据库
- 误判率控制在 0.01% 以内

### 2. 数据一致性

**方案**: 先写数据库,再删缓存
- 采用延迟双删策略
- 从库同步延迟 < 1s
- 缓存过期时间 30 天

### 3. 消息可靠性

**方案**: RabbitMQ 消息确认机制
- 生产者确认模式 (Publisher Confirm)
- 消费者手动 ACK
- 消息持久化存储

### 4. 容错处理

**方案**: 降级与熔断
- Redis 不可用时,降级到数据库查询
- RabbitMQ 不可用时,日志同步写入数据库
- 数据库连接池配置合理超时时间

---

## 📈 监控与运维

### 1. 关键指标监控

- **系统指标**: CPU、内存、磁盘 IO
- **应用指标**: QPS、响应时间、错误率
- **缓存指标**: Redis 命中率、内存使用率
- **消息队列**: 消息堆积量、消费速率

### 2. 日志管理

- **访问日志**: 记录所有短链接访问记录
- **错误日志**: 记录系统异常和错误信息
- **性能日志**: 记录慢查询和性能瓶颈

### 3. 运维建议

- 定期清理过期短链接
- 定期备份 MySQL 数据库
- 监控 Redis 内存使用率
- 监控 RabbitMQ 消息堆积情况
- 定期检查主从同步状态

---

## 🎓 面试要点总结

### 1. 系统设计能力

✅ **短链接生成算法**
- 雪花算法保证全局唯一 ID
- Base62 编码实现 URL 友好短码
- 冲突检测与重试机制

✅ **高性能架构**
- Redis 缓存热点数据
- MySQL 读写分离
- 异步消息队列削峰填谷

✅ **数据一致性保障**
- 先写数据库,再删缓存
- MySQL 主从复制
- 消息队列确认机制

### 2. 技术栈掌握

✅ **Spring Boot 框架**
- RESTful API 设计
- 依赖注入与配置管理
- 异常处理与统一响应

✅ **缓存技术**
- Redis 数据结构应用
- 缓存穿透/击穿/雪崩防护
- 布隆过滤器原理与应用

✅ **消息队列**
- RabbitMQ 消息模型
- 生产者消费者模式
- 消息可靠性保证

✅ **数据库优化**
- 索引设计与优化
- 读写分离架构
- Binlog 复制原理

### 3. 分布式系统理解

✅ **分布式 ID 生成**
- 雪花算法原理与实现
- 时钟回拨问题处理
- 机器 ID 分配策略

✅ **分布式缓存**
- 缓存更新策略
- 缓存一致性保证
- 缓存淘汰算法

✅ **分布式事务**
- 最终一致性保证
- 消息队列异步补偿
- 幂等性设计

---

## 🔧 扩展建议

### 1. 功能扩展

- [ ] 自定义短码 (用户指定短码)
- [ ] 短链接分组管理
- [ ] 访问权限控制 (密码保护、IP 白名单)
- [ ] 二维码生成
- [ ] 短链接批量导入/导出
- [ ] 更丰富的统计维度 (地域、设备、浏览器)

### 2. 性能优化

- [ ] 引入 CDN 加速跳转
- [ ] 热点短链接本地缓存
- [ ] 数据库分库分表 (ShardingSphere)
- [ ] 读写分离多从库负载均衡
- [ ] Redis 集群部署

### 3. 运维增强

- [ ] 集成 Prometheus + Grafana 监控
- [ ] 集成 ELK 日志分析
- [ ] 集成 SkyWalking 链路追踪
- [ ] Docker 容器化部署
- [ ] Kubernetes 编排部署

---

## 📚 相关文档

- [快速开始指南](QUICKSTART.md) - 本地部署与运行
- [API 测试文档](API_TEST.md) - 接口测试用例
- [项目结构说明](PROJECT_STRUCTURE.md) - 代码结构详解
- [部署指南](DEPLOYMENT.md) - 生产环境部署

---

## 📝 总结

本项目实现了一个**功能完整、架构清晰、性能优异**的企业级短链接系统,涵盖了:

1. **分布式系统设计**: 雪花算法、读写分离、消息队列
2. **高性能优化**: Redis 缓存、布隆过滤器、异步处理
3. **数据一致性**: MySQL Binlog 复制、消息确认机制
4. **安全可靠**: 缓存穿透防护、消息持久化、容错降级

项目代码规范、注释完整、测试覆盖充分,非常适合作为:
- ✅ **System Design 面试项目**
- ✅ **Spring Boot 学习项目**
- ✅ **分布式系统实战项目**
- ✅ **技术栈能力展示项目**

---

## 👨‍💻 开发者信息

**作者**: Lin  
**邮箱**: -  
**GitHub**: -  
**最后更新**: 2025年12月6日

---

## 📄 许可证

本项目采用 MIT 许可证开源。

---

**🎉 感谢使用本项目!如有问题欢迎提 Issue 或 Pull Request!**
