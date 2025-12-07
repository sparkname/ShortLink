# 压测快速上手指南

> **目标**: 20 分钟完成性能压测,获得可量化的 Redis 优化数据

---

## 📋 准备工作 (5 分钟)

### 1. 安装 JMeter

**Windows 系统:**

```powershell
# 下载: https://jmeter.apache.org/download_jmeter.cgi
# 解压到任意目录,例如 D:\apache-jmeter-5.6.3

# 添加到 PATH (临时)
$env:PATH += ";D:\apache-jmeter-5.6.3\bin"

# 验证
jmeter --version
```

**macOS 系统:**

```bash
brew install jmeter
jmeter --version
```

### 2. 准备测试数据

启动应用后,创建测试短链接 (已有 `shortcodes.csv` 则跳过):

```powershell
# 快速创建 10 个测试短链接
$codes = @("test001", "test002", "test003", "test004", "test005",
           "test006", "test007", "test008", "test009", "test010")

foreach ($code in $codes) {
    $body = @{
        originalUrl = "https://github.com"
        expirationHours = 24
        customCode = $code
    } | ConvertTo-Json
    
    try {
        Invoke-RestMethod -Uri "http://localhost:8080/api/short-link" `
                          -Method POST -Body $body `
                          -ContentType "application/json" `
                          -ErrorAction SilentlyContinue
    } catch { }
}
Write-Host "✅ 测试数据创建完成!" -ForegroundColor Green
```

---

## 🚀 步骤一: 无 Redis 压测 (高并发场景)

### 1.1 禁用 Redis

编辑 `src/main/resources/application.properties`:

```properties
# 注释掉 Redis 配置
# spring.data.redis.host=localhost
# spring.data.redis.port=6379
```

### 1.2 重启应用

```powershell
# 停止应用 (Ctrl+C) 然后:
mvn spring-boot:run
```

### 1.3 执行高并发压测 (500 并发)

```powershell
cd c:\Users\lin\Desktop\java-test2\short-link-service

# 清理旧数据
Remove-Item performance-test\results\no-redis-* -Recurse -Force -ErrorAction SilentlyContinue

# 执行压测 (500并发 × 100次 = 50,000请求)
jmeter -n -t performance-test\ShortLink-LoadTest.jmx `
       -l performance-test\results\no-redis-results.jtl `
       -e -o performance-test\results\no-redis-report
```

### 1.4 查看结果

压测完成后,浏览器打开:
```
performance-test\results\no-redis-report\index.html
```

### 1.5 记录关键数据

在 `performance-test\RESULTS.md` 中记录:
- **TPS**: ___953.5 ____ 次/秒
- **平均响应时间**: ____4___ ms
### 1.4 查看结果

```powershell
# 自动打开报告
Start-Process performance-test\results\no-redis-report\index.html
## 🚀 步骤二: 有 Redis 压测

### 2.1 启用 Redis + 启动服务

```powershell
# 1. 启动 Redis
docker run -d -p 6379:6379 --name redis redis:latest

# 2. 验证 Redis
docker exec -it redis redis-cli ping  # 应返回 PONG

# 3. 编辑 application.properties,取消注释:
# spring.data.redis.host=localhost
# spring.data.redis.port=6379

# 4. 重启应用
mvn spring-boot:run
```

### 2.2 预热缓存

```powershell
# 访问测试链接,加载数据到缓存
for ($i=1; $i -le 10; $i++) {
    $code = "test{0:D3}" -f $i
    Invoke-WebRequest -Uri "http://localhost:8080/$code" `
                     -MaximumRedirection 0 -ErrorAction SilentlyContinue
}
Write-Host "✅ 缓存预热完成!" -ForegroundColor Green
```

### 2.3 执行压测

```powershell
# 清理旧数据
Remove-Item performance-test\results\with-redis-* -Recurse -Force -ErrorAction SilentlyContinue

