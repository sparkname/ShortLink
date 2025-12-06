# 企业级部署指南

## 📋 部署清单

### 必需组件
- ✅ MySQL 主库 (端口 3306)
- ✅ MySQL 从库 (端口 3307,可选,开发环境可指向主库)
- ✅ Redis (端口 6379)
- ✅ RabbitMQ (端口 5672, 管理界面 15672)
- ✅ JDK 17+

---

## 🚀 完整部署流程

### 方案一: Docker 快速部署(推荐)

#### 1. 启动 RabbitMQ
```powershell
docker run -d --name rabbitmq `
  -p 5672:5672 `
  -p 15672:15672 `
  -e RABBITMQ_DEFAULT_USER=admin `
  -e RABBITMQ_DEFAULT_PASS=admin123 `
  rabbitmq:3-management
```

**验证:**
访问 http://localhost:15672  
账号: admin / admin123

#### 2. 启动 Redis
```powershell
docker run -d --name redis `
  -p 6379:6379 `
  redis:latest
```

**验证:**
```powershell
docker exec -it redis redis-cli ping
# 返回 PONG 表示成功
```

#### 3. 启动 MySQL 主库
```powershell
docker run -d --name mysql-master `
  -p 3306:3306 `
  -e MYSQL_ROOT_PASSWORD=123456 `
  -e MYSQL_DATABASE=short_link_db `
  mysql:8.0
```

#### 4. 启动 MySQL 从库(可选)
```powershell
docker run -d --name mysql-slave `
  -p 3307:3306 `
  -e MYSQL_ROOT_PASSWORD=123456 `
  mysql:8.0
```

**简化配置(开发环境):**  
如果不想配置主从复制,可以让从库也指向主库:
```properties
# application.properties
spring.datasource.slave.url=jdbc:mysql://localhost:3306/short_link_db
```

---

### 方案二: 本地安装

#### 1. 安装 RabbitMQ

**Windows:**
1. 下载并安装 Erlang: https://www.erlang.org/downloads
2. 下载并安装 RabbitMQ: https://www.rabbitmq.com/download.html
3. 启动服务:
```powershell
rabbitmq-plugins enable rabbitmq_management
net start RabbitMQ
```

**Linux:**
```bash
sudo apt-get install rabbitmq-server
sudo rabbitmq-plugins enable rabbitmq_management
sudo systemctl start rabbitmq-server
```

#### 2. 安装 Redis

**Windows:**
1. 下载: https://github.com/microsoftarchive/redis/releases
2. 解压并运行: `redis-server.exe`

**Linux:**
```bash
sudo apt-get install redis-server
sudo systemctl start redis
```

#### 3. 安装 MySQL

按照官方文档安装 MySQL 8.0+

---

## 🔧 MySQL 主从复制配置(可选)

### 主库配置

**1. 修改配置文件 (my.ini 或 my.cnf)**
```ini
[mysqld]
# 服务器ID(唯一)
server-id=1

# 开启二进制日志
log-bin=mysql-bin

# 日志格式
binlog-format=ROW

# 要同步的数据库
binlog-do-db=short_link_db
```

**2. 重启 MySQL**
```powershell
# Windows
net stop MySQL80
net start MySQL80

# Linux
sudo systemctl restart mysql
```

**3. 创建复制用户**
```sql
CREATE USER 'repl'@'%' IDENTIFIED WITH mysql_native_password BY 'repl123';
GRANT REPLICATION SLAVE ON *.* TO 'repl'@'%';
FLUSH PRIVILEGES;

-- 查看主库状态
SHOW MASTER STATUS;
-- 记住 File 和 Position
```

### 从库配置

**1. 修改配置文件**
```ini
[mysqld]
# 服务器ID(必须不同)
server-id=2

# 中继日志
relay-log=relay-log

# 只读模式
read-only=1
```

**2. 重启 MySQL**

**3. 配置主从关系**
```sql
CHANGE MASTER TO
  MASTER_HOST='localhost',
  MASTER_PORT=3306,
  MASTER_USER='repl',
  MASTER_PASSWORD='repl123',
  MASTER_LOG_FILE='mysql-bin.000001',  -- 主库的File
  MASTER_LOG_POS=0;                    -- 主库的Position

-- 启动复制
START SLAVE;

-- 查看状态
SHOW SLAVE STATUS\G
-- Slave_IO_Running: Yes
-- Slave_SQL_Running: Yes
-- 表示成功
```

---

## 📦 应用部署

### 1. 初始化数据库

```powershell
# 连接主库
mysql -u root -p -h localhost -P 3306

# 执行建表脚本
source src/main/resources/schema.sql
```

### 2. 修改配置文件

编辑 `src/main/resources/application.properties`:

```properties
# 主库配置
spring.datasource.master.url=jdbc:mysql://localhost:3306/short_link_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.master.username=root
spring.datasource.master.password=123456

# 从库配置(如果没有从库,填写主库地址)
spring.datasource.slave.url=jdbc:mysql://localhost:3307/short_link_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
spring.datasource.slave.username=root
spring.datasource.slave.password=123456

# RabbitMQ配置
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

# Redis配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. 编译打包

```powershell
mvn clean package -DskipTests
```

### 4. 运行应用

**方式1: Maven**
```powershell
mvn spring-boot:run
```

**方式2: JAR包**
```powershell
java -jar target/short-link-service-0.0.1-SNAPSHOT.jar
```

**方式3: IDEA**  
直接运行 `ShortLinkServiceApplication`

---

## 🧪 功能测试

### 1. 健康检查

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health"
```

