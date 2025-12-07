# 短链接系统性能压测指南

本文档详细说明如何使用 JMeter 对短链接系统进行性能压测,并对比 Redis 缓存前后的性能差异。

## 📊 压测目标

- **验证缓存效果**: 对比开启/关闭 Redis 前后的性能差异
- **获取量化数据**: 记录 TPS、响应时间等关键指标
- **发现性能瓶颈**: 找出系统的性能极限
- **简历数据支撑**: 获得真实的性能数据用于面试

## 🛠️ 工具安装

### 1. 安装 JMeter

#### Windows 系统

1. **下载 JMeter**
   - 访问: https://jmeter.apache.org/download_jmeter.cgi
   - 下载最新版本 (推荐 5.6.x)
   - 或使用以下命令直接下载:
   ```powershell
   # 使用 PowerShell 下载
   Invoke-WebRequest -Uri "https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.zip" -OutFile "jmeter.zip"
   ```

2. **解压并配置环境变量**
   ```powershell
   # 解压到指定目录
   Expand-Archive -Path jmeter.zip -DestinationPath C:\jmeter
   
   # 添加到 PATH (临时)
   $env:PATH += ";C:\jmeter\apache-jmeter-5.6.3\bin"
   ```

3. **验证安装**
   ```powershell
   jmeter --version
   ```

#### macOS/Linux 系统

```bash
# 使用 Homebrew (macOS)
brew install jmeter

# 或手动下载解压
wget https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-5.6.3.tgz
tar -xzf apache-jmeter-5.6.3.tgz
cd apache-jmeter-5.6.3/bin
./jmeter --version
```

### 2. 安装 JMeter 插件 (可选但推荐)

安装 Plugins Manager 以获取更好的图表和监控功能:

1. 下载 Plugins Manager: https://jmeter-plugins.org/get/
2. 将 jar 文件放到 `lib/ext` 目录
3. 重启 JMeter

## 📝 JMeter 测试计划配置

### 方式一: 使用提供的测试脚本

直接使用本目录下的 `ShortLink-LoadTest.jmx` 文件:

```powershell
# 启动 JMeter GUI
jmeter -t ShortLink-LoadTest.jmx
```

### 方式二: 手动创建测试计划

#### 步骤 1: 创建线程组

1. 右键点击 `Test Plan` → `Add` → `Threads (Users)` → `Thread Group`
2. 配置参数:
   - **Number of Threads (users)**: 100 (模拟 100 个并发用户)
   - **Ramp-up Period (seconds)**: 10 (10 秒内启动所有线程)
   - **Loop Count**: 100 (每个线程执行 100 次请求)

#### 步骤 2: 添加 HTTP 请求

1. 右键点击 `Thread Group` → `Add` → `Sampler` → `HTTP Request`
2. 配置创建短链接请求:
   - **Name**: 创建短链接
   - **Server Name or IP**: localhost
   - **Port Number**: 8080
   - **Method**: POST
   - **Path**: /api/short-link
   - **Body Data**:
   ```json
   {
     "originalUrl": "https://example.com/test/${__UUID()}",
     "expirationHours": 24
   }
   ```

3. 添加第二个 HTTP 请求用于跳转测试:
   - **Name**: 短链接跳转
   - **Server Name or IP**: localhost
   - **Port Number**: 8080
   - **Method**: GET
   - **Path**: /${shortCode} (使用正则提取器从上一步获取)

#### 步骤 3: 添加监听器

右键点击 `Thread Group` → `Add` → `Listener`:

1. **View Results Tree**: 查看详细结果
2. **Summary Report**: 查看汇总报告
3. **Aggregate Report**: 查看聚合报告 (重点关注)
4. **Response Time Graph**: 响应时间图表
5. **Transactions per Second**: TPS 图表

#### 步骤 4: 添加 HTTP Header Manager

1. 右键点击 `HTTP Request` → `Add` → `Config Element` → `HTTP Header Manager`
2. 添加 Header:
   - `Content-Type`: `application/json`

## 🚀 执行压测

### 准备工作

1. **确保数据库中有测试数据**
```sql
-- 插入一些测试短链接
INSERT INTO short_link (short_code, original_url, created_at, expire_at, access_count, is_active) 
VALUES 
('test001', 'https://example.com/1', NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR), 0, 1),
('test002', 'https://example.com/2', NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR), 0, 1),
('test003', 'https://example.com/3', NOW(), DATE_ADD(NOW(), INTERVAL 24 HOUR), 0, 1);
```

2. **启动应用**
```powershell
cd c:\Users\lin\Desktop\java-test2\short-link-service
mvn spring-boot:run
```

### 场景一: 无 Redis 缓存压测

1. **修改配置文件 - 禁用 Redis**
```properties
# src/main/resources/application.properties
# 注释掉 Redis 配置
# spring.data.redis.host=localhost
# spring.data.redis.port=6379
```

2. **重启应用**
```powershell
mvn spring-boot:run
```

3. **执行 JMeter 测试**

GUI 模式 (调试用):
```powershell
jmeter -t performance-test/ShortLink-LoadTest.jmx
```

命令行模式 (正式压测):
```powershell
jmeter -n -t performance-test/ShortLink-LoadTest.jmx -l results/no-redis-results.jtl -e -o results/no-redis-report
```

4. **记录关键指标**
- **TPS (Throughput)**: 每秒处理的事务数
- **Average Response Time**: 平均响应时间
- **90% Line**: 90% 的请求响应时间
- **Error Rate**: 错误率

### 场景二: 开启 Redis 缓存压测

1. **修改配置文件 - 启用 Redis**
```properties
# src/main/resources/application.properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000
```