# 执行压测
jmeter -n -t performance-test\ShortLink-LoadTest.jmx `
       -l performance-test\results\with-redis-results.jtl `
       -e -o performance-test\results\with-redis-report

# 打开报告
Start-Process performance-test\results\with-redis-report\index.html
```

### 2.4 记录数据

在 `RESULTS.md` 中记录关键指标

---

## 📊 步骤三: 对比分析 (5 分钟)

### 3.1 计算性能提升

在 `performance-test\RESULTS.md` 中填写:

```
TPS 提升倍数 = (有Redis的TPS) / (无Redis的TPS)
响应时间减少 = [(无Redis响应时间 - 有Redis响应时间) / 无Redis响应时间] × 100%
```

**示例:**
- 无 Redis: TPS = 250, 响应时间 = 180ms
- 有 Redis: TPS = 2500, 响应时间 = 18ms

则:
- TPS 提升: 2500 / 250 = **10 倍**
- 响应时间减少: (180 - 18) / 180 × 100% = **90%**

### 3.2 对比表格
## 📊 步骤三: 性能对比分析

在 `RESULTS.md` 中填写对比表格:

| 指标 | 无 Redis (500并发) | 有 Redis (500并发) | 提升 |
|------|-------------------|-------------------|------|
| **TPS** | _______ | _______ | ___倍 |
| **平均响应时间** | _______ms | _______ms | 减少__% |
| **90% Line** | _______ms | _______ms | 减少__% |
| **错误率** | _______% | _______% | - |

**计算公式:**
- TPS 提升倍数 = (有Redis的TPS) ÷ (无Redis的TPS)
- 响应时间减少 = [(无Redis - 有Redis) ÷ 无Redis] × 100%

---

## 🎯 预期结果 (500 并发场景)

| 指标 | 无 Redis | 有 Redis | 提升 |
|------|----------|----------|------|
| **TPS** | 300-500 | 2000-3000 | **6-10倍** |
| **响应时间** | 300-500ms | 20-40ms | **10倍+** |
| **错误率** | 可能 >1% | <0.1% | 更稳定 |

---

## 💡 面试话术模板

```
"在短链接项目中,我通过 JMeter 进行了完整的性能压测:

测试场景:
- 500 并发用户,每用户 100 次请求,总计 50,000 次
- 对比无缓存和有缓存两种场景

压测结果:
- 无Redis: TPS [你的数据],响应时间 [你的数据]ms
- 有Redis: TPS [你的数据],响应时间 [你的数据]ms
- 性能提升: TPS 提升 [X] 倍,响应时间减少 [Y]%

技术实现:
- Cache-Aside 缓存策略
- 布隆过滤器防缓存穿透
- 分布式锁防缓存击穿
- 合理的过期时间避免缓存雪崩

这个优化使系统在相同条件下支撑了 [X] 倍的流量。"
```

---

## ✅ 完成检查清单

- [ ] JMeter 已安装
- [ ] 测试数据已创建
- [ ] 无 Redis 压测完成
- [ ] 有 Redis 压测完成
- [ ] 性能数据已记录
- [ ] 截图已保存 (Win+Shift+S)

---

## 🔧 常见问题

**Q: TPS 很低,只有几十?**
- 检查数据库连接池: `spring.datasource.hikari.maximum-pool-size=50`
- 确保没有其他程序占用资源

**Q: 压测出现错误?**
- 检查应用是否运行: `netstat -ano | findstr :8080`
- 查看应用日志排查问题

**Q: 无法生成报告?**
- 清空旧数据: `Remove-Item -Recurse results\*-report`
- 确保使用 `-n` 参数

**Q: Redis 性能提升不明显?**
- 当前已配置 500 并发,应该能看出明显差异
- 确保 Redis 正常运行,缓存已预热

---

**总耗时**: 20-30 分钟 | **难度**: ⭐⭐☆☆☆ | **收获**: ⭐⭐⭐⭐⭐
