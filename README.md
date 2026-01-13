# YandeReViewer

An Android application for browsing and viewing images from the Yande.re image board, built with modern Android development practices. It provides a simple, fast, and intuitive user experience for exploring a vast collection of anime-style artwork.

## ✨ Features

- **Infinite Scrolling**: Seamlessly browse through thousands of posts using Android's Paging 3 library, with efficient data handling via `DiffUtil` for a smooth experience.
- **Powerful Search**: Find specific images by tags.
- **Rating Filters**: Easily filter posts by ratings (Safe, Questionable, Explicit).
- **Internationalization**: Supports multiple languages, including English and Chinese.
- **GIF Support**: View animated GIFs directly within the app, thanks to Coil's GIF decoder.
- **Immersive Detail View**: Tap an image to open a full-screen, pinch-to-zoom viewer powered by `PhotoView`. A simple tap on the image exits the full-screen view.
- **Swipe Navigation**: Intuitively swipe left and right in the detail view to move between adjacent posts.
- **Interactive & Categorized Tags**: In the detail view, tags are dynamically fetched and categorized into `Artist`, `Copyright`, `Character`, and `General` for clearer organization. Tapping on any tag—including the author's name—immediately launches a new search.
- **Source Button**: A "Source" button appears in the detail view if a post has source information. It intelligently opens URLs in a browser and displays plain text in a dialog.
- **Optimized Image Loading**: Improves the viewing experience by using low-resolution preview images as placeholders for a seamless transition while full-resolution images load. It also features a custom parallel downloader for Coil to accelerate image loading.
- **Download Manager**: Download your favorite images directly to your device's "Downloads" folder. A long-press on an image in the detail view provides a quick-download shortcut.
- **Bulk Operations**: Long-press an image in the grid to enter a native `ActionMode`. This allows you to select multiple images to download at once or copy all their links to the clipboard.
- **Efficient Caching**: 
    - Leverages **Coil** for robust image caching (memory and disk).
    - Utilizes **Paging 3**'s built-in caching for a smooth browsing experience.
    - Implements a smart, priority-based in-memory cache for tag metadata (`TagTypeCache`). It fetches tags for the current post with high priority while fetching tags for other posts in the background, optimizing performance and minimizing API calls.
- **Smart Auto-Update Checker**: The app automatically checks for new versions from GitHub Releases. When an update is available, it displays the release notes and provides options to "Update Now," "Ignore this version," or "Remind me in 7 days."
- **UI/UX Polish**:
    - **"Spotlight" Highlight**: When returning from the detail view, the previously viewed image is briefly highlighted for better visual context.
    - **Seamless Transitions**: The full-screen image viewer uses the cached preview image as a placeholder to provide a smooth, uninterrupted experience.
    - **Modern UI Adaptation**: The app correctly handles system window insets, ensuring UI elements like the search bar don't overlap with the status bar on modern Android devices.

## 🛠️ Tech Stack & Architecture

- **Language**: **Kotlin**
- **Core**: 
    - **Kotlin Coroutines & Flow** for asynchronous operations.
- **Architecture**: 
    - **Model-View-ViewModel (MVVM)** to separate UI logic from business logic.
- **UI**: 
    - **Primarily Android Views with XML** for the main activities.
    - **Material Components** for modern UI elements like Chips, Cards, and Buttons.
    - **ViewPager2** for swipeable views.
    - **RecyclerView** with `PagingDataAdapter` for efficient list display.
    - Includes **Jetpack Compose** dependencies for potential future components.
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

- **无限滚动**: 使用 Android Paging 3 库无缝浏览数千个帖子，并通过 `DiffUtil` 高效处理数据，带来流畅的体验。
- **强大搜索**: 通过标签查找特定图片。
- **分级筛选**: 按评级（安全、存疑、限制级）轻松筛选帖子。
- **国际化**: 支持多种语言，包括英语和中文。
- **GIF 动图支持**: 借助 Coil 的 GIF 解码器，可直接在应用内查看 GIF 动图。
- **沉浸式详情视图**: 点击图片可打开一个支持双指缩放的全屏查看器，由 `PhotoView` 强力驱动。在全屏视图下，单击任意处即可退出。
- **滑动导航**: 在详情视图中直观地左右滑动，即可在相邻的帖子之间切换。
- **交互式和分类标签**: 在详情视图中，标签被动态获取并分为`作者`、`版权`、`角色`和`通用`等类别，使组织更清晰。点击任何标签（包括作者姓名）会立即启动新的搜索。
- **来源按钮**: 如果帖子包含来源信息，详情视图中会出现一个“来源”按钮。它会智能地在浏览器中打开来源网址或在对话框中显示来源文本。
- **优化的图片加载**: 通过在加载全分辨率图片时使用低分辨率预览图作为占位符，改善了观看体验，实现了无缝过渡。它还为 Coil 配备了自定义并行下载器，以加快图片加载速度。
- **下载管理器**: 将您喜爱的图片直接下载到设备的“下载”文件夹中。在详情视图中长按图片可快速下载。
- **批量操作**: 在主网格中长按一张图片可进入原生的`ActionMode`（操作模式）。这使您可以一次性选择多张图片进行下载，或将所有选定图片的链接复制到剪贴板。
- **高效缓存**:
    - 利用 **Coil** 进行强大的图片缓存（内存和磁盘）。
    - 利用 **Paging 3** 的内置缓存，带来流畅灵敏的浏览体验。
    - 为标签元数据实现了基于优先级的智能内存缓存 (`TagTypeCache`)。它会优先获取当前查看帖子的标签，同时在后台获取其他帖子的标签，从而优化性能并最大限度地减少 API 调用。
- **智能自动更新检查**: 应用会自动从 GitHub Releases 检查新版本。当有可用更新时，它会显示发行说明，并提供“立即更新”、“忽略此版本”或“7天后提醒”的选项。
- **UI/UX 优化**:
    - **“聚光灯”高亮**: 从详情视图返回主网格时，之前查看的图片会通过“聚光灯”效果短暂高亮，以提供更好的视觉上下文。
    - **无缝过渡**: 全屏图片查看器使用缓存的预览图作为占位符，提供了流畅、不间断的体验。
    - **现代 UI 适配**: 应用能正确处理系统窗口边衬区，确保搜索栏等 UI 元素不会在现代 Android 设备上与状态栏重叠。

## 🛠️ 技术栈与架构

- **语言**: **Kotlin**
- **核心**: 
    - **Kotlin Coroutines & Flow** 用于异步操作。
- **架构**: 
    - **Model-View-ViewModel (MVVM)** 将 UI 逻辑与业务逻辑分离。
- **UI**: 
    - **主要使用 Android Views with XML** 构建核心界面。
    - **Material Components** 用于现代 UI 元素，如 Chips、Cards 和 Buttons。
    - **ViewPager2** 用于可滑动的视图。
    - **RecyclerView** 与 `PagingDataAdapter` 配合，高效地显示列表。
    - 包含了 **Jetpack Compose** 依赖，为未来可能引入的组件做好准备。
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
