# GitHub上传和自动构建指南

## 📋 前置要求

1. **GitHub账号** - [注册地址](https://github.com/signup)
2. **Git客户端** - [下载地址](https://git-scm.com/downloads)

## 🚀 上传到GitHub步骤

### 步骤1：初始化Git仓库
```bash
# 在项目根目录执行
git init
git add .
git commit -m "初始提交: Android随机屏保应用"
```

### 步骤2：创建GitHub仓库
1. 登录GitHub
2. 点击右上角 "+" → "New repository"
3. 填写仓库信息：
   - Repository name: `random-screensaver`
   - Description: "Android随机屏保应用 - 锁屏和常规状态下随机显示文字"
   - Public（公开仓库）
   - 不勾选"Add a README file"（我们已经有）
4. 点击"Create repository"

### 步骤3：关联远程仓库并推送
```bash
# 复制GitHub提供的命令（类似以下）
git remote add origin https://github.com/YOUR_USERNAME/random-screensaver.git
git branch -M main
git push -u origin main
```

## ⚙️ GitHub Actions自动构建

### 工作流程说明
项目已配置`.github/workflows/android-build.yml`：
- **触发条件**：推送到main分支或手动触发
- **构建环境**：Ubuntu最新版 + JDK 11 + Android SDK
- **构建任务**：执行`./gradlew assembleDebug`
- **输出产物**：APK文件保存在Artifacts中

### 使用方法
1. **启用Actions**：首次推送后会自动启用
2. **查看构建状态**：在仓库的"Actions"标签页
3. **下载APK**：构建完成后点击"Artifacts"下载

### 手动触发构建
在GitHub仓库页面：
1. 点击"Actions"标签
2. 选择"Android Build"工作流
3. 点击"Run workflow"
4. 选择分支并运行

## 📦 发布APK到Releases

### 创建第一个发布版本
1. 点击"Releases" → "Create a new release"
2. 填写版本信息：
   - Tag: `v1.0.0`
   - Title: "v1.0.0 - 初始版本"
   - Description: 更新日志
3. 上传APK文件：
   - 从Actions Artifacts下载`app-debug.apk`
   - 拖拽到发布页面
4. 点击"Publish release"

## 🔧 常见问题

### Q1：构建失败怎么办？
1. 检查Android SDK路径是否正确
2. 确认Gradle版本兼容性
3. 查看构建日志中的具体错误

### Q2：如何更新代码？
```bash
# 修改代码后
git add .
git commit -m "描述更改内容"
git push origin main
# Actions会自动构建新版本
```

### Q3：如何下载构建的APK？
1. 进入仓库的"Actions"页面
2. 点击最新的构建记录
3. 在"Artifacts"部分下载APK

### Q4：如何配置自动发布？
在`.github/workflows/android-build.yml`中添加：
```yaml
- name: Create Release
  uses: actions/create-release@v1
  # 详细配置参考GitHub Actions文档
```

## 📱 APK安装说明

### Android设备安装
1. 从GitHub下载APK文件
2. 在设备上找到下载的APK
3. 点击安装（需要允许安装未知来源应用）
4. 安装完成后打开应用

### 权限设置
首次运行需要：
1. 授予"显示在其他应用上层"权限
2. （可选）禁用电池优化
3. （可选）允许开机自启动

## 🔄 持续集成

### 自动构建规则
- `push`到main分支时自动构建
- 创建Pull Request时进行验证
- 支持手动触发构建

### 构建通知
配置通知方式：
1. 邮件通知
2. Slack通知
3. Discord通知

## 📈 监控和统计

### 构建统计
- 查看构建成功/失败率
- 构建时长统计
- 资源使用情况

### 应用分析
- APK文件大小监控
- 构建版本跟踪
- 发布频率统计

## 💡 最佳实践

1. **版本管理**：使用语义化版本控制
2. **代码审查**：通过Pull Request进行代码审查
3. **测试覆盖**：添加单元测试和UI测试
4. **文档更新**：代码变更时同步更新文档

## 📞 技术支持

遇到问题请：
1. 查看[GitHub Actions文档](https://docs.github.com/actions)
2. 提交[Issue](https://github.com/YOUR_USERNAME/random-screensaver/issues)
3. 查阅项目Wiki

---

**重要**：首次构建可能需要几分钟时间下载Android SDK和依赖包。后续构建会利用缓存加速。