2. **启动 Redis**
```powershell
# 如果使用 Docker
docker run -d -p 6379:6379 --name redis redis:latest

# 或使用本地 Redis
redis-server
```

3. **重启应用并预热缓存**
```powershell
mvn spring-boot:run
```

访问几次测试链接进行预热:
```powershell
curl http://localhost:8080/test001
curl http://localhost:8080/test002
curl http://localhost:8080/test003
```

4. **执行 JMeter 测试**
```powershell
jmeter -n -t performance-test/ShortLink-LoadTest.jmx -l results/with-redis-results.jtl -e -o results/with-redis-report
```

5. **记录关键指标** (同上)

## 📈 结果分析

### 预期性能对比

| 指标 | 无 Redis | 有 Redis | 提升幅度 |
|------|----------|----------|----------|
| **TPS** | ~200-300 | ~2000-3000 | **10倍** |
| **平均响应时间** | ~200ms | ~20ms | **90%↓** |
| **90% 响应时间** | ~300ms | ~30ms | **90%↓** |
| **错误率** | <1% | <0.1% | 更稳定 |

### 关键报告说明

1. **Summary Report** (`results/*-report/index.html`)
   - 打开生成的 HTML 报告
   - 重点关注: Throughput (TPS)、Average、90% Line

2. **Aggregate Report**
   - Label: 请求名称
   - Samples: 总请求数
   - Average: 平均响应时间 (ms)
   - Min/Max: 最小/最大响应时间
   - Throughput: TPS (请求/秒)
   - Error %: 错误率

3. **响应时间分布图**
   - 查看响应时间的分布情况
   - 识别性能瓶颈点

## 📸 截图保存指南

为简历和面试准备以下截图:

### 必备截图

1. **JMeter 测试配置截图**
   - 线程组配置 (显示 100 并发)
   - HTTP 请求配置

2. **无 Redis 压测结果**
   - Summary Report 截图
   - TPS 图表
   - 响应时间图表

3. **有 Redis 压测结果**
   - Summary Report 截图
   - TPS 图表
   - 响应时间图表

4. **性能对比图**
   - 制作一个对比表格或图表
   - 突出显示性能提升的倍数

5. **系统监控截图** (可选)
   - CPU 使用率
   - 内存使用率
   - 数据库连接数

### 截图技巧

- 使用 Windows 自带的截图工具: `Win + Shift + S`
- 确保截图清晰,字体大小适中
- 在截图上标注关键数据
- 保存为 PNG 格式以保证质量

## 🎯 面试话术准备

### 示例话术

> "在这个短链接项目中,我进行了完整的性能压测。使用 JMeter 模拟 100 个并发用户,每个用户执行 100 次请求,总共 10000 次请求。
> 
> **优化前**(无缓存):
> - TPS 约为 250 次/秒
> - 平均响应时间 180ms
> - 90% 请求在 280ms 内完成
> 
> **优化后**(使用 Redis 缓存):
> - TPS 提升到 2500 次/秒,**提升了 10 倍**
> - 平均响应时间降至 18ms,**减少了 90%**
> - 90% 请求在 25ms 内完成
> 
> 这个缓存策略不仅提升了性能,还显著降低了数据库压力,在相同硬件条件下支撑了更高的并发量。"

## 📊 数据记录模板

```
=== 压测环境 ===
应用服务器: 本地 (4核 8GB)
数据库: MySQL 8.0
缓存: Redis 7.0
测试工具: JMeter 5.6.3

=== 测试场景 ===
并发用户数: 100
每用户请求数: 100
总请求数: 10,000
测试接口: GET /{shortCode} (短链接跳转)

=== 无 Redis 缓存 ===
TPS: ___ 次/秒
平均响应时间: ___ ms
中位数响应时间: ___ ms
90% Line: ___ ms
95% Line: ___ ms
99% Line: ___ ms
最大响应时间: ___ ms
错误率: ___ %

=== 有 Redis 缓存 ===
TPS: ___ 次/秒
平均响应时间: ___ ms
中位数响应时间: ___ ms
90% Line: ___ ms
95% Line: ___ ms
99% Line: ___ ms
最大响应时间: ___ ms
错误率: ___ %

=== 性能提升 ===
TPS 提升: ___ 倍
响应时间减少: ___ %
稳定性改善: (描述)
```

## 🔍 常见问题

### Q1: JMeter 报错 "Connection refused"
**A**: 确保应用已启动且端口正确 (8080)

### Q2: TPS 很低,只有几十
**A**: 
- 检查数据库连接池配置
- 确保没有其他程序占用资源
- 增加数据库连接池大小

### Q3: 错误率很高
**A**: 
- 检查应用日志
- 可能是数据库连接耗尽
- 检查短链接是否存在

### Q4: 无法生成 HTML 报告
**A**: 
- 确保使用 `-n` 命令行模式
- 检查输出目录是否存在
- 确保有写入权限

## 🎓 进阶优化建议

1. **分布式压测**: 使用多台机器进行压测
2. **监控集成**: 集成 Grafana + Prometheus 实时监控
3. **更多场景**: 测试创建短链接、查询统计等接口
4. **压力梯度**: 从 50、100、200 逐步增加并发
5. **持久化测试**: 长时间压测 (30分钟以上) 观察稳定性

## 📚 相关资源

- [JMeter 官方文档](https://jmeter.apache.org/usermanual/index.html)
- [JMeter 最佳实践](https://jmeter.apache.org/usermanual/best-practices.html)
- [性能测试指标解读](https://www.nginx.com/blog/testing/)

---

**提示**: 压测应在非生产环境进行,避免影响真实用户!
