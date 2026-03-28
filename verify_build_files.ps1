Write-Host "=== 验证构建文件 ===" -ForegroundColor Green

# 1. 检查gradlew文件
Write-Host "`n1. 检查gradlew文件:" -ForegroundColor Cyan
if (Test-Path "gradlew") {
    $size = (Get-Item "gradlew").Length
    $firstLine = Get-Content "gradlew" -First 1
    Write-Host "   ✓ gradlew存在 ($size 字节)" -ForegroundColor Green
    Write-Host "   第一行: $firstLine" -ForegroundColor Gray
} else {
    Write-Host "   ✗ gradlew不存在" -ForegroundColor Red
}

# 2. 检查gradlew.bat文件
Write-Host "`n2. 检查gradlew.bat文件:" -ForegroundColor Cyan
if (Test-Path "gradlew.bat") {
    Write-Host "   ✓ gradlew.bat存在" -ForegroundColor Green
} else {
    Write-Host "   ✗ gradlew.bat不存在" -ForegroundColor Red
}

# 3. 检查gradle-wrapper文件
Write-Host "`n3. 检查gradle-wrapper文件:" -ForegroundColor Cyan
if (Test-Path "gradle/wrapper/gradle-wrapper.properties") {
    $props = Get-Content "gradle/wrapper/gradle-wrapper.properties"
    Write-Host "   ✓ gradle-wrapper.properties存在" -ForegroundColor Green
    foreach ($line in $props) {
        if ($line -match "distributionUrl") {
            Write-Host "   Gradle版本: $line" -ForegroundColor Gray
        }
    }
} else {
    Write-Host "   ✗ gradle-wrapper.properties不存在" -ForegroundColor Red
}

# 4. 检查GitHub Actions配置
Write-Host "`n4. 检查GitHub Actions配置:" -ForegroundColor Cyan
if (Test-Path ".github/workflows/android-build.yml") {
    Write-Host "   ✓ android-build.yml存在" -ForegroundColor Green
} else {
    Write-Host "   ✗ android-build.yml不存在" -ForegroundColor Red
}

# 5. 检查关键Android文件
Write-Host "`n5. 检查关键Android文件:" -ForegroundColor Cyan
$androidFiles = @(
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/example/randomscreensaver/MainActivity.kt"
)

foreach ($file in $androidFiles) {
    if (Test-Path $file) {
        Write-Host "   ✓ $file 存在" -ForegroundColor Green
    } else {
        Write-Host "   ✗ $file 不存在" -ForegroundColor Red
    }
}

Write-Host "`n=== 验证完成 ===" -ForegroundColor Green
Write-Host "`n下一步："
Write-Host "1. 运行: git add gradlew .github/workflows/android-build.yml" -ForegroundColor Yellow
Write-Host "2. 运行: git commit -m '修复gradlew语法错误，使用标准sh语法'" -ForegroundColor Yellow
Write-Host "3. 运行: git push origin main" -ForegroundColor Yellow