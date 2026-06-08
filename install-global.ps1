#!/usr/bin/env pwsh

Write-Host "================================================" -ForegroundColor Cyan
Write-Host "  全局 Git Hooks & Claude Skills 安装" -ForegroundColor Cyan
Write-Host "================================================" -ForegroundColor Cyan
Write-Host ""

# 1. 创建全局 Git hooks 目录
Write-Host "[1/5] 配置全局 Git hooks..." -ForegroundColor Yellow

$globalHooksDir = "$env:USERPROFILE\.git-hooks"
if (-not (Test-Path $globalHooksDir)) {
    New-Item -ItemType Directory -Force -Path $globalHooksDir | Out-Null
}

# 检查是否需要复制 hooks
$needCopy = $false
if (Test-Path ".git-hooks\commit-msg.ps1") {
    Copy-Item ".git-hooks\commit-msg.ps1" "$globalHooksDir\commit-msg.ps1" -Force
    $needCopy = $true
}

if (Test-Path ".git-hooks\commit-msg") {
    Copy-Item ".git-hooks\commit-msg" "$globalHooksDir\commit-msg" -Force
    $needCopy = $true
} else {
    # 创建 wrapper
    $wrapper = @"
#!/bin/sh
# Git Hook: 禁止 AI 署名
powershell.exe -ExecutionPolicy Bypass -File "`$(dirname `$0)/commit-msg.ps1" "`$1"
"@
    $wrapper | Out-File -FilePath "$globalHooksDir\commit-msg" -Encoding ASCII -NoNewline -Force
    $needCopy = $true
}

if ($needCopy) {
    Write-Host "  ✓ 复制 hooks 到全局目录" -ForegroundColor Green
}

# 设置全局 hooks 路径
$currentHooksPath = git config --global core.hooksPath
if ($currentHooksPath -ne "$env:USERPROFILE/.git-hooks") {
    git config --global core.hooksPath "$env:USERPROFILE/.git-hooks"
    Write-Host "  ✓ 设置全局 hooks 路径" -ForegroundColor Green
} else {
    Write-Host "  ✓ 全局 hooks 路径已存在" -ForegroundColor Green
}

Write-Host ""

# 2. 创建 Claude skills 目录
Write-Host "[2/5] 配置 Claude Code skills..." -ForegroundColor Yellow

$skillsDir = "$env:USERPROFILE\.claude\skills"
if (-not (Test-Path $skillsDir)) {
    New-Item -ItemType Directory -Force -Path $skillsDir | Out-Null
    Write-Host "  ✓ 创建 skills 目录" -ForegroundColor Green
} else {
    Write-Host "  ✓ Skills 目录已存在" -ForegroundColor Green
}

Write-Host ""

# 3. 安装 gitmoji-commit skill
Write-Host "[3/5] 安装 gitmoji-commit skill..." -ForegroundColor Yellow

$gitmojiUrl = "https://raw.githubusercontent.com/final00000000/gitmoji-commit/main/SKILL.md"
try {
    $gitmojiContent = Invoke-WebRequest -Uri $gitmojiUrl -UseBasicParsing -TimeoutSec 30 | Select-Object -ExpandProperty Content
    $gitmojiContent | Out-File -FilePath "$skillsDir\gitmoji-commit.md" -Encoding UTF8
    Write-Host "  ✓ gitmoji-commit 安装成功" -ForegroundColor Green
} catch {
    Write-Host "  ✗ gitmoji-commit 下载失败: $_" -ForegroundColor Red
    Write-Host "  → 使用本地备份版本" -ForegroundColor Yellow
}

Write-Host ""

# 4. 安装 grill-me skill
Write-Host "[4/5] 安装 grill-me skill..." -ForegroundColor Yellow

$grillUrl = "https://raw.githubusercontent.com/mattpocock/skills/main/skills/productivity/grill-me/SKILL.md"
try {
    $grillContent = Invoke-WebRequest -Uri $grillUrl -UseBasicParsing -TimeoutSec 30 | Select-Object -ExpandProperty Content
    $grillContent | Out-File -FilePath "$skillsDir\grill-me.md" -Encoding UTF8
    Write-Host "  ✓ grill-me 安装成功" -ForegroundColor Green
} catch {
    Write-Host "  ✗ grill-me 下载失败: $_" -ForegroundColor Red
    Write-Host "  → 使用本地备份版本" -ForegroundColor Yellow
}

Write-Host ""

# 5. 验证安装
Write-Host "[5/5] 验证安装结果..." -ForegroundColor Yellow

$hookExists = Test-Path "$globalHooksDir\commit-msg.ps1"
$gitmojiExists = Test-Path "$skillsDir\gitmoji-commit.md"
$grillExists = Test-Path "$skillsDir\grill-me.md"
$gitConfigOk = (git config --global core.hooksPath) -eq "$env:USERPROFILE/.git-hooks"

if ($hookExists) {
    Write-Host "  ✓ Git hooks: $globalHooksDir" -ForegroundColor Green
} else {
    Write-Host "  ✗ Git hooks 未找到" -ForegroundColor Red
}

if ($gitConfigOk) {
    Write-Host "  ✓ Git config: core.hooksPath 已配置" -ForegroundColor Green
} else {
    Write-Host "  ✗ Git config 未正确配置" -ForegroundColor Red
}

if ($gitmojiExists) {
    Write-Host "  ✓ Skill: gitmoji-commit" -ForegroundColor Green
} else {
    Write-Host "  ✗ Skill: gitmoji-commit 未找到" -ForegroundColor Red
}

if ($grillExists) {
    Write-Host "  ✓ Skill: grill-me" -ForegroundColor Green
} else {
    Write-Host "  ✗ Skill: grill-me 未找到" -ForegroundColor Red
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host "  安装完成！" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""

Write-Host "已安装组件:" -ForegroundColor Cyan
Write-Host "  • 全局 Git hooks (禁止 AI 署名)" -ForegroundColor White
Write-Host "  • gitmoji-commit skill (带 emoji 的 git commit)" -ForegroundColor White
Write-Host "  • grill-me skill (深度讨论和规划)" -ForegroundColor White
Write-Host ""

Write-Host "使用方法:" -ForegroundColor Cyan
Write-Host "  1. Git hooks 会自动应用到所有新克隆的仓库" -ForegroundColor Gray
Write-Host "  2. 在 Claude Code 中使用 /gitmoji-commit 创建提交" -ForegroundColor Gray
Write-Host "  3. 在 Claude Code 中使用 /grill-me 开始深度讨论" -ForegroundColor Gray
Write-Host ""

Write-Host "测试命令:" -ForegroundColor Cyan
Write-Host '  cd <your-repo>' -ForegroundColor Gray
Write-Host '  git commit --allow-empty -m "test: check hooks"' -ForegroundColor Gray
Write-Host ""

Write-Host "卸载方法:" -ForegroundColor Yellow
Write-Host '  git config --global --unset core.hooksPath' -ForegroundColor Gray
Write-Host "  Remove-Item -Recurse $globalHooksDir" -ForegroundColor Gray
Write-Host ""
