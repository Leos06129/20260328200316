# gradlew文件检查脚本
Write-Host "=== gradlew文件检查 ===" -ForegroundColor Green

$gradlewPath = "gradlew"

if (-not (Test-Path $gradlewPath)) {
    Write-Host "❌ gradlew文件不存在" -ForegroundColor Red
    exit 1
}

# 检查文件大小
$fileInfo = Get-Item $gradlewPath
Write-Host "📊 文件信息：" -ForegroundColor Yellow
Write-Host "  大小: $($fileInfo.Length) 字节" -ForegroundColor Gray
Write-Host "  路径: $($fileInfo.FullName)" -ForegroundColor Gray

# 检查第一行（shebang）
Write-Host "🔍 检查shebang行：" -ForegroundColor Yellow
$firstLine = Get-Content $gradlewPath -TotalCount 1
Write-Host "  第一行: $firstLine" -ForegroundColor Gray

if ($firstLine -ne "#!/usr/bin/env sh") {
    Write-Host "  ⚠️  shebang行可能有问题" -ForegroundColor Yellow
}

# 检查换行符
Write-Host "🔍 检查换行符：" -ForegroundColor Yellow
$content = Get-Content -Raw $gradlewPath
$hasWindowsLineEndings = $content -match "`r`n"
$hasUnixLineEndings = $content -match "[^`r]`n"

if ($hasWindowsLineEndings) {
    Write-Host "  ❌ 检测到Windows换行符(\r\n)" -ForegroundColor Red
    Write-Host "    需要转换为Unix换行符(\n)" -ForegroundColor Yellow
} elseif ($hasUnixLineEndings) {
    Write-Host "  ✅ 使用Unix换行符(\n)" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  无法确定换行符类型" -ForegroundColor Yellow
}

# 检查文件权限（模拟Linux）
Write-Host "🔍 模拟Linux权限检查：" -ForegroundColor Yellow
$fileBytes = [System.IO.File]::ReadAllBytes($gradlewPath)
$isBinary = $false
foreach ($byte in $fileBytes) {
    if ($byte -eq 0) {
        $isBinary = $true
        break
    }
}

if ($isBinary) {
    Write-Host "  ⚠️  文件可能包含二进制数据" -ForegroundColor Yellow
} else {
    Write-Host "  ✅ 文件是纯文本" -ForegroundColor Green
}

# 检查文件内容完整性
Write-Host "🔍 检查关键内容：" -ForegroundColor Yellow
$content = Get-Content -Raw $gradlewPath
$checks = @{
    "#!/usr/bin/env sh" = "shebang行"
    "APP_HOME" = "APP_HOME变量"
    "CLASSPATH=" = "CLASSPATH设置"
    "JAVACMD=" = "Java命令检测"
    "exec \"`$JAVACMD\"" = "执行命令"
}

foreach ($pattern in $checks.Keys) {
    if ($content -match [regex]::Escape($pattern)) {
        Write-Host "  ✅ $($checks[$pattern])存在" -ForegroundColor Green
    } else {
        Write-Host "  ❌ $($checks[$pattern])缺失" -ForegroundColor Red
    }
}

# 修复建议
Write-Host "`n🔧 修复建议：" -ForegroundColor Magenta
if ($hasWindowsLineEndings) {
    Write-Host "1. 转换换行符：" -ForegroundColor White
    Write-Host "   (Get-Content -Raw gradlew) -replace \"`r`n\", \"`n\" | Set-Content -NoNewline gradlew" -ForegroundColor Gray
}

Write-Host "`n2. 验证GitHub Actions配置：" -ForegroundColor White
Write-Host "   .github/workflows/android-build.yml 应包含：" -ForegroundColor Gray
Write-Host "   sed -i 's/\r$//' ./gradlew" -ForegroundColor Gray

Write-Host "`n3. 测试gradlew：" -ForegroundColor White
Write-Host "   # 在Linux或WSL中测试" -ForegroundColor Gray
Write-Host "   ./gradlew --version" -ForegroundColor Gray

Write-Host "`n✅ 检查完成！" -ForegroundColor Green
Write-Host "现在可以重新提交和推送代码到GitHub。" -ForegroundColor Cyan