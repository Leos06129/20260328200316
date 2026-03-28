# 项目完整性检查脚本
Write-Host "=== Android随机屏保应用项目检查 ===" -ForegroundColor Green

# 检查关键文件是否存在
$requiredFiles = @(
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/example/randomscreensaver/MainActivity.kt",
    "app/src/main/res/layout/activity_main.xml",
    "gradlew.bat"
)

Write-Host "`n检查关键文件：" -ForegroundColor Cyan
foreach ($file in $requiredFiles) {
    if (Test-Path $file) {
        Write-Host "✅ $file" -ForegroundColor Green
    } else {
        Write-Host "❌ $file (缺失)" -ForegroundColor Red
    }
}

# 检查目录结构
Write-Host "`n检查目录结构：" -ForegroundColor Cyan
$directories = Get-ChildItem -Directory -Recurse | Select-Object -First 20
foreach ($dir in $directories) {
    Write-Host "📁 $($dir.FullName.Replace($pwd.Path, '').TrimStart('\'))"
}

# Java环境检查
Write-Host "`n检查Java环境：" -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Java已安装" -ForegroundColor Green
        Write-Host "   $($javaVersion[0])"
    }
} catch {
    Write-Host "❌ Java未安装或未配置环境变量" -ForegroundColor Red
}

# Gradle检查
Write-Host "`n检查Gradle环境：" -ForegroundColor Cyan
if (Test-Path "gradlew.bat") {
    Write-Host "✅ Gradle包装器可用" -ForegroundColor Green
} else {
    Write-Host "❌ Gradle包装器缺失" -ForegroundColor Red
}

# 项目统计
Write-Host "`n项目统计：" -ForegroundColor Cyan
$kotlinFiles = Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse
$xmlFiles = Get-ChildItem -Path "app/src/main/res" -Filter "*.xml" -Recurse
$layoutFiles = Get-ChildItem -Path "app/src/main/res/layout" -Filter "*.xml"

Write-Host "Kotlin文件: $($kotlinFiles.Count)个"
Write-Host "资源文件: $($xmlFiles.Count)个"
Write-Host "布局文件: $($layoutFiles.Count)个"

# 构建建议
Write-Host "`n=== 构建建议 ===" -ForegroundColor Yellow
Write-Host "`n如果Java已安装，可以运行以下命令：" -ForegroundColor White
Write-Host "1. 设置Java环境变量：" -ForegroundColor Cyan
Write-Host "   SET JAVA_HOME=C:\Program Files\Java\jdk-xx.x.x" -ForegroundColor Gray
Write-Host "   SET PATH=%JAVA_HOME%\bin;%PATH%" -ForegroundColor Gray
Write-Host "`n2. 构建项目：" -ForegroundColor Cyan
Write-Host "   .\gradlew.bat assembleDebug" -ForegroundColor Gray
Write-Host "`n3. APK位置：" -ForegroundColor Cyan
Write-Host "   app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor Gray

Write-Host "`n=== 替代方案 ===" -ForegroundColor Magenta
Write-Host "1. 使用Android Studio（推荐）" -ForegroundColor White
Write-Host "2. 使用在线构建服务" -ForegroundColor White
Write-Host "3. 使用Docker容器" -ForegroundColor White

Write-Host "`n详细说明请查看 BUILD_INSTRUCTIONS.md" -ForegroundColor Green
Write-Host "`n项目检查完成！" -ForegroundColor Green