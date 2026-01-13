# YandeReViewer

An Android application for browsing and viewing images from the Yande.re image board, built with modern Android development practices. It provides a simple, fast, and intuitive user experience for exploring a vast collection of anime-style artwork.

## ✨ Features

- **Infinite Scrolling**: Seamlessly browse through thousands of posts using Android's Paging 3 library.
- **Powerful Search**: Find specific images by tags.
- **Rating Filters**: Easily filter posts by ratings (Safe, Questionable, Explicit).
- **Immersive Detail View**: Tap an image to open a full-screen viewer with pinch-to-zoom capabilities, powered by `PhotoView`.
- **Swipe Navigation**: Intuitively swipe left and right in the detail view to move between adjacent posts.
- **Interactive Tags**: In the detail view, tap on tags (such as artist, character, or copyright) to immediately launch a new search for that tag.
- **Download Manager**: Download your favorite images directly to your device's "Downloads" folder.
- **Bulk Operations**: Long-press an image to enter selection mode, allowing you to download multiple images or copy their links at once.
- **Efficient Caching**: 
    - Leverages **Coil** for robust image caching (memory and disk), ensuring fast load times and reduced network usage.
    - Utilizes **Paging 3**'s built-in caching for a smooth and responsive browsing experience.
    - Implements a custom in-memory cache for tag metadata to optimize detail view performance.
- **Auto Update Checker**: The app automatically checks for new versions from the project's GitHub Releases to keep you up-to-date.
- **"Spotlight" Highlight**: When returning from the detail view to the main grid, the previously viewed image is briefly highlighted with a "spotlight" effect for better visual context.

## 🛠️ Tech Stack & Architecture

- **Language**: **Kotlin**
- **Core**: 
    - **Koltin Coroutines & Flow** for asynchronous operations.
- **Architecture**: 
    - **Model-View-ViewModel (MVVM)** to separate UI logic from business logic.
- **UI**: 
    - **Android Views with XML**.
    - **Material Components** for modern UI elements like Chips, Cards, and Buttons.
    - **ViewPager2** for swipeable views.
    - **RecyclerView** for displaying the main image grid efficiently.
- **Networking**: 
    - **Retrofit2** for type-safe REST API communication.
    - **Gson** for JSON serialization and deserialization.
- **Image Loading**: 
    - **Coil** for fast, efficient image loading, caching, and GIF support.
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
