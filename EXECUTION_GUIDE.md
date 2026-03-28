# Android随机屏保应用 - 执行指南

## 🎯 目标
通过GitHub Actions自动构建Android APK文件

## 📋 快速开始步骤

### 阶段1：准备工作
1. **安装Git** - [下载地址](https://git-scm.com/downloads)
2. **注册GitHub账号** - [注册地址](https://github.com/signup)
3. **准备代码** - 确保项目文件完整

### 阶段2：创建GitHub仓库
1. **登录GitHub**
2. **创建新仓库**：
   - 点击右上角 "+" → "New repository"
   - 名称: `random-screensaver`
   - 描述: "Android随机屏保应用"
   - 选择: Public (公开)
   - 不勾选: README, .gitignore, license
3. **记录仓库URL**：`https://github.com/YOUR_USERNAME/random-screensaver.git`

### 阶段3：上传项目到GitHub
在项目根目录（c:/Users/Administrator/CodeBuddy/20260328200316）执行：

```powershell
# 方法1：使用脚本（推荐）
.\upload_to_github.ps1

# 方法2：手动执行命令
git init
git add .
git commit -m "初始提交: Android随机屏保应用"
git remote add origin https://github.com/YOUR_USERNAME/random-screensaver.git
git branch -M main
git push -u origin main
```

### 阶段4：启用GitHub Actions
1. **自动启用**：首次推送后会自动启用Actions
2. **查看状态**：在仓库页面点击"Actions"标签
3. **等待构建**：首次构建约需5-10分钟

### 阶段5：下载APK文件
构建完成后：
1. 进入"Actions"标签页
2. 点击最新的构建记录
3. 在"Artifacts"部分下载`random-screensaver-apk`
4. 解压得到`app-debug.apk`

## ⚙️ GitHub Actions工作流程

### 配置位置
`.github/workflows/android-build.yml`

### 构建过程
1. **环境准备**：Ubuntu + JDK 11 + Android SDK
2. **代码检出**：获取最新代码
3. **权限设置**：赋予gradlew执行权限
4. **构建执行**：运行`./gradlew assembleDebug`
5. **产物上传**：APK文件保存为Artifact

### 触发方式
- ✅ 推送代码到main分支
- ✅ 创建Pull Request
- ✅ 手动触发（Workflow Dispatch）

## 📱 APK安装和使用

### 安装步骤
1. 下载APK文件到Android设备
2. 允许安装未知来源应用
3. 点击APK文件开始安装
4. 安装完成后打开应用

### 首次使用设置
1. **授予权限**：
   - 显示在其他应用上层（必需）
   - 电池优化白名单（建议）
2. **配置设置**：
   - 输入显示语句
   - 设置时间间隔（5-60秒）
   - 启用开机自启动（可选）
3. **启动服务**：
   - 返回主界面
   - 点击"启动屏保"
   - 应用会在通知栏显示状态

### 功能验证
1. **常规模式**：应用在前台/后台时随机显示
2. **锁屏模式**：锁屏后观察是否继续显示
3. **随机效果**：每次显示颜色、大小、位置不同

## 🔧 故障排除

### 构建失败
1. **检查日志**：在Actions页面查看详细错误信息
2. **常见问题**：
   - Android SDK缺失：更新工作流程配置
   - 权限问题：检查gradlew权限
   - 依赖下载失败：网络问题，重试构建

### 应用无法安装
1. **未知来源**：确保允许安装未知来源应用
2. **设备兼容性**：检查Android版本要求（API 21+）
3. **签名问题**：Debug版本可直接安装

### 应用无法显示
1. **悬浮窗权限**：必须授予"显示在其他应用上层"
2. **后台限制**：检查电池优化设置
3. **通知权限**：确保通知权限已开启

## 📈 监控和维护

### 构建监控
- **成功/失败率**：定期检查构建状态
- **构建时长**：优化构建脚本减少时间
- **资源使用**：监控GitHub Actions使用量

### 版本管理
1. **语义化版本**：使用v1.0.0格式
2. **发布说明**：每次更新添加详细说明
3. **回滚机制**：保留历史版本便于回滚

### 代码维护
1. **定期更新**：更新依赖库和SDK版本
2. **安全检查**：检查安全漏洞和权限
3. **性能优化**：优化APK大小和运行效率

## 💡 最佳实践

### 开发实践
1. **版本控制**：每个功能一个分支
2. **代码审查**：通过Pull Request进行审查
3. **持续集成**：每次提交自动构建

### 发布实践
1. **测试充分**：正式发布前充分测试
2. **用户反馈**：收集用户反馈持续改进
3. **文档更新**：代码变更同步更新文档

### 安全实践
1. **权限最小化**：只申请必要权限
2. **数据保护**：不收集不必要用户数据
3. **定期审计**：定期审查安全设置

## 📞 支持资源

### 文档资源
- `GITHUB_SETUP.md` - GitHub详细设置指南
- `BUILD_INSTRUCTIONS.md` - 构建说明文档
- `README.md` - 项目主文档

### 脚本工具
- `check_project.ps1` - 项目完整性检查
- `upload_to_github.ps1` - 一键上传脚本

### 外部资源
- [GitHub Actions文档](https://docs.github.com/actions)
- [Android开发文档](https://developer.android.com/docs)
- [Gradle用户指南](https://docs.gradle.org/current/userguide)

---

## 🎉 成功标志

完成所有步骤后，你将获得：
1. ✅ GitHub仓库包含完整代码
2. ✅ GitHub Actions自动构建APK
3. ✅ 可下载的APK文件
4. ✅ 可安装测试的Android应用

**开始行动吧！按照步骤执行，你将在30分钟内获得可用的APK文件。**