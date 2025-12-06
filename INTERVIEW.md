# 面试问答宝典

## 核心概念

### 1. 什么是短链接系统?

**简答:** 短链接系统将长 URL 转换为短 URL,方便分享和传播。

**详细回答:**
短链接系统是一种 URL 缩短服务,它的核心功能是:
1. 将冗长的 URL 转换为简短的 URL (如 t.cn/Ab3xZ)
2. 通过短链接跳转到原始长链接
3. 统计访问数据 (PV/UV/IP等)

**应用场景:**
- 微博/Twitter 字数限制
- 美化分享链接
- 追踪营销效果
- 隐藏真实链接

---

## 算法相关

### 2. Base62 是什么?为什么不用 Hash?

**Base62 定义:**
- 使用 0-9, a-z, A-Z 共 62 个字符
- 类似于十进制转二进制,只是转 62 进制
- 示例: 10000 → "2Bi"

**为什么不用 Hash:**
1. **长度问题:** MD5/SHA1 生成的字符串太长
2. **冲突问题:** Hash 有天然的冲突概率
3. **不可控:** Hash 输出长度固定,无法自定义

**Base62 优势:**
- 输出短 (7位可表示 62^7 = 3.5万亿)
- URL 友好 (纯数字和字母)
- 无冲突 (基于唯一 ID)

---

### 3. 雪花算法是什么?如何保证唯一性?

**结构 (64位):**
```
┌─────┬──────────────────┬──────────┬────────────┐
│ 1bit│   41bit 时间戳   │ 10bit 机器│ 12bit 序列 │
└─────┴──────────────────┴──────────┴────────────┘
```

**保证唯一性的方式:**
1. **时间戳:** 精确到毫秒,41位可用 69 年
2. **机器 ID:** 10位支持 1024 台机器
3. **序列号:** 12位每毫秒可生成 4096 个 ID

**优势:**
- 趋势递增,利于 MySQL 索引
- 不依赖数据库,性能高
- 分布式友好

**缺点:**
- 依赖系统时钟
- 时钟回拨会导致 ID 重复

---

## 缓存相关

### 4. 什么是缓存穿透?如何防止?

**定义:**
大量请求查询不存在的数据,导致请求直接打到数据库。

**场景:**
黑客疯狂请求 `http://t.cn/NotExist`,每次都查数据库。

**解决方案:**

**方案1: 布隆过滤器 (本项目采用)**
```java
if (!bloomFilter.mightContain(shortCode)) {
    return null; // 直接拦截
}
```
- 优点: 空间小,速度快
- 缺点: 有 1% 误判率

**方案2: 缓存空对象**
```java
redisTemplate.set("empty:" + shortCode, "", 5分钟);
```
- 优点: 实现简单
- 缺点: 占用 Redis 内存

---

### 5. 布隆过滤器的原理?

**原理:**
1. 使用多个 Hash 函数将元素映射到位数组
2. 判断元素存在: 检查对应位是否都为 1
3. 如果都为 1 → 可能存在 (误判)
4. 如果有 0 → 一定不存在 (零漏判)

**示例:**
```
添加 "abc": Hash1("abc")=3, Hash2("abc")=7, Hash3("abc")=11
位数组: [0,0,0,1,0,0,0,1,0,0,0,1,0,0,0,0]
         ↑       ↑       ↑       ↑
         3       7      11
```

**配置参数:**
- 预期数据量: 1000万
- 误判率: 1%
- 占用空间: 约 12MB

**面试加分项:**
"布隆过滤器的误判只会出现在判断存在时,判断不存在时绝对准确。"

---

## 并发相关

### 6. 为什么要异步处理日志?

**问题:**
如果同步处理,用户跳转流程:
```
收到请求 → 查询 URL → 记录日志(100ms) → 返回跳转
```
用户要等 100ms 才能跳转。

**优化后:**
```
收到请求 → 查询 URL → 返回跳转 (20ms)
                   ↓
              异步记录日志(100ms)
```

**实现方式:**
```java
asyncLogExecutor.execute(() -> {
    // 记录日志
    // 更新统计
});
```

