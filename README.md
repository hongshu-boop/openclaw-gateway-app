# OpenClaw Gateway Android App

轻量级Android WebView封装，将OpenClaw网关网页打包成原生APP。

## 📱 快速下载APK

**方式1：GitHub Releases（推荐）**
1. 点击上方 [Releases](../../releases)
2. 下载最新版本的 `app-debug.apk`
3. 在手机上安装

**方式2：GitHub Actions Artifacts**
1. 点击上方 [Actions](../../actions)
2. 选择最新的成功构建
3. 下载 `openclaw-gateway-apk` 工件

## ⚙️ 首次配置

安装后首次启动会弹出配置：
- **网关地址**: 输入你的OpenClaw网关地址，如 `http://192.168.1.100:8080`
- **Token**: 可选，用于自动登录

配置会自动保存，下次启动直接使用。

## 🔄 重新配置

点击右上角菜单 → **设置**，可修改网关地址和Token。

## 🛠 功能

- ✅ WebView加载网关网页
- ✅ 本地存储网关地址和Token
- ✅ 启动时自动填充登录
- ✅ 支持返回键导航
- ✅ 下拉刷新
- ✅ 深色主题

## 📁 项目结构

```
android/
├── app/src/main/
│   ├── java/com/openclaw/gateway/
│   │   └── MainActivity.java
│   └── res/
│       ├── layout/activity_main.xml
│       ├── menu/main_menu.xml
│       └── values/
└── build.gradle
```

## 📝 自行构建

```bash
cd android
./gradlew assembleDebug
```

APK输出：`app/build/outputs/apk/debug/app-debug.apk`

## 🔒 权限

- `INTERNET` - 访问网关
- `ACCESS_NETWORK_STATE` - 检测网络状态

---

**注意**: 首次安装可能需要在手机设置中允许"安装未知来源应用"。
