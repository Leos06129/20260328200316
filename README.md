# Android随机屏保应用

一个Android随机屏保应用，可以在锁屏和常规状态下随机显示自定义文字，每次显示的文字颜色、大小、位置都随机生成。

## 🌟 功能特性

### 核心功能
1. **锁屏模式**：锁屏时随机10-12秒显示文字内容
2. **常规模式**：不锁屏时随机10-20秒全屏显示同一语句
3. **随机显示**：每次显示文字颜色、大小、位置都随机生成
4. **用户自定义**：可输入语句内容、随机运行时间上限
5. **背景设置**：常规模式下背景全黑色

### 技术特性
- ✅ 原生Android应用（Kotlin）
- ✅ 前台服务管理
- ✅ 锁屏状态检测
- ✅ 悬浮窗权限管理
- ✅ 后台持续运行
- ✅ 电池优化支持

## 📱 应用截图

| 主界面 | 设置页面 | 全屏显示 |
|--------|----------|----------|
| ![主界面](screenshots/main.png) | ![设置页面](screenshots/settings.png) | ![全屏显示](screenshots/fullscreen.png) |

## 🚀 快速开始

### 方法1：直接下载APK（推荐）
1. 前往 [Releases页面](https://github.com/yourusername/random-screensaver/releases)
2. 下载最新版本的 `app-debug.apk`
3. 安装到Android设备（需要允许安装未知来源应用）

### 方法2：通过GitHub Actions构建
1. Fork本仓库
2. 在Actions标签页启用工作流程
3. 推送到main分支会自动构建
4. 在构建完成后下载APK

### 方法3：本地构建
```bash
# 克隆项目
git clone https://github.com/yourusername/random-screensaver.git

# 进入项目目录
cd random-screensaver

# 构建APK
./gradlew assembleDebug

# APK位置
# app/build/outputs/apk/debug/app-debug.apk
```

## 🔧 使用说明

### 首次使用
1. **安装应用**：安装APK文件
2. **授予权限**：应用需要"显示在其他应用上层"权限
3. **启动服务**：在主界面点击"启动屏保"

### 配置设置
1. **自定义语句**：在设置页面输入要显示的文字
2. **时间间隔**：设置显示的时间范围（5-60秒）
3. **自启动选项**：可选择开机自动启动

### 工作模式
- **锁屏状态**：固定10-12秒间隔显示
- **常规状态**：使用用户设置的时间间隔
- **后台运行**：应用会在通知栏显示状态

## 📁 项目结构

```
random-screensaver/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/randomscreensaver/
│   │   │   ├── MainActivity.kt          # 主界面
│   │   │   ├── SettingsActivity.kt      # 设置界面
│   │   │   ├── FullscreenActivity.kt    # 全屏显示
│   │   │   ├── ScreenService.kt         # 后台服务
│   │   │   ├── ScreenStateReceiver.kt   # 锁屏检测
│   │   │   └── NotificationHelper.kt    # 通知管理
│   │   └── res/
│   │       ├── layout/                  # 布局文件
│   │       ├── values/                  # 资源文件
│   │       └── xml/                     # 配置文件
├── .github/workflows/
│   └── android-build.yml                # CI/CD配置
└── gradle/                              # Gradle配置
```

## 🔐 权限说明

| 权限 | 用途 | 必需 |
|------|------|------|
| `SYSTEM_ALERT_WINDOW` | 显示在其他应用上层 | ✅ |
| `WAKE_LOCK` | 保持屏幕唤醒 | ✅ |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | 电池优化白名单 | ⚠️ |
| `RECEIVE_BOOT_COMPLETED` | 开机自启动 | ⚠️ |

⚠️ 可选权限：建议授予以获得最佳体验

## 🤝 贡献指南

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系与支持

遇到问题或有建议？
- 提交 [Issue](https://github.com/yourusername/random-screensaver/issues)
- 查看 [Wiki](https://github.com/yourusername/random-screensaver/wiki)

## 🌐 相关链接

- [应用演示视频](https://youtube.com/demo)
- [设计文档](docs/DESIGN.md)
- [API文档](docs/API.md)

---

**注意**：首次运行需要手动授予悬浮窗权限，否则无法正常显示。建议关闭电池优化限制以确保后台服务稳定运行。