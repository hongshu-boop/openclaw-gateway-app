# OpenClaw Gateway - MIT App Inventor 项目

## 快速开始

1. 访问 https://ai2.appinventor.mit.edu/
2. 登录（可用Google账号）
3. 点击 **"Create New Project"**
4. 项目名：`OpenClawGateway`

## 界面设计 (Designer)

### 组件列表

| 组件 | 类型 | 名称 | 属性设置 |
|------|------|------|----------|
| 主布局 | VerticalArrangement | MainLayout | Width: Fill parent, Height: Fill parent |
| 配置面板 | VerticalArrangement | ConfigPanel | Width: Fill parent, Visible: ✓ |
| 网关地址输入 | TextBox | UrlInput | Hint: "网关地址 (如: http://192.168.1.100:8080)", Width: Fill parent |
| Token输入 | TextBox | TokenInput | Hint: "Token (可选)", Width: Fill parent |
| 保存按钮 | Button | SaveBtn | Text: "保存并连接", Width: Fill parent |
| Web浏览框 | WebViewer | WebView | Width: Fill parent, Height: Fill parent, Visible: ✗ |
| 重新配置按钮 | Button | ReconfigBtn | Text: "⚙️ 设置", Width: 100px, Visible: ✗ |
| 刷新按钮 | Button | RefreshBtn | Text: "🔄", Width: 80px, Visible: ✗ |
| 本地数据库 | TinyDB | TinyDB1 | - |
| 对话框 | Notifier | Notifier1 | - |

### 布局结构

```
Screen1
├── MainLayout (VerticalArrangement)
│   ├── ConfigPanel (VerticalArrangement)
│   │   ├── UrlInput (TextBox)
│   │   ├── TokenInput (TextBox)
│   │   └── SaveBtn (Button)
│   └── WebView (WebViewer)
├── ReconfigBtn (Button) - 放在右上角
└── RefreshBtn (Button) - 放在ReconfigBtn旁边
```

## 代码逻辑 (Blocks)

### 1. Screen1.Initialize
```
当 Screen1.Initialize 执行
  设置 UrlInput.Text 为 TinyDB1.GetValue("url", "")
  设置 TokenInput.Text 为 TinyDB1.GetValue("token", "")
  
  如果 TinyDB1.GetValue("url", "") 不等于 ""
    调用 LoadGateway
```

### 2. SaveBtn.Click
```
当 SaveBtn.Click 执行
  如果 UrlInput.Text 等于 ""
    调用 Notifier1.ShowAlert("请输入网关地址")
    返回
  
  TinyDB1.StoreValue("url", UrlInput.Text)
  TinyDB1.StoreValue("token", TokenInput.Text)
  
  调用 LoadGateway
```

### 3. LoadGateway 过程
```
过程 LoadGateway
  设置 ConfigPanel.Visible 为 false
  设置 WebView.Visible 为 true
  设置 ReconfigBtn.Visible 为 true
  设置 RefreshBtn.Visible 为 true
  
  WebView.HomeUrl = TinyDB1.GetValue("url", "")
  WebView.GoHome
  
  // 延迟注入token
  调用 Clock1.TimerEnabled = true
```

### 4. Clock1.Timer (注入Token)
```
当 Clock1.Timer 执行
  设置 Clock1.TimerEnabled 为 false
  
  token = TinyDB1.GetValue("token", "")
  如果 token 不等于 ""
    js = "javascript:localStorage.setItem('openclaw_token', '" + token + "');"
    WebView.GoToUrl(js)
```

### 5. ReconfigBtn.Click
```
当 ReconfigBtn.Click 执行
  设置 ConfigPanel.Visible 为 true
  设置 WebView.Visible 为 false
  设置 ReconfigBtn.Visible 为 false
  设置 RefreshBtn.Visible 为 false
```

### 6. RefreshBtn.Click
```
当 RefreshBtn.Click 执行
  WebView.Reload
```

### 7. WebView.BackPressed
```
当 WebView.BackPressed 执行
  如果 WebView.CanGoBack
    WebView.GoBack
  否则
    调用 Notifier1.ShowAlert("已经是第一页了")
```

## 需要添加的组件（在Designer左侧Palette）

1. **User Interface**
   - VerticalArrangement × 2
   - TextBox × 2
   - Button × 3
   - WebViewer × 1

2. **Storage**
   - TinyDB × 1

3. **User Interface**
   - Notifier × 1

4. **Sensors**
   - Clock × 1 (TimerInterval: 2000, TimerEnabled: false)

## 打包APK

1. 点击顶部菜单 **"Build"**
2. 选择 **"App (provide QR code for .apk)"** 或 **"App (save .apk to my computer)"**
3. 等待构建完成（约1-2分钟）
4. 扫描二维码下载或直接下载APK

## 使用说明

1. 首次打开输入网关地址（如 `http://192.168.1.100:8080`）
2. 可选输入Token用于自动登录
3. 点击"保存并连接"
4. 之后自动记住配置
5. 点击"⚙️ 设置"可重新配置

---

**注意**: MIT App Inventor完全免费，无需安装任何软件。
