# YandeReViewer

### Overview
YandeReViewer is a **third-party Android client** for browsing content provided by the public API of **yande.re**. Built with modern Android development practices, it delivers a smooth, intuitive browsing experience with full internationalization support.

**Important**: This application **does not host, store, or distribute any image content**. All content is fetched from yande.re's public API.

---

## ✨ Features

### 🖼️ Core Browsing Experience
- **Infinite Scrolling**: Seamless pagination using Paging 3 with efficient `DiffUtil` processing
- **Tag-Based Search**: 
  - Combine multiple tags (space-separated)
  - Real-time auto-completion from yande.re tag database
  - Smart tag categorization (Artist/Copyright/Character/General)
  - Long-press tags to copy to clipboard
  - Colorful chip badges for different tag types
- **Rating Filters**: Filter posts by Safe, Questionable, and Explicit ratings with checkboxes
- **Smart Caching**: Multi-layer caching strategy:
  - Image caching via Coil (memory + disk)
  - Paging cache for browsing data
  - Tag metadata cache (`TagTypeCache`) for optimized API usage
  - Auto-cleanup expired cache on app start
- **Dual Browsing Modes**:
  - **NORMAL**: Infinite-scroll paged results from yande.re API
  - **FAVORITES**: Local-only mode with tag filtering support
- **Blacklist System**: Automatically filter posts containing blacklisted tags

### 📸 Image Viewing
- **Immersive Detail View**: 
  - Full-screen viewer with pinch-to-zoom (`PhotoView` library)
  - Single tap enters fully immersive mode
  - Elegant shared element transitions
  - Custom back animations based on scroll position
- **Swipe Navigation**: Swipe between adjacent posts with smooth ViewPager2 transitions
- **Optimized Loading**: 
  - Low-resolution preview placeholders
  - High-res images load in background
  - Crossfade transitions without flickering
  - Memory cache key reuse prevents duplicate loads
- **Parallel Image Fetching**: Custom Coil fetcher splits images into 4 parallel chunks
- **Interactive Tags**: 
  - Tap any tag to instant search
  - Long-press menu: Copy/Blacklist/Favorite
  - Artist tags show aliases (Japanese/Romaji)
  - Tags grouped by category
- **Source Access**: 
  - URL sources open in browser
  - Text sources display in dialog
  - Smart URL format detection and protocol completion

### 💾 User Features
- **Local Save (User-Initiated)**: Save images to device storage on explicit user action
  - API 30+: Long-press drag gesture (1s trigger)
  - API <30: System DownloadManager integration
  - Long-press in detail view for quick download
  - Download completion notifications
- **Bulk Operations**: 
  - Long-press grid item to enter selection mode
  - Multi-select images for batch save
  - Batch copy image links to clipboard
  - Toolbar shows selection count
- **Favorites Management**: 
  - Save favorite posts via SharedPreferences
  - Timestamp tracking for each favorite
  - FAB for quick favorite/unfavorite in detail view
  - Navigation drawer for quick favorites view switch
  - Tag filtering support in favorites mode
- **Favorite Tags**: 
  - Bookmark frequently-used tags for quick search
  - Tags organized by category
  - Long-press menu: Copy/Remove
  - Tap favorite tag to search instantly
- **Blacklist System**: 
  - Tag blacklist auto-filters unwanted content
  - Dedicated blacklist management page
  - Local persistent storage

### 🌐 Modern UX & Technical Excellence
- **Internationalization (i18n)**: Full support for English and Chinese (`values/` and `values-zh-rCN/`)
- **Smart Auto-Update Checker**: 
  - Auto-check GitHub Releases on app start
  - Dialog displays version and full changelog
  - One-tap download and install latest APK
- **Tag Metadata Sync**: 
  - Background incremental tag sync on startup
  - Supports Artist, Copyright, Character, General categories
  - Local JSON cache prevents redundant downloads
  - Pre-packaged initial data for instant first launch
- **Material Design 3**: 
  - Dynamic color scheme (Android 12+)
  - Hybrid Jetpack Compose + XML layouts
  - Fluid Material You design language
- **Responsive Animations**: 
  - Shared element transitions
  - Custom exit animations based on scroll position
  - Pull-to-refresh animations
  - Smooth scroll-to-top
- **Navigation Drawer**: 
  - Favorite Tags management
  - Favorite Images view
  - Blacklist management
  - History (planned)
- **Staggered Grid Layout**: Two-column adaptive grid optimizing space usage
- **Floating Action Buttons**: 
  - Quick scroll to top
  - Manual refresh current list

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| **Language** | Kotlin |
| **Architecture** | MVVM with Paging 3 |
| **Async** | Kotlin Coroutines + Flow |
| **Networking** | Retrofit2 + Gson + OkHttp3 |
| **Image Loading** | Coil with custom parallel fetcher |
| **UI Framework** | Jetpack Compose + XML Layouts |
| **Pagination** | Paging 3 (`PagingSource`, `PagingDataAdapter`) |
| **Storage** | SharedPreferences + JSON serialization |
| **Zoom Library** | PhotoView |

### Key Components
- **ViewModels**: `PostViewModel` (paging logic), `UpdateViewModel` (release checks)
- **PagingSource**: `PostPagingSource` queries yande.re API page-by-page
- **Network Clients**: `YandeApi` (post/tag endpoints), `GitHubApiClient` (releases)
- **Custom Fetcher**: `ParallelImageFetcher` for optimized image downloads
- **Tag System**: `TagTypeCache` + `TagSyncer` for metadata management

---

## 🚀 Getting Started

### Download Pre-built APK
Download the latest release APK from:

[View Latest Release](https://github.com/AliceJump/YandeReViewer/releases)

### Build from Source

```bash
# Clone the repository
git clone https://github.com/AliceJump/YandeReViewer.git
cd YandeReViewer

# Debug build (uses release signing config)
./gradlew installDebug

# Release build with ProGuard
./gradlew assembleRelease
```

### Signing Configuration
- **CI/CD**: Uses environment variables (`SIGNING_KEY_STORE_PATH`, `SIGNING_KEY_ALIAS`, etc.)
- **Local Development**: Configure in `local.properties`:
  ```properties
  SIGNING_KEY_STORE_PATH=/path/to/keystore.jks
  SIGNING_KEY_ALIAS=your_alias
  SIGNING_KEY_PASSWORD=your_key_password
  SIGNING_STORE_PASSWORD=your_store_password
  ```

---

## ⚖️ Disclaimer & Copyright Notice

- This application is a **third-party client** and is **not affiliated with yande.re**
- All images and metadata are retrieved directly from the **public yande.re API**
- **This application does not host, store, or distribute any copyrighted content**
- All artwork and related content remain the property of their respective copyright holders
- Files saved through the app are stored **locally on the user's device only**
- For DMCA-related concerns regarding this repository, please open an issue or contact the repository owner

---

## 📄 License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.
