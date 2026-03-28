# GitHub上传脚本 - Android随机屏保应用
Write-Host "=== Android随机屏保应用 - GitHub上传脚本 ===" -ForegroundColor Green
Write-Host "此脚本将帮助你上传项目到GitHub并使用GitHub Actions自动构建APK" -ForegroundColor Cyan

# 检查Git是否安装
Write-Host "`n1. 检查Git环境..." -ForegroundColor Yellow
try {
    git --version 2>&1 | Out-Null
    Write-Host "   ✅ Git已安装" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Git未安装" -ForegroundColor Red
    Write-Host "   请先安装Git: https://git-scm.com/downloads" -ForegroundColor Yellow
    exit 1
}

# 检查是否已初始化Git
Write-Host "`n2. 检查Git仓库状态..." -ForegroundColor Yellow
if (Test-Path ".git") {
    Write-Host "   ✅ Git仓库已初始化" -ForegroundColor Green
} else {
    Write-Host "   ℹ️  Git仓库未初始化" -ForegroundColor Yellow
    Write-Host "   是否要初始化Git仓库？(Y/N)" -ForegroundColor Cyan
    $answer = Read-Host
    if ($answer -eq "Y" -or $answer -eq "y") {
        git init
        Write-Host "   ✅ Git仓库初始化完成" -ForegroundColor Green
    } else {
        Write-Host "   跳过Git初始化" -ForegroundColor Yellow
    }
}

# 显示上传步骤
Write-Host "`n3. GitHub上传步骤：" -ForegroundColor Yellow
Write-Host "   =========================================" -ForegroundColor Gray
Write-Host "   第一步：在GitHub创建新仓库" -ForegroundColor White
Write-Host "     1. 访问 https://github.com" -ForegroundColor Cyan
Write-Host "     2. 点击右上角 '+' → 'New repository'" -ForegroundColor Cyan
Write-Host "     3. 填写仓库信息：" -ForegroundColor Cyan
Write-Host "        - 仓库名: random-screensaver" -ForegroundColor Gray
Write-Host "        - 描述: Android随机屏保应用" -ForegroundColor Gray
Write-Host "        - 选择 Public (公开)" -ForegroundColor Gray
Write-Host "        - 不勾选 README (.gitignore等)" -ForegroundColor Gray
Write-Host ""
Write-Host "   第二步：将本地代码推送到GitHub" -ForegroundColor White
Write-Host "     4. 复制GitHub提供的命令：" -ForegroundColor Cyan
Write-Host "        git remote add origin https://github.com/YOUR_USERNAME/random-screensaver.git" -ForegroundColor Gray
Write-Host "        git branch -M main" -ForegroundColor Gray
Write-Host "        git push -u origin main" -ForegroundColor Gray
Write-Host ""
Write-Host "   第三步：启用GitHub Actions" -ForegroundColor White
Write-Host "     5. 推送后自动启用Actions" -ForegroundColor Cyan
Write-Host "     6. 在仓库的'Actions'标签页查看构建状态" -ForegroundColor Cyan
Write-Host ""
Write-Host "   第四步：下载APK" -ForegroundColor White
Write-Host "     7. 构建完成后，在Artifacts中下载app-debug.apk" -ForegroundColor Cyan
Write-Host "   =========================================" -ForegroundColor Gray

# 准备提交的文件
Write-Host "`n4. 准备要提交的文件..." -ForegroundColor Yellow
$filesToAdd = @(
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/com/example/randomscreensaver/",
    "app/src/main/res/",
    "app/proguard-rules.pro",
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradlew.bat",
    ".gitignore",
    ".github/workflows/android-build.yml",
    "README.md",
    "GITHUB_SETUP.md",
    "BUILD_INSTRUCTIONS.md",
    "check_project.ps1"
)

# 显示文件统计
Write-Host "   项目文件统计：" -ForegroundColor Cyan
$kotlinFiles = Get-ChildItem -Path "app/src/main/java" -Filter "*.kt" -Recurse -ErrorAction SilentlyContinue
$xmlFiles = Get-ChildItem -Path "app/src/main/res" -Filter "*.xml" -Recurse -ErrorAction SilentlyContinue
Write-Host "     Kotlin文件: $($kotlinFiles.Count)个" -ForegroundColor Gray
Write-Host "     资源文件: $($xmlFiles.Count)个" -ForegroundColor Gray
Write-Host "     配置文件: 8个" -ForegroundColor Gray

# 生成提交命令
Write-Host "`n5. 执行Git命令：" -ForegroundColor Yellow
Write-Host "   复制并执行以下命令：" -ForegroundColor Cyan
Write-Host ""
Write-Host "   # 添加所有文件" -ForegroundColor Gray
Write-Host "   git add ." -ForegroundColor Gray
Write-Host ""
Write-Host "   # 提交更改" -ForegroundColor Gray
Write-Host "   git commit -m '初始提交: Android随机屏保应用'" -ForegroundColor Gray
Write-Host ""
Write-Host "   # 推送前请先完成第一步在GitHub创建仓库" -ForegroundColor Yellow
Write-Host "   git remote add origin https://github.com/YOUR_USERNAME/random-screensaver.git" -ForegroundColor Gray
Write-Host "   git branch -M main" -ForegroundColor Gray
Write-Host "   git push -u origin main" -ForegroundColor Gray

# GitHub Actions说明
Write-Host "`n6. GitHub Actions自动构建说明：" -ForegroundColor Yellow
Write-Host "   📍 工作流程: .github/workflows/android-build.yml" -ForegroundColor Cyan
Write-Host "   ⏱️  构建时间: 约5-10分钟（首次较长）" -ForegroundColor Gray
Write-Host "   📱 输出产物: app-debug.apk" -ForegroundColor Gray
Write-Host "   🔄 触发方式: 推送代码或手动触发" -ForegroundColor Gray

# 后续步骤
Write-Host "`n7. 后续步骤：" -ForegroundColor Yellow
Write-Host "   1. 构建完成后，在GitHub仓库的'Actions'标签页" -ForegroundColor White
Write-Host "   2. 点击最新的构建记录" -ForegroundColor White
Write-Host "   3. 在'Artifacts'部分下载'random-screensaver-apk'" -ForegroundColor White
Write-Host "   4. 将APK安装到Android设备测试" -ForegroundColor White

# 帮助信息
Write-Host "`n📞 需要帮助？" -ForegroundColor Green
Write-Host "   - 查看 GITHUB_SETUP.md 获取详细步骤" -ForegroundColor Cyan
Write-Host "   - 查看 BUILD_INSTRUCTIONS.md 获取构建说明" -ForegroundColor Cyan
Write-Host "   - 运行 check_project.ps1 检查项目完整性" -ForegroundColor Cyan

Write-Host "`n✅ 脚本执行完成！现在可以按照上述步骤上传到GitHub。" -ForegroundColor Green
Write-Host "   祝你好运！🎉" -ForegroundColor Magenta

# 等待用户确认
Write-Host "`n按Enter键退出..." -ForegroundColor Gray
Read-Host