# YandeReViewer

YandeReViewer is a **third-party Android client** for browsing content
provided by the public API of **yande.re**.

This project focuses on delivering a smooth, modern, and user-friendly
browsing experience using up-to-date Android development practices.\
The application itself **does not host, store, or distribute any image
content**.

------------------------------------------------------------------------

## ✨ Features

-   **Infinite Scrolling**\
    Seamlessly browse posts using Android's Paging 3 library, combined
    with efficient diffing via `DiffUtil` for smooth performance.

-   **Tag-Based Search**\
    Explore content by searching tags.

-   **Rating Filters**\
    Filter posts by rating: Safe, Questionable, and Explicit.

-   **Internationalization (i18n)**\
    Supports multiple languages, including English and Chinese.

-   **GIF Support**\
    View animated GIFs directly within the app using Coil's GIF decoder.

-   **Immersive Detail View**\
    Tap an image to open a full-screen viewer with pinch-to-zoom
    functionality powered by `PhotoView`.\
    A single tap exits the full-screen mode.

-   **Swipe Navigation**\
    Swipe left or right in the detail view to navigate between adjacent
    posts.

-   **Categorized & Interactive Tags**\
    Tags are dynamically fetched and categorized into **Artist**,
    **Copyright**, **Character**, and **General** groups.\
    Tapping any tag---including artist names---launches a new search
    instantly.

-   **Source Access**\
    If a post includes source information, a "Source" button appears:

    -   URLs are opened in a browser
    -   Plain text sources are displayed in a dialog

-   **Optimized Image Loading**\
    Low-resolution preview images are used as placeholders while
    higher-resolution images load, ensuring seamless transitions.\
    A custom parallel downloader for Coil accelerates image fetching.

-   **Local File Access (User-Initiated)**

    -   Images may be saved **locally on the user's device** upon
        explicit user action.
    -   The application does **not** upload, synchronize, or
        redistribute saved files.
    -   No server-side storage is involved.

-   **Bulk Operations (User-Controlled)**\
    Long-press an item in the grid to enter native `ActionMode`,
    allowing:

    -   Selection of multiple items
    -   Local saving initiated by the user
    -   Copying image links to the clipboard

-   **Efficient Caching**

    -   Image caching via **Coil** (memory + disk)
    -   Paging cache provided by **Paging 3**
    -   A priority-based in-memory cache for tag metadata
        (`TagTypeCache`), optimizing API usage and performance

-   **Smart Auto-Update Checker**\
    Automatically checks GitHub Releases for new versions and displays
    release notes.

------------------------------------------------------------------------

## 🛠️ Tech Stack & Architecture

-   **Language**: Kotlin
-   **Architecture**: Model-View-ViewModel (MVVM)
-   **Asynchronous**: Kotlin Coroutines & Flow
-   **Networking**: Retrofit2 + Gson
-   **Image Loading**: Coil (with GIF support)
-   **Pagination**: Paging 3

------------------------------------------------------------------------

## 🚀 Getting Started

``` bash
git clone https://github.com/AliceJump/YandeReViewer.git
```

------------------------------------------------------------------------

## ⚖️ Disclaimer & Copyright Notice

-   This application is a **third-party client** and is **not affiliated
    with yande.re**.
-   All images and metadata are retrieved directly from the **public
    yande.re API**.
-   **This application does not host, store, or distribute any
    copyrighted content**.
-   All artwork and related content remain the property of their
    respective copyright holders.
-   Files saved through the app are stored **locally on the user's
    device only**.

If you are a copyright holder and believe that content accessed through
this application infringes your rights,\
please contact the original content host (**yande.re**) directly.

For DMCA-related concerns regarding this repository,\
please open an issue or contact the repository owner.

------------------------------------------------------------------------

## 📄 License

This project is licensed under the **MIT License**.
