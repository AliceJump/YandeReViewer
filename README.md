# YandeReViewer

An Android application for browsing and viewing images from the Yande.re image board, built with modern Android development practices. It provides a simple, fast, and intuitive user experience for exploring a vast collection of anime-style artwork.

## ✨ Features

- **Infinite Scrolling**: Seamlessly browse through thousands of posts using Android's Paging 3 library.
- **Powerful Search**: Find specific images by tags.
- **Rating Filters**: Easily filter posts by ratings (Safe, Questionable, Explicit).
- **GIF Support**: View animated GIFs directly within the app, thanks to Coil's GIF decoder.
- **Immersive Detail View**: Tap an image to open a full-screen viewer with pinch-to-zoom capabilities, powered by `PhotoView`.
- **Swipe Navigation**: Intuitively swipe left and right in the detail view to move between adjacent posts.
- **Interactive & Categorized Tags**: In the detail view, tags are dynamically fetched and categorized into `Artist`, `Copyright`, `Character`, and `General` for clearer organization. Tapping on any tag—including the author's name—immediately launches a new search.
- **Optimized Image Loading**: Improves the viewing experience by using low-resolution preview images as placeholders while full-resolution images load. It also features a custom parallel downloader for Coil, which fetches images in multiple chunks simultaneously to accelerate load times.
- **Download Manager**: Download your favorite images directly to your device's "Downloads" folder.
- **Bulk Operations**: Long-press an image to enter selection mode, allowing you to download multiple images or copy their links at once.
- **Efficient Caching**: 
    - Leverages **Coil** for robust image caching (memory and disk), ensuring fast load times and reduced network usage.
    - Utilizes **Paging 3**'s built-in caching for a smooth and responsive browsing experience.
    - Implements a smart in-memory cache for tag metadata (`TagTypeCache`) that fetches tag information in the background and prioritizes tags for the currently viewed post, optimizing detail view performance and minimizing API calls.
- **Auto Update Checker**: The app automatically checks for new versions from the project's GitHub Releases to keep you up-to-date.
- **"Spotlight" Highlight**: When returning from the detail view to the main grid, the previously viewed image is briefly highlighted with a "spotlight" effect for better visual context.

## 🛠️ Tech Stack & Architecture

- **Language**: **Kotlin**
- **Core**: 
    - **Kotlin Coroutines & Flow** for asynchronous operations.
- **Architecture**: 
    - **Model-View-ViewModel (MVVM)** to separate UI logic from business logic.
- **UI**: 
    - **Hybrid UI**: A mix of **Jetpack Compose** with **Material 3** and traditional **Android Views with XML**.
    - **Material Components** for modern UI elements like Chips, Cards, and Buttons.
    - **ViewPager2** for swipeable views.
    - **RecyclerView** for displaying the main image grid efficiently.
- **Networking**: 
    - **Retrofit2** for type-safe REST API communication.
    - **Gson** for JSON serialization and deserialization.
    - **Custom Coil Downloader**: Implements a parallel image fetcher to accelerate image downloads.
- **Image Loading**: 
    - **Coil** for fast, efficient image loading and caching, with **GIF support**.
- **Pagination**: 
    - **Paging 3** for loading and displaying large data sets.
- **Image Zoom**: 
    - **PhotoView** for implementing pinch-to-zoom functionality.

## 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/AliceJump/YandeReViewer.git
    ```
2.  Open the project in the latest stable version of **Android Studio**.
3.  Let Gradle sync the dependencies.
4.  Build and run the application on an Android device or emulator.

## 📄 License

This project is licensed under the MIT License. See the `LICENSE` file for details.

---

# YandeReViewer (中文说明)

一个用于浏览和查看 Yande.re 图站的 Android 应用程序，采用现代 Android 开发实践构建。它为探索海量动漫风格艺术品提供了简单、快速且直观的用户体验。

## ✨ 功能特性

- **无限滚动**: 使用 Android Paging 3 库无缝浏览数千个帖子。
- **强大搜索**: 通过标签查找特定图片。
- **分级筛选**: 按评级（安全、存疑、限制级）轻松筛选帖子。
- **GIF 动图支持**: 借助 Coil 的 GIF 解码器，可直接在应用内查看 GIF 动图。
- **沉浸式详情视图**: 点击图片可打开一个支持双指缩放的全屏查看器，由 `PhotoView` 强力驱动。
- **滑动导航**: 在详情视图中直观地左右滑动，即可在相邻的帖子之间切换。
- **交互式和分类标签**: 在详情视图中，标签被动态获取并分为`作者`、`版权`、`角色`和`通用`等类别，使组织更清晰。点击任何标签（包括作者姓名）会立即启动新的搜索。
- **优化的图片加载**: 通过在加载全分辨率图片时使用低分辨率预览图作为占位符，改善了观看体验。它还为 Coil 配备了自定义并行下载器，可同时以多个块获取图像，从而加快加载时间。
- **下载管理器**: 将您喜爱的图片直接下载到设备的“下载”文件夹中。
- **批量操作**: 长按一张图片进入选择模式，可以一次性下载多张图片或复制它们的链接。
- **高效缓存**:
    - 利用 **Coil** 进行强大的图片缓存（内存和磁盘），确保快速加载并减少网络使用。
    - 利用 **Paging 3** 的内置缓存，带来流畅灵敏的浏览体验。
    - 为标签元数据实现了智能内存缓存 (`TagTypeCache`)，它会在后台获取标签信息，并优先处理当前查看帖子所需的标签，从而优化详情视图的性能并最大限度地减少 API 调用。
- **自动更新检查**: 应用会自动从项目的 GitHub Releases 中检查新版本，让您保持最新。
- **“聚光灯”高亮**: 从详情视图返回主网格时，之前查看的图片会通过“聚光灯”效果短暂高亮，以提供更好的视觉上下文。

## 🛠️ 技术栈与架构

- **语言**: **Kotlin**
- **核心**: 
    - **Kotlin Coroutines & Flow** 用于异步操作。
- **架构**: 
    - **Model-View-ViewModel (MVVM)** 将 UI 逻辑与业务逻辑分离。
- **UI**: 
    - **混合 UI**: **Jetpack Compose** 与 **Material 3** 搭配传统的 **Android Views with XML**。
    - **Material Components** 用于现代 UI 元素，如 Chips、Cards 和 Buttons。
    - **ViewPager2** 用于可滑动的视图。
    - **RecyclerView** 高效地显示主图片网格。
- **网络**:
    - **Retrofit2** 用于类型安全的 REST API 通信。
    - **Gson** 用于 JSON 序列化和反序列化。
    - **自定义 Coil 下载器**: 实现了一个并行图片获取器以加速图片下载。
- **图片加载**:
    - **Coil** 用于快速、高效的图片加载和缓存，并 **支持 GIF**。
- **分页**:
    - **Paging 3** 用于加载和显示大型数据集。
- **图片缩放**: 
    - **PhotoView** 用于实现双指缩放功能。

## 🚀 开始使用

1.  **克隆仓库**:
    ```bash
    git clone https://github.com/AliceJump/YandeReViewer.git
    ```
2.  在最新稳定版的 **Android Studio** 中打开项目。
3.  等待 Gradle 同步依赖项。
4.  在 Android 设备或模拟器上构建并运行应用程序。

## 📄 许可证

本项目基于 MIT 许可证授权。详情请见 `LICENSE` 文件。