**效果:**
- 响应时间从 120ms 降到 20ms
- 吞吐量提升 5 倍

---

### 7. 如何保证高并发下的性能?

**多层优化:**

**1. 布隆过滤器 (第一道防线)**
- 拦截 99% 的无效请求
- 内存判断,极快

**2. Redis 缓存 (第二道防线)**
- 缓存热点数据
- 命中率 > 95%

**3. 数据库索引 (最后防线)**
- short_code 唯一索引
- 查询时间 < 5ms

**4. 异步处理**
- 日志落库不阻塞主流程

**最终效果:**
- 单机 QPS > 1000
- 响应时间 < 20ms

---

## 重定向相关

### 8. 301 vs 302,面试必考!

| 状态码 | 名称 | 浏览器行为 | 能否统计 | 使用场景 |
|--------|------|-----------|---------|----------|
| 301 | 永久重定向 | **会缓存** | ❌ | 域名更换 |
| 302 | 临时重定向 | **不缓存** | ✅ | 短链接跳转 |

**详细解释:**

**301 的问题:**
```
第一次访问: 浏览器 → 服务器 → 长链接
第二次访问: 浏览器 → (缓存) → 长链接 (不经过服务器)
```
无法统计第二次访问!

**302 的优势:**
```
每次访问: 浏览器 → 服务器 → 长链接
```
每次都经过服务器,可以统计所有访问。

**标准答案:**
"短链接系统必须使用 302,因为需要统计每次访问的数据,包括 PV/UV/IP 等。如果使用 301,浏览器会缓存结果,导致后续访问不经过服务器,无法统计。"

---

## 数据库相关

### 9. 数据库表结构如何设计?

**核心表:**

**1. short_link (短链接主表)**
```sql
CREATE TABLE short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10) UNIQUE,  -- 短码
    original_url VARCHAR(2048),     -- 长链接
    create_time DATETIME,
    expire_time DATETIME,
    status TINYINT,
    INDEX idx_short_code(short_code)  -- 核心索引!
);
```

**2. access_log (访问日志表)**
```sql
CREATE TABLE access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(10),
    ip VARCHAR(64),
    access_time DATETIME,
    INDEX idx_short_code(short_code),
    INDEX idx_access_time(access_time)
);
```

**3. access_stats (统计表)**
```sql
CREATE TABLE access_stats (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(10) UNIQUE,
    pv BIGINT DEFAULT 0,
    uv BIGINT DEFAULT 0
);
```

**设计要点:**
1. short_code 必须建唯一索引
2. 访问日志和统计分开 (读写分离)
3. 统计表预聚合,避免实时计算

---

### 10. 如何统计 UV?

**方案1: Redis Set (本项目采用)**
```java
String key = "uv:" + shortCode;
Boolean isNew = redisTemplate.opsForSet().add(key, clientIp);
if (isNew) {
    statsMapper.incrementUv(shortCode);
}
```
**优点:** 自动去重,实时准确  
**缺点:** 占用 Redis 内存

**方案2: HyperLogLog**
```java
redisTemplate.opsForHyperLogLog().add(key, clientIp);
long uv = redisTemplate.opsForHyperLogLog().size(key);
```
**优点:** 极省内存 (12KB 统计千万数据)  
**缺点:** 有 0.81% 误差

**方案3: 布隆过滤器 + 计数器**
- 优点: 内存小
- 缺点: 实现复杂

---

## 扩展问题

### 11. 如果要支持自定义短链怎么办?

**需求:** 用户想要 `t.cn/alibaba` 这样的个性化短链

**实现思路:**

**1. 数据表增加字段:**
```sql
ALTER TABLE short_link ADD COLUMN custom_code VARCHAR(20);
```

**2. 生成逻辑修改:**
```java
if (request.getCustomCode() != null) {
    // 检查是否已被占用
    if (存在) {
        return error("自定义短码已被占用");
    }
    shortCode = request.getCustomCode();
} else {
    // 系统自动生成
    shortCode = Base62Util.encode(id);
}
```

