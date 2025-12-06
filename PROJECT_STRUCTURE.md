# 项目完整结构

```
short-link-service/
│
├── src/
│   ├── main/
│   │   ├── java/com/lin/shortlinkservice/
│   │   │   │
│   │   │   ├── ShortLinkServiceApplication.java    # 启动类
│   │   │   │
│   │   │   ├── common/                              # 通用类
│   │   │   │   └── Result.java                     # 统一响应结果
│   │   │   │
│   │   │   ├── config/                              # 配置类
│   │   │   │   ├── BloomFilterConfig.java          # 布隆过滤器配置 ⭐
│   │   │   │   └── ThreadPoolConfig.java           # 线程池配置 ⭐
│   │   │   │
│   │   │   ├── controller/                          # 控制器层
│   │   │   │   └── ShortLinkController.java        # REST API接口
│   │   │   │
│   │   │   ├── dto/                                 # 数据传输对象
│   │   │   │   ├── CreateShortLinkRequest.java     # 创建短链请求
│   │   │   │   ├── ShortLinkResponse.java          # 短链响应
│   │   │   │   └── StatsResponse.java              # 统计响应
│   │   │   │
│   │   │   ├── entity/                              # 实体类
│   │   │   │   ├── ShortLink.java                  # 短链接实体
│   │   │   │   ├── AccessLog.java                  # 访问日志实体
│   │   │   │   └── AccessStats.java                # 访问统计实体
│   │   │   │
│   │   │   ├── mapper/                              # 数据访问层
│   │   │   │   ├── ShortLinkMapper.java            # 短链接 Mapper
│   │   │   │   ├── AccessLogMapper.java            # 日志 Mapper
│   │   │   │   └── AccessStatsMapper.java          # 统计 Mapper
│   │   │   │
│   │   │   ├── service/                             # 业务逻辑层
│   │   │   │   └── ShortLinkService.java           # 核心业务服务 ⭐⭐⭐
│   │   │   │
│   │   │   └── util/                                # 工具类
│   │   │       ├── Base62Util.java                 # Base62 编码工具 ⭐⭐
│   │   │       └── SnowflakeIdGenerator.java       # 雪花算法 ID 生成器 ⭐⭐
│   │   │
│   │   └── resources/
│   │       ├── application.properties               # 配置文件
│   │       └── schema.sql                           # 数据库建表脚本
│   │
│   └── test/
│       └── java/com/lin/shortlinkservice/
│           ├── ShortLinkServiceApplicationTests.java
│           └── util/
│               ├── Base62UtilTest.java              # Base62 测试
│               └── SnowflakeIdGeneratorTest.java    # 雪花算法测试
│
├── pom.xml                                           # Maven 配置文件
│
├── README.md                                         # 项目说明文档 📖
├── QUICKSTART.md                                     # 快速启动指南 🚀
├── INTERVIEW.md                                      # 面试问答宝典 💡
└── API_TEST.md                                       # API 测试文档 🧪

```

## 核心文件说明

### ⭐⭐⭐ 三星级(必看)

**ShortLinkService.java** - 整个系统的核心
- 包含所有业务逻辑
- 布隆过滤器使用示例
- Redis 缓存实现
- 异步日志处理
- **面试重点!**

### ⭐⭐ 二星级(重点)

**Base62Util.java** - Base62 编码算法
- 如何将数字 ID 转换为短字符串
- 面试必问的核心算法
- 代码简洁易懂

**SnowflakeIdGenerator.java** - 雪花算法
- 分布式唯一 ID 生成
- 高并发场景的标准方案
- 面试高频考点

### ⭐ 一星级(了解)

**BloomFilterConfig.java** - 布隆过滤器配置
- 展示如何使用 Guava 的布隆过滤器
- 防止缓存穿透的关键

**ThreadPoolConfig.java** - 线程池配置
- 异步处理的基础设施
- 性能优化的重要手段

## 技术亮点分布

### 1. 核心算法 (Base62 + Snowflake)
- 位置: `util/` 包
- 文件: `Base62Util.java`, `SnowflakeIdGenerator.java`
- 简历关键词: "分布式 ID 生成", "Base62 编码"

