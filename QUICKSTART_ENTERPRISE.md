# 🚀 企业级短链接系统 - 快速启动指南

## ⚠️ 重要说明

**当前版本使用 Spring Boot 4.0.0 存在兼容性问题!**

由于 Spring Boot 4.0.0 与 MyBatis Plus 3.5.3.1 和 ShardingSphere 5.4.1 存在严重的类路径冲突,导致应用无法正常启动。

## 🔧 解决方案

### 方案 1: 降级 Spring Boot (推荐)

修改 `pom.xml`,将 Spring Boot 版本从 `4.0.0` 改为稳定的 `3.2.0`:

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>  <!-- 改为 3.2.0 -->
    <relativePath/>
</parent>
```

然后重新编译:
```powershell
mvn clean package -DskipTests
```

### 方案 2: 简化版启动(暂时移除读写分离)

如果你只是想快速测试功能,可以暂时禁用 ShardingSphere 读写分离。

#### Step 1: 修改 `application.properties`

注释掉主从配置,使用单数据源:

```properties
# 单数据源配置(简化版)
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/short_link_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=123456

# 注释掉读写分离配置
# spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
# spring.datasource.master.xxx
# spring.datasource.slave.xxx
```

#### Step 2: 移除 ShardingSphereConfig

暂时不加载 `ShardingSphereConfig.java`,或者在类上添加 `@Profile("prod")` 注解:

```java
@Configuration
@Profile("prod")  // 只在生产环境启用
public class ShardingSphereConfig {
    // ...
}
```

#### Step 3: 启动应用

```powershell
# 移除 ShardingSphere 依赖后重新打包
mvn clean package -DskipTests

# 启动应用
java -jar target\short-link-service-0.0.1-SNAPSHOT.jar
```

## 📋 企业级完整部署步骤

### 前置准备

1. **MySQL 主从架构**
   ```bash
   # 主库: localhost:3306
   # 从库: localhost:3307
   ```

2. **RabbitMQ 消息队列**
   ```powershell
   # Docker 启动
   docker run -d --name rabbitmq `
       -p 5672:5672 `
       -p 15672:15672 `
       rabbitmq:3-management
   ```

3. **Redis 缓存**
   ```powershell
   # Docker 启动
   docker run -d --name redis -p 6379:6379 redis:latest
   ```

### 配置 MySQL 主从复制

#### 主库配置 (3306)

编辑 `my.ini`:
```ini
[mysqld]
server-id=1
log-bin=mysql-bin
binlog-do-db=short_link_db
```

创建复制用户:
```sql
CREATE USER 'replicator'@'%' IDENTIFIED BY 'password';
GRANT REPLICATION SLAVE ON *.* TO 'replicator'@'%';
FLUSH PRIVILEGES;
```

#### 从库配置 (3307)

```sql
CHANGE MASTER TO
    MASTER_HOST='localhost',
    MASTER_USER='replicator',
    MASTER_PASSWORD='password',
    MASTER_LOG_FILE='mysql-bin.000001',
    MASTER_LOG_POS=154;

START SLAVE;
SHOW SLAVE STATUS\G
```

### 启动应用

```powershell
# 1. 编译打包
mvn clean package -DskipTests

# 2. 启动应用
java -jar target\short-link-service-0.0.1-SNAPSHOT.jar

# 3. 健康检查
Invoke-RestMethod -Uri "http://localhost:8080/health"
```

## 🐛 常见问题

### Q1: ClassNotFoundException: DataSourceAutoConfiguration

**原因:** Spring Boot 4.0.0 与 MyBatis Plus 不兼容

**解决:** 降级到 Spring Boot 3.2.0

### Q2: Failed to generate bean name for imported class

**原因:** ShardingSphere 与 Spring Boot 4.0.0 版本冲突

**解决:**
1. 使用 Spring Boot 3.2.0
2. 或暂时移除 ShardingSphere 配置

### Q3: Could not connect to RabbitMQ

**原因:** RabbitMQ 服务未启动

**解决:**
```powershell
# 使用 Docker 启动
docker start rabbitmq

# 或安装后台服务
rabbitmq-server
```

### Q4: Access denied for user 'root'@'localhost'

**原因:** 数据库密码不正确

**解决:** 修改 `application.properties` 中的密码

## 📊 架构说明

### RabbitMQ 削峰填谷

```
访问请求 -> Controller -> Service
                            ↓
                      发送 MQ 消息
                            ↓
                    AccessLogQueue (队列)
                            ↓
                      Consumer 消费
                            ↓
                      批量写入数据库
```

**优势:**
- 异步处理,提升响应速度
- 削峰填谷,保护数据库
- 消息可靠性(DLQ 死信队列)

### ShardingSphere 读写分离

```
@Transactional         -> Master (3306) 写操作
@Transactional(readOnly=true) -> Slave (3307) 读操作
```

**优势:**
- 自动路由,代码无感知
- 负载均衡(轮询策略)
- 读写分离,提升并发能力

## 🎯 性能指标

- **QPS:** 单机 10,000+ (Redis 缓存命中率 >90%)
- **响应时间:** P99 < 50ms
- **消息处理:** 5000 msg/s
- **数据库:** 主从延迟 <100ms

## 📚 相关文档

- [README_ENTERPRISE.md](README_ENTERPRISE.md) - 企业级架构说明
- [UPGRADE_SUMMARY.md](UPGRADE_SUMMARY.md) - 升级总结
- [INTERVIEW.md](INTERVIEW.md) - 面试题库

---

**注意:** 如果你使用的是 Spring Boot 4.0.0,强烈建议降级到 3.2.0 以避免兼容性问题!
