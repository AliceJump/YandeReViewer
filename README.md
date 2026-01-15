# YandeReViewer

YandeReViewer 是一个 **第三方 Android 客户端**，用于浏览 **yande.re** 公共 API 提供的内容。

YandeReViewer is a **third-party Android client** for browsing content provided by the public API of **yande.re**.

本项目旨在提供流畅、现代、易用的浏览体验，并采用最新的 Android 开发实践。
应用本身 **不托管、不存储、也不分发任何图片内容**。

This project focuses on delivering a smooth, modern, and user-friendly browsing experience using up-to-date Android development practices.
The application itself **does not host, store, or distribute any image content**.

---

## ✨ Features / 特色功能

* **Infinite Scrolling / 无限滚动**
  使用 Android Paging 3 库实现无缝浏览，结合 `DiffUtil` 高效 diff 处理，体验更流畅。
  Seamlessly browse posts using Android's Paging 3 library, combined with efficient diffing via `DiffUtil` for smooth performance.

* **Tag-Based Search / 标签搜索**
  支持通过标签查找内容。
  Explore content by searching tags.

* **Rating Filters / 评分筛选**
  按 Safe / Questionable / Explicit 过滤帖子。
  Filter posts by rating: Safe, Questionable, and Explicit.

* **Internationalization (i18n) / 国际化**
  支持多语言，包括英文和中文。
  Supports multiple languages, including English and Chinese.

* **Immersive Detail View / 沉浸式详情**
  点击图片进入全屏查看，可缩放 (`PhotoView`)。单击可退出全屏。
  Tap an image to open a full-screen viewer with pinch-to-zoom functionality powered by `PhotoView`. A single tap exits full-screen mode.

* **Swipe Navigation / 滑动切换**
  在详情页左右滑动切换相邻图片。
  Swipe left or right in the detail view to navigate between adjacent posts.

* **Categorized & Interactive Tags / 分类标签与互动**
  标签按 **Artist / Copyright / Character / General** 分类。点击标签可立即进行搜索，包括艺术家名。
  Tags are dynamically fetched and categorized into Artist, Copyright, Character, and General groups. Tapping any tag—including artist names—launches a new search instantly.

* **Source Access / 来源访问**
  帖子含有来源时显示“Source”按钮：URL 会在浏览器打开，普通文本会在对话框显示。
  If a post includes source information, a "Source" button appears: URLs are opened in a browser, plain text sources are displayed in a dialog.

* **Optimized Image Loading / 优化图片加载**
  使用低分辨率预览图作为占位，同时加载高清图，自定义并行下载加速 Coil。
  Low-resolution preview images are used as placeholders while higher-resolution images load, ensuring seamless transitions.

* **Local File Access (User-Initiated) / 本地保存（用户主动操作）**
  图片可在用户明确操作下保存到设备，不上传、不同步、不分发，无服务器存储。
  Images may be saved locally on the user's device upon explicit user action. The application does not upload, synchronize, or redistribute saved files. No server-side storage is involved.

* **Bulk Operations (User-Controlled) / 批量操作**
  长按 Grid 项进入原生 `ActionMode`，可进行多选、本地保存或复制图片链接。
  Long-press an item in the grid to enter native `ActionMode`, allowing selection of multiple items, local saving, and copying image links to the clipboard.

* **Efficient Caching / 高效缓存**
  Coil 内存+磁盘缓存，Paging 3 分页缓存，标签元数据缓存 (`TagTypeCache`) 优化 API 使用。
  Image caching via Coil (memory + disk), Paging cache provided by Paging 3, and a priority-based in-memory cache for tag metadata (`TagTypeCache`), optimizing API usage and performance.

* **Smart Auto-Update Checker / 智能更新检测**
  自动检查 GitHub Release 并显示更新说明。
  Automatically checks GitHub Releases for new versions and displays release notes.

---

## 🛠️ Tech Stack & Architecture / 技术栈与架构

* **Language / 语言**: Kotlin
* **Architecture / 架构**: MVVM (Model-View-ViewModel)
* **Asynchronous / 异步**: Kotlin Coroutines & Flow
* **Networking / 网络**: Retrofit2 + Gson
* **Image Loading / 图片加载**: Coil
* **Pagination / 分页**: Paging 3

---

## 🚀 Getting Started / 快速开始

您可以通过以下方式获取最新版本：
You can get the latest release using the following methods:

1. 克隆仓库并自行编译：
   Clone the repository and build it yourself:

```bash
git clone https://github.com/AliceJump/YandeReViewer.git
```

2. 下载最新 Release APK 并直接安装：
   Download the latest Release APK and install it directly:

[查看最新 Release / View Latest Release](https://github.com/AliceJump/YandeReViewer/releases)

---

## ⚖️ Disclaimer & Copyright Notice / 免责声明与版权声明

* 本应用为 **第三方客户端**，与 yande.re **无关**

* 所有图片及元数据来自 **yande.re 公共 API**

* **应用不托管、不存储、不分发任何受版权保护的内容**

* 所有作品及相关内容归原作者所有

* 用户保存的文件仅存储于 **本地设备**

* This application is a **third-party client** and is **not affiliated with yande.re**.

* All images and metadata are retrieved directly from the **public yande.re API**.

* **This application does not host, store, or distribute any copyrighted content**.

* All artwork and related content remain the property of their respective copyright holders.

* Files saved through the app are stored **locally on the user's device only**.

如果您是版权方并认为通过本应用访问的内容侵犯了您的权利，请直接联系原站 **yande.re**。
DMCA 或其他版权问题可在仓库中开 issue 或联系作者。

For DMCA-related concerns regarding this repository, please open an issue or contact the repository owner.

---

## 📄 License / 许可

本项目采用 **MIT License**
This project is licensed under the **MIT License**