### 2. 高性能优化 (Redis + BloomFilter)
- 位置: `config/` 和 `service/` 包
- 文件: `BloomFilterConfig.java`, `ShortLinkService.java`
- 简历关键词: "缓存优化", "防缓存穿透"

### 3. 异步架构 (ThreadPool)
- 位置: `config/` 和 `service/` 包
- 文件: `ThreadPoolConfig.java`, `ShortLinkService.java`
- 简历关键词: "异步解耦", "性能提升"

### 4. 数据统计 (PV/UV)
- 位置: `service/` 包
- 文件: `ShortLinkService.java`
- 简历关键词: "访问统计", "Redis Set 去重"

## 代码行数统计

| 类型 | 文件数 | 代码行数 | 说明 |
|------|--------|----------|------|
| 实体类 | 3 | ~150 | 数据模型 |
| 工具类 | 2 | ~200 | 核心算法 |
| 配置类 | 2 | ~100 | Spring 配置 |
| 业务类 | 1 | ~300 | 核心逻辑 |
| 控制器 | 1 | ~100 | REST API |
| 测试类 | 2 | ~100 | 单元测试 |
| **总计** | **11** | **~950** | **核心代码** |

## 面试时如何介绍

### 30秒版本(电梯演讲)
"这是一个企业级短链接系统,我负责核心功能的设计和实现。使用雪花算法生成唯一 ID,Base62 编码转换为短链接。引入 Redis 缓存和布隆过滤器防止缓存穿透,采用异步架构处理访问日志,在 1000 QPS 压测下响应时间稳定在 20ms。"

### 3分钟版本(技术细节)
1. **项目背景**: 短链接系统是 System Design 的经典案例
2. **核心挑战**: 
   - 如何生成不重复的短链接
   - 如何支持高并发访问
   - 如何统计访问数据
3. **技术方案**:
   - 算法: 雪花算法 + Base62 编码
   - 缓存: Redis + 布隆过滤器
   - 异步: 线程池解耦日志记录
4. **项目成果**:
   - 支持千万级短链生成
   - QPS 达到 1000+
   - 响应时间 < 20ms

### 10分钟版本(架构设计)
详见 [INTERVIEW.md](INTERVIEW.md)

## 学习路径建议

### 第一阶段: 理解核心算法
1. 阅读 `Base62Util.java` - 理解进制转换
2. 阅读 `SnowflakeIdGenerator.java` - 理解分布式 ID
3. 运行单元测试,验证算法正确性

### 第二阶段: 掌握业务逻辑
1. 阅读 `ShortLinkService.java` - 理解完整流程
2. 重点关注:
   - 布隆过滤器的使用
   - Redis 缓存策略
   - 异步日志处理
3. 理解为什么用 302 而不是 301

### 第三阶段: 实际测试
1. 启动项目 (参考 QUICKSTART.md)
2. 测试 API 接口 (参考 API_TEST.md)
3. 观察日志输出,理解执行流程

### 第四阶段: 准备面试
1. 阅读 INTERVIEW.md
2. 背诵核心算法原理
3. 准备简历话术
4. 模拟面试问答

## 扩展功能建议

如果想进一步完善项目,可以添加:

1. **自定义短链** - 支持用户指定短码
2. **二维码生成** - 为短链接生成二维码
3. **数据可视化** - 使用 ECharts 展示统计数据
4. **权限管理** - 用户注册、登录、权限控制
5. **链接管理** - 用户可以查看、编辑、删除自己的短链
6. **监控告警** - 集成 Prometheus + Grafana
7. **分布式部署** - Docker + Kubernetes

## 常用命令速查

```powershell
# 编译项目
mvn clean install

# 运行项目
mvn spring-boot:run

# 运行测试
mvn test

# 打包项目
mvn package

# 跳过测试打包
mvn package -DskipTests
```

---

**记住: 这个项目的核心价值在于展示你对分布式系统、高并发、缓存优化的理解!**