预期返回:
```json
{
  "code": 200,
  "message": "success",
  "data": "OK"
}
```

### 2. 测试生成短链接(写主库)

```powershell
$body = @{
    originalUrl = "https://github.com/example/repo"
    expireDays = 30
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/short-link" `
    -Method Post `
    -Body $body `
    -ContentType "application/json"

$response
```

预期返回:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortUrl": "http://localhost:8080/2Bi",
    "shortCode": "2Bi",
    "originalUrl": "https://github.com/example/repo"
  }
}
```

### 3. 测试短链接跳转(读从库)

```powershell
# 在浏览器中访问
Start-Process "http://localhost:8080/2Bi"
```

应该跳转到原始链接。

### 4. 查看 RabbitMQ 消息

访问: http://localhost:15672  
点击 Queues → `short_link.access_log.queue`  
查看消息数量,应该能看到访问日志消息。

### 5. 查询统计(读从库)

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/stats/2Bi"
```

预期返回:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortCode": "2Bi",
    "originalUrl": "https://github.com/example/repo",
    "pv": 5,
    "uv": 2
  }
}
```

---

## 🔍 验证读写分离

### 方法1: 查看日志

启动应用后,观察控制台输出的 SQL 日志:

**写操作(主库):**
```
Creating new JDBC Connection for DataSource [master]
Executing SQL: INSERT INTO short_link ...
```

**读操作(从库):**
```
Creating new JDBC Connection for DataSource [slave]
Executing SQL: SELECT * FROM short_link ...
```

### 方法2: 使用 MySQL 日志

**主库:**
```sql
-- 查看最近的写操作
SHOW BINLOG EVENTS IN 'mysql-bin.000001' LIMIT 10;
```

**从库:**
```sql
-- 查看复制状态
SHOW SLAVE STATUS\G

-- Seconds_Behind_Master: 0 表示无延迟
```

---

## 📊 监控 RabbitMQ

### 查看队列状态

访问: http://localhost:15672

**关键指标:**
- **Ready**: 待消费的消息数
- **Unacked**: 正在处理的消息数
- **Total**: 总消息数
- **Publish Rate**: 生产速率
- **Consume Rate**: 消费速率

**健康标准:**
- Ready < 10000 (消息积压不严重)
- Consume Rate ≈ Publish Rate (生产消费平衡)

---

## ⚠️ 常见问题

### Q1: RabbitMQ 连接失败

**错误:** `Connection refused`

**解决:**
```powershell
# 检查 RabbitMQ 是否启动
docker ps | Select-String rabbitmq

# 查看日志
docker logs rabbitmq
```

### Q2: MySQL 从库同步失败

**错误:** `Slave_IO_Running: No`

**解决:**
```sql
-- 在从库执行
STOP SLAVE;
RESET SLAVE;

-- 重新配置主从
CHANGE MASTER TO ...
START SLAVE;
```

### Q3: 布隆过滤器初始化失败

**错误:** `Could not initialize Bloom Filter`

**原因:** 数据库连接失败或表不存在

**解决:**
1. 检查数据库是否启动
2. 确认已执行 schema.sql
3. 检查配置文件中的数据库地址和密码

### Q4: 消息消费失败

**现象:** RabbitMQ 中消息一直积压

**排查:**
1. 查看应用日志是否有异常
2. 检查数据库连接是否正常
3. 查看死信队列中的消息

**解决:**
```java
// 查看消费者日志
log.error("处理访问日志失败: {}", message.getShortCode(), e);
```

---

## 🎯 压力测试

### 使用 Apache Bench

**测试跳转接口:**
```powershell
# 先创建一个短链接
# 假设短码是 2Bi

# 10000 请求, 100 并发
ab -n 10000 -c 100 http://localhost:8080/2Bi
```

**预期结果:**
- Requests per second: > 1000
- Time per request: < 100ms

### 观察 RabbitMQ

压测期间访问 RabbitMQ 管理界面:
- 消息数量应该快速增长
- 消费者稳定消费
- 无消息丢失

---

## 🚀 生产环境优化

### 1. MySQL 优化

```ini
[mysqld]
# 连接数
max_connections=1000

# 缓存
innodb_buffer_pool_size=2G

# 日志
slow_query_log=1
long_query_time=1
```

### 2. Redis 优化

```conf
# 最大内存
maxmemory 2gb

# 淘汰策略
maxmemory-policy allkeys-lru

# 持久化
save 900 1
save 300 10
```

### 3. RabbitMQ 优化

```
# 消息持久化
durable=true

# 预取数量
prefetch-count=10

# 消费者数量(可配置多个)
consumers=5
```

### 4. 应用优化

```properties
# JVM 参数
-Xms2g -Xmx2g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200

# 数据库连接池
spring.datasource.master.maximum-pool-size=20
spring.datasource.slave.maximum-pool-size=50
```

---

**部署完成! 🎉**

检查清单:
- ✅ MySQL 主库运行正常
- ✅ MySQL 从库复制正常(可选)
- ✅ Redis 连接成功
- ✅ RabbitMQ 队列创建成功
- ✅ 应用启动无报错
- ✅ 短链接生成和跳转正常
- ✅ 消息队列消费正常
- ✅ 统计数据正确
