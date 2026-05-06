# YandeReViewer — Flutter 版

[中文](#中文)

---

<a name="中文"></a>

## 项目简介

这是 **YandeReViewer** 的 **Flutter 版本**,与原 Kotlin/Android 版本功能对齐。
采用 Flutter 3.x + Dart 3.x 构建,支持 Android / iOS 双平台。

> **重要提示**: 应用本身**不托管、不存储、也不分发任何图片内容**。所有内容均来自 yande.re 公共 API。

---

## ✨ 功能特性

| 功能 | 状态 |
|------|------|
| 瀑布流图片浏览（无限滚动） | ✅ |
| 多标签搜索 + 实时自动补全 | ✅ |
| 评分筛选（Safe / Q / Explicit） | ✅ |
| 图片详情（PhotoView 双指缩放） | ✅ |
| 左右滑动切换图片 | ✅ |
| 沉浸式全屏模式 | ✅ |
| 本地收藏管理 | ✅ |
| 收藏模式浏览 | ✅ |
| 黑名单标签过滤 | ✅ |
| 收藏标签快捷搜索 | ✅ |
| 来源链接打开 | ✅ |
| 标签右键菜单（复制/黑名单/收藏） | ✅ |
| 深色/浅色主题（跟随系统） | ✅ |
| Material Design 3 | ✅ |

---

## 🛠️ 技术栈

| 组件 | 技术 |
|------|------|
| **语言** | Dart 3 |
| **框架** | Flutter 3.x |
| **状态管理** | Provider 6 |
| **网络** | Dio 5 |
| **图片加载** | cached_network_image |
| **图片查看** | photo_view |
| **分页滚动** | infinite_scroll_pagination |
| **瀑布流** | flutter_staggered_grid_view |
| **本地存储** | shared_preferences |
| **URL 打开** | url_launcher |

---

## 📁 目录结构

```
flutter_app/
├── lib/
│   ├── main.dart                     # 应用入口 & Provider 注入
│   ├── api/
│   │   └── yande_api.dart            # yande.re API 客户端 (Dio)
│   ├── models/
│   │   ├── post.dart                 # 帖子数据模型
│   │   └── tag_info.dart             # 标签数据模型
│   ├── providers/
│   │   ├── post_provider.dart        # 帖子列表 & 分页逻辑
│   │   ├── favorites_provider.dart   # 收藏管理（持久化）
│   │   ├── blacklist_provider.dart   # 黑名单（持久化）
│   │   └── favorite_tags_provider.dart # 收藏标签（持久化）
│   ├── screens/
│   │   ├── home_screen.dart          # 主页（搜索、网格、抽屉）
│   │   ├── detail_screen.dart        # 图片详情（滑动 + 缩放）
│   │   ├── blacklist_screen.dart     # 黑名单管理页
│   │   └── favorite_tags_screen.dart # 收藏标签管理页
│   └── widgets/
│       ├── post_grid_item.dart       # 网格卡片组件
│       └── tag_chip_widget.dart      # 标签 Chip 组件
└── android/                          # Android 平台配置
```

---

## 🚀 快速开始

### 环境要求

- Flutter SDK ≥ 3.0.0
- Dart SDK ≥ 3.0.0
- Android SDK 21+ / iOS 12+

### 安装依赖

```bash
cd flutter_app
flutter pub get
```

### 运行

```bash
# 连接设备后运行
flutter run

# 构建 Android APK
flutter build apk --release

# 构建 iOS IPA
flutter build ipa --release
```

---

## ⚖️ 免责声明

- 本应用为**第三方客户端**，与 yande.re **无关联**
- 所有图片及元数据直接来自 **yande.re 公共 API**
- **应用不托管、不存储、不分发任何受版权保护的内容**

---

## 📄 许可证

本项目采用 **MIT License** 许可。详情见根目录 [LICENSE](../LICENSE)
