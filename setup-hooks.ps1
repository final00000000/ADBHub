#!/usr/bin/env pwsh

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  Git Hooks 自动安装 - 禁止 AI 署名" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 检查是否是 Git 仓库
if (-not (Test-Path ".git")) {
    Write-Host "初始化 Git 仓库..." -ForegroundColor Yellow
    git init
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Git 初始化失败" -ForegroundColor Red
        exit 1
    }
    Write-Host "✓ Git 仓库初始化成功" -ForegroundColor Green
    Write-Host ""
}

# 创建 hooks 目录
$hooksDir = ".git\hooks"
if (-not (Test-Path $hooksDir)) {
    New-Item -ItemType Directory -Force -Path $hooksDir | Out-Null
    Write-Host "✓ 创建 hooks 目录" -ForegroundColor Green
}

# 检查源文件是否存在
$sourceHook = ".git-hooks\commit-msg.ps1"
if (-not (Test-Path $sourceHook)) {
    Write-Host "❌ 找不到源文件: $sourceHook" -ForegroundColor Red
    exit 1
}

# 复制 PowerShell hook
Copy-Item $sourceHook "$hooksDir\commit-msg.ps1" -Force
Write-Host "✓ 复制 PowerShell hook 脚本" -ForegroundColor Green

# 创建 Git Bash wrapper
$wrapper = @"
#!/bin/sh
# Git Hook: 禁止 AI 署名
powershell.exe -ExecutionPolicy Bypass -File "`$(dirname `$0)/commit-msg.ps1" "`$1"
"@

$wrapper | Out-File -FilePath "$hooksDir\commit-msg" -Encoding ASCII -NoNewline -Force
Write-Host "✓ 创建 Git Bash wrapper" -ForegroundColor Green

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  安装成功！" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "Hook 功能:" -ForegroundColor Cyan
Write-Host "  • 自动检测提交信息中的 AI 署名" -ForegroundColor White
Write-Host "  • 阻止包含 AI 署名的提交" -ForegroundColor White
Write-Host "  • 自动清理 AI 相关标记" -ForegroundColor White
Write-Host ""
Write-Host "测试命令:" -ForegroundColor Cyan
Write-Host '  git commit --allow-empty -m "test: normal commit"' -ForegroundColor Gray
Write-Host ""
Write-Host "临时禁用 (不推荐):" -ForegroundColor Yellow
Write-Host "  git commit --no-verify -m 'message'" -ForegroundColor Gray
Write-Host ""
