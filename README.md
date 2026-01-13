# YandeReViewer

A simple yet powerful Android client for browsing the [Yande.re](https://yande.re) image board, built with modern Android development practices.

## ✨ Features

-   🖼️ **Infinite Scrolling:** Browse posts seamlessly with a paginated, two-column grid layout.
-   🔍 **Advanced Search:** Search by any combination of tags and easily filter by ratings (Safe, Questionable, Explicit) using dedicated checkboxes.
-   👆 **Multi-Select Mode:** Long-press any image to enter selection mode for powerful batch operations.
-   💾 **Batch Download:** Easily download multiple selected images at once. The downloads are handled reliably by the system's `DownloadManager`.
-   🔗 **Batch Copy Links:** Copy the direct file links of all selected images to your clipboard with a single tap.
-   📄 **Detailed View:** Tap an image to see a scrollable detail page that includes the image and all its associated tags.
-   🏷️ **Interactive Tags:** In the detail view, all tags are displayed as clickable chips. Tap any tag to instantly launch a new search for it.
-   🤏 **Immersive Viewer:** Tap the image in the detail view to open a full-screen, zoomable, and pannable viewer, allowing you to inspect every detail.
-   📥 **Quick Download:** Long-press an image in the detail view to save it directly to your device's gallery.

## 🛠️ Tech Stack & Libraries

-   **100% Kotlin**
-   **Coroutines & Flow** for asynchronous operations.
-   **AndroidX Libraries**:
    -   `AppCompat` for base components.
    -   `ViewModel` for UI-related data lifecycle management.
    -   `Paging 3` for efficient, paginated data loading.
    -   `RecyclerView` for displaying the image grid.
-   **Retrofit** for networking and consuming the Yande.re API.
-   **Coil** for fast and efficient image loading and caching.
-   **PhotoView** for the interactive, zoomable image viewer.
-   **Material Components** for modern UI elements like `Chip` and `ChipGroup`.

## 🚀 Automated Releases (CI/CD)

This project uses **GitHub Actions** for automated builds and releases.

-   **Trigger:** Pushing a new tag in the format `v*` (e.g., `v1.0.1`) to the repository.
-   **Action:** A workflow is automatically triggered to:
    1.  Build a debug version of the application (`app-debug.apk`).
    2.  Create a new GitHub Release based on the tag name.
    3.  Attach the installable `app-debug.apk` file to the release assets, ready for download.

## 🚀 Getting Started

1.  Clone the repository.
2.  Open the project in the latest version of Android Studio.
3.  Let Gradle sync the dependencies.
4.  Build and run the application.

## 📸 Screenshots

*(You can add screenshots of the main screen, selection mode, and detail view here to showcase the app's features.)*