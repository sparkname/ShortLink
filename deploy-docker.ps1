# 快速部署脚本 - Docker Compose 方式

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "短链接系统 - Docker 快速部署" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 检查 Docker 是否安装
Write-Host "[1/6] 检查 Docker 环境..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version
    Write-Host "  ✓ Docker 已安装: $dockerVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Docker 未安装" -ForegroundColor Red
    Write-Host ""
    Write-Host "请先安装 Docker Desktop:" -ForegroundColor Yellow
    Write-Host "  下载地址: https://www.docker.com/products/docker-desktop" -ForegroundColor Cyan
    exit 1
}

try {
    $composeVersion = docker-compose --version
    Write-Host "  ✓ Docker Compose 已安装: $composeVersion" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Docker Compose 未安装" -ForegroundColor Red
    exit 1
}

# 检查 Docker 是否运行
Write-Host ""
Write-Host "[2/6] 检查 Docker 服务状态..." -ForegroundColor Yellow
try {
    docker ps | Out-Null
    Write-Host "  ✓ Docker 服务正在运行" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Docker 服务未启动" -ForegroundColor Red
    Write-Host "  请启动 Docker Desktop 后重试" -ForegroundColor Yellow
    exit 1
}

# 进入项目目录
$projectDir = "c:\Users\lin\Desktop\java-test2\short-link-service"
Write-Host ""
Write-Host "[3/6] 进入项目目录..." -ForegroundColor Yellow
if (Test-Path $projectDir) {
    Set-Location $projectDir
    Write-Host "  ✓ 当前目录: $projectDir" -ForegroundColor Green
} else {
    Write-Host "  ✗ 项目目录不存在: $projectDir" -ForegroundColor Red
    exit 1
}

# 停止旧容器
Write-Host ""
Write-Host "[4/6] 停止旧容器(如果存在)..." -ForegroundColor Yellow
docker-compose down 2>$null
Write-Host "  ✓ 旧容器已停止" -ForegroundColor Green

# 构建并启动服务
Write-Host ""
Write-Host "[5/6] 构建并启动所有服务..." -ForegroundColor Yellow
Write-Host "  提示: 首次运行需要下载镜像和构建应用,可能需要 5-10 分钟" -ForegroundColor Cyan
Write-Host ""

docker-compose up -d --build

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "  ✓ 所有服务启动成功!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "  ✗ 服务启动失败" -ForegroundColor Red
    Write-Host "  请查看错误信息并检查配置" -ForegroundColor Yellow
    exit 1
}

# 等待服务就绪
Write-Host ""
Write-Host "[6/6] 等待服务就绪..." -ForegroundColor Yellow
Write-Host "  正在等待应用启动 (最多等待 60 秒)..." -ForegroundColor Cyan

$maxAttempts = 12
$attempt = 0
$ready = $false

while ($attempt -lt $maxAttempts -and -not $ready) {
    Start-Sleep -Seconds 5
    $attempt++
    
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" `
                                       -TimeoutSec 2 `
                                       -UseBasicParsing `
                                       -ErrorAction SilentlyContinue
        
        if ($response.StatusCode -eq 200) {
            $ready = $true
            Write-Host "  ✓ 应用已就绪!" -ForegroundColor Green
        }
    } catch {
        Write-Host "  ⏳ 尝试 $attempt/$maxAttempts - 等待中..." -ForegroundColor Gray
    }
}

if (-not $ready) {
    Write-Host ""
    Write-Host "  ⚠ 应用启动时间较长,请稍后手动检查" -ForegroundColor Yellow
    Write-Host "  可使用命令查看日志: docker-compose logs -f app" -ForegroundColor Cyan
}

# 显示服务信息
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "部署完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "📊 服务状态:" -ForegroundColor Yellow
docker-compose ps

Write-Host ""
Write-Host "🌐 访问地址:" -ForegroundColor Yellow
Write-Host "  应用首页:          http://localhost:8080" -ForegroundColor Cyan
Write-Host "  健康检查:          http://localhost:8080/actuator/health" -ForegroundColor Cyan
Write-Host "  RabbitMQ 管理界面: http://localhost:15672" -ForegroundColor Cyan
Write-Host "                      账号: admin / admin123" -ForegroundColor Gray

Write-Host ""
Write-Host "🧪 快速测试:" -ForegroundColor Yellow
Write-Host "  创建短链接:" -ForegroundColor White
Write-Host '  $body = @{ originalUrl = "https://www.github.com"; expirationHours = 24 } | ConvertTo-Json' -ForegroundColor Gray
Write-Host '  Invoke-RestMethod -Uri "http://localhost:8080/api/short-link" -Method POST -Body $body -ContentType "application/json"' -ForegroundColor Gray

Write-Host ""
Write-Host "📝 常用命令:" -ForegroundColor Yellow
Write-Host "  查看日志:   docker-compose logs -f app" -ForegroundColor Gray
Write-Host "  停止服务:   docker-compose stop" -ForegroundColor Gray
Write-Host "  启动服务:   docker-compose start" -ForegroundColor Gray
Write-Host "  重启服务:   docker-compose restart" -ForegroundColor Gray
Write-Host "  删除服务:   docker-compose down" -ForegroundColor Gray

Write-Host ""
Write-Host "📚 详细文档:" -ForegroundColor Yellow
Write-Host "  部署指南: DEPLOYMENT.md" -ForegroundColor Cyan
Write-Host "  快速开始: QUICKSTART.md" -ForegroundColor Cyan
Write-Host "  API 文档: API_TEST.md" -ForegroundColor Cyan

Write-Host ""
