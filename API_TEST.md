# API 测试示例

## 使用 PowerShell 测试

### 1. 健康检查
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get
```

### 2. 生成短链接
```powershell
$body = @{
    originalUrl = "https://www.example.com/very/long/url/with/many/parameters"
    expireDays = 30
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/api/short-link" `
    -Method Post `
    -Body $body `
    -ContentType "application/json"
```

### 3. 访问短链接
```powershell
# 假设生成的短码是 2Bi
Start-Process "http://localhost:8080/2Bi"
```

### 4. 查询统计
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/stats/2Bi" -Method Get
```

---

## 使用 curl 测试

### 1. 健康检查
```bash
curl http://localhost:8080/health
```

### 2. 生成短链接
```bash
curl -X POST http://localhost:8080/api/short-link \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.example.com/very/long/url",
    "expireDays": 30
  }'
```

### 3. 访问短链接
```bash
curl -L http://localhost:8080/2Bi
```

### 4. 查询统计
```bash
curl http://localhost:8080/api/stats/2Bi
```

---

## 压力测试

### 使用 Apache Bench
```bash
# 测试健康检查接口
ab -n 10000 -c 100 http://localhost:8080/health

# 测试短链接跳转 (需要先创建短链接)
ab -n 5000 -c 50 http://localhost:8080/2Bi
```

### 使用 JMeter

1. 创建线程组: 100 线程
2. 添加 HTTP 请求: 
   - 服务器: localhost
   - 端口: 8080
   - 路径: /2Bi
3. 运行测试,查看响应时间

---

## 预期结果

### 生成短链接响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortUrl": "http://localhost:8080/2Bi",
    "shortCode": "2Bi",
    "originalUrl": "https://www.example.com/very/long/url"
  }
}
```

### 统计查询响应示例
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortCode": "2Bi",
    "originalUrl": "https://www.example.com/very/long/url",
    "pv": 156,
    "uv": 42
  }
}
```