**3. 注意事项:**
- 自定义短码可能与系统生成的冲突
- 需要设置长度限制 (如 3-20 字符)
- 需要屏蔽敏感词

---

### 12. 如何防止恶意生成大量短链接?

**方案:**

**1. IP 限流:**
```java
String key = "rate_limit:" + ip;
Long count = redisTemplate.opsForValue().increment(key);
if (count == 1) {
    redisTemplate.expire(key, 1, TimeUnit.HOURS);
}
if (count > 100) {
    return error("请求过于频繁");
}
```

**2. 用户登录:**
- 限制只有登录用户才能生成
- 每个用户每天限制生成数量

**3. 验证码:**
- 高频请求时要求输入验证码

**4. 付费套餐:**
- 免费用户每天 10 个
- 付费用户每天 1000 个

---

### 13. 如果短链接被恶意访问怎么办?

**监控指标:**
- PV 突然暴增
- 来自同一 IP 的大量请求
- User-Agent 异常

**防护措施:**

**1. IP 黑名单:**
```java
if (redisTemplate.opsForSet().isMember("blacklist", ip)) {
    return error("访问受限");
}
```

**2. 访问频率限制:**
```java
String key = "access:" + ip + ":" + shortCode;
Long count = redisTemplate.opsForValue().increment(key);
if (count > 100) { // 1分钟超过100次
    return error("访问过于频繁");
}
redisTemplate.expire(key, 1, TimeUnit.MINUTES);
```

**3. 链接暂停:**
```sql
UPDATE short_link SET status = 0 WHERE short_code = ?;
```

---

## 性能优化

### 14. 如何优化到 QPS 10000+?

**当前瓶颈分析:**

**1. Redis 单线程限制**
- 解决: Redis 集群 + 读写分离
- 主节点写,从节点读

**2. 数据库连接数限制**
- 解决: 提高连接池大小
- 读写分离,主库写,从库读

**3. 单机 CPU 限制**
- 解决: 负载均衡 + 多实例部署
- Nginx → 多个 Spring Boot 实例

**4. 网络 IO**
- 解决: CDN 加速

**最终架构:**
```
         Nginx (负载均衡)
            ↓
    ┌───────┼───────┐
    ↓       ↓       ↓
  App1    App2    App3
    ↓       ↓       ↓
  ┌─────────┴─────────┐
  ↓                   ↓
Redis 集群        MySQL 主从
```

---

### 15. 如何实现读写分离?

**数据库层面:**

**1. MySQL 主从复制**
```
主库 (写) → 同步 → 从库 (读)
```

**2. ShardingSphere 配置**
```yaml
spring:
  shardingsphere:
    datasource:
      master: # 主库
        type: com.zaxxer.hikari.HikariDataSource
        url: jdbc:mysql://master:3306/db
      slave: # 从库
        type: com.zaxxer.hikari.HikariDataSource
        url: jdbc:mysql://slave:3306/db
    rules:
      readwrite-splitting:
        data-sources:
          ds:
            write-data-source-name: master
            read-data-source-names: slave
```

**代码层面:**
```java
@Transactional // 走主库
public void create() { ... }

@Transactional(readOnly = true) // 走从库
public Data query() { ... }
```

---

## 总结

### 简历上的三句话

**1. 核心算法:**
"采用雪花算法生成分布式唯一 ID,结合 Base62 编码转换为短字符串,解决了 Hash 冲突问题,单机支持千万级唯一 ID 生成。"

**2. 性能优化:**
"引入 Redis 缓存 + 布隆过滤器双重防护,有效防止缓存穿透,在 1000 QPS 压测下,接口响应时间稳定在 20ms 以内。"

**3. 异步架构:**
"采用异步架构(线程池)处理访问日志落库,将核心跳转接口与数据统计解耦,提升了系统吞吐量 5 倍。"

---

**关键词记忆:**
- Base62 编码
- 雪花算法
- 布隆过滤器
- Redis 缓存
- 异步处理
- 302 重定向
- 读写分离
- 缓存穿透
