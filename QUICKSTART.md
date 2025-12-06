# 快速启动指南

## 前置准备

### 1. 安装 MySQL
- 下载并安装 MySQL 8.0+
- 创建数据库和表

### 2. 安装 Redis
- Windows: 下载 Redis for Windows
- 启动 Redis 服务

## 启动步骤

### Step 1: 初始化数据库

打开 MySQL 命令行:

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS short_link_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE short_link_db;

-- 执行 schema.sql 中的建表语句
-- (复制 src/main/resources/schema.sql 的内容执行)
```

或者直接执行:
```powershell
mysql -u root -p < src/main/resources/schema.sql
```

### Step 2: 修改配置

编辑 `src/main/resources/application.properties`:

```properties
# 修改数据库密码
spring.datasource.password=你的MySQL密码

# 确认 Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### Step 3: 启动 Redis

**Windows PowerShell:**
```powershell
# 如果已安装 Redis
redis-server
```

**或者使用 Docker:**
```powershell
docker run -d -p 6379:6379 redis:latest
```

### Step 4: 编译项目

```powershell
mvn clean install
```

### Step 5: 运行项目

```powershell
mvn spring-boot:run
```

或者在 IDEA 中直接运行 `ShortLinkServiceApplication`

### Step 6: 验证启动

访问健康检查接口:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health"
```

如果返回:
```json
{
  "code": 200,
  "message": "success",
  "data": "OK"
}
```
说明启动成功! 🎉

## 快速测试

### 1. 生成短链接

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/short-link" `
    -Method Post `
    -Body (@{originalUrl="https://github.com"; expireDays=30} | ConvertTo-Json) `
    -ContentType "application/json"

# 查看完整响应
$response | ConvertTo-Json -Depth 3

# 获取短码
$shortCode = $response.data.shortCode
Write-Host "短码是: $shortCode"
Write-Host "完整短链接: http://localhost:8080/$shortCode"
```

### 2. 访问短链接

假设生成的短码是 `2Bi`,在浏览器中访问:
```
http://localhost:8080/2Bi
```

应该会跳转到原始链接。

### 3. 查看统计

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/stats/2Bi"
```

## 常见问题

### Q1: 启动时报错 "Access denied for user"
**解决:** 检查 `application.properties` 中的数据库密码是否正确

### Q2: 启动时报错 "Could not connect to Redis"
**解决:** 确保 Redis 服务已启动,端口是 6379

### Q3: 启动时报错 "Table doesn't exist"
**解决:** 执行 `schema.sql` 创建数据库表

### Q4: 无法访问 8080 端口
**解决:** 检查端口是否被占用,可以修改 `application.properties` 中的端口

### Q5: 布隆过滤器初始化失败
**解决:** 确保数据库连接正常,首次启动可能需要几秒钟

## 开发模式

### 热部署

添加 Spring Boot DevTools 依赖后,修改代码会自动重启:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
</dependency>
```

### 调试模式

IDEA 中点击 Debug 按钮,设置断点进行调试。

## 下一步

- 查看 [README.md](README.md) 了解完整功能
- 查看 [INTERVIEW.md](INTERVIEW.md) 准备面试
- 查看 [API_TEST.md](API_TEST.md) 进行接口测试

---

**祝你面试顺利! 🚀**
