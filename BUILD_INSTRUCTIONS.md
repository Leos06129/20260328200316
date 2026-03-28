# Android随机屏保应用 - 构建指南

## 项目简介
这是一个Android随机屏保应用，具有以下功能：
1. 全屏随机显示文字（颜色、大小、位置随机）
2. 锁屏检测（锁屏时10-12秒，常规时用户可设置间隔）
3. 用户自定义语句和时间间隔
4. 后台服务持续运行
5. 系统权限管理

## 构建步骤

### 方法1：使用Android Studio（推荐）

1. **安装Android Studio**
   - 下载并安装最新版Android Studio
   - 安装过程中选择Java开发环境

2. **导入项目**
   - 打开Android Studio
   - 选择"Open an existing project"
   - 选择本项目的根目录

3. **构建项目**
   - 等待Gradle同步完成
   - 点击菜单栏的"Build" → "Make Project"

4. **生成APK**
   - 点击"Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
   - 或者直接运行"assembleDebug"任务

5. **APK位置**
   - 生成的APK位于：`app/build/outputs/apk/debug/app-debug.apk`

### 方法2：手动构建（需要Java环境）

1. **安装Java JDK 8+**
   - 下载并安装Java JDK
   - 设置环境变量：
     ```
     JAVA_HOME = C:\Program Files\Java\jdk-xx.x.x
     PATH添加：%JAVA_HOME%\bin
     ```

2. **构建命令**
   ```bash
   # 在项目根目录执行
   gradlew.bat assembleDebug
   
   # 如果遇到权限问题
   gradlew.bat clean
   gradlew.bat assembleDebug
   ```

### 方法3：使用预配置环境

如果不想安装完整环境，可以使用：
1. **在线构建工具**（需要网络）
2. **Docker容器**（需要Docker环境）
3. **CI/CD服务**（如GitHub Actions）

## 权限说明

应用需要以下权限：
1. **SYSTEM_ALERT_WINDOW** - 显示在其他应用上层（必需）
2. **WAKE_LOCK** - 保持屏幕唤醒
3. **REQUEST_IGNORE_BATTERY_OPTIMIZATIONS** - 电池优化白名单（可选）

## 测试流程

1. **安装应用**
   - 将APK文件复制到Android设备
   - 在设备上安装（需要允许安装未知来源应用）

2. **首次运行**
   - 打开应用
   - 授予"显示在其他应用上层"权限
   - 进入设置页面配置语句和时间间隔

3. **启动服务**
   - 返回主页面
   - 点击"启动屏保"按钮
   - 应用会显示在通知栏中

4. **验证功能**
   - 观察随机文字显示
   - 测试锁屏/解锁状态
   - 检查后台服务运行

## 故障排除

### 1. 构建失败
- 检查Java环境：`java -version`
- 检查Android SDK路径
- 清理项目：`gradlew.bat clean`

### 2. 安装失败
- 确保设备已启用"开发者选项"
- 启用"USB调试"
- 允许"安装未知来源应用"

### 3. 运行时问题
- 检查权限是否已授予
- 重启设备后重新测试
- 查看应用日志

## 项目结构

```
项目根目录/
├── app/                    # 应用模块
│   ├── src/main/
│   │   ├── java/          # Kotlin源代码
│   │   ├── res/           # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle           # 项目构建配置
├── gradlew.bat           # Windows Gradle包装器
└── README.md             # 项目说明
```

## 联系支持

如需进一步帮助，请提供：
1. 完整的错误日志
2. 设备型号和Android版本
3. 复现步骤

祝构建顺利！🎉