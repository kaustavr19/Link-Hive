# LinkHive

<p align="center">
  <img src="assets/logo.png" alt="LinkHive Logo" width="200" />
</p>

## Overview

**LinkHive** is an offline-first Android app designed to help you save, auto-organize, and intelligently search your collection of links using the power of Gemini AI.

Have you ever saved a link and forgot where or why you saved it? LinkHive solves this by automatically categorizing your saved links and enabling powerful, AI-driven search capabilities.

## Key Features

*   **Offline-First Architecture**: Built with local persistence (Room Database) ensuring your links are accessible even without an active internet connection.
*   **Gemini AI Integration**: Leverages Google's Gemini AI to auto-categorize saved links and provide intelligent, context-aware search capabilities.
*   **Share Intent Support**: Easily save links directly from your browser or other apps using the native Android "Share" menu.
*   **Modern UI**: Built entirely with Jetpack Compose for a reactive, modern, and beautiful user experience.

## Screenshots

*(Placeholder for screenshots. Add screenshots to the `assets/` directory and update the links below.)*

| Home Screen | Saved Links | Search Results |
| :---: | :---: | :---: |
| <img src="assets/screenshot_home.png" width="200" alt="Home Screen"/> | <img src="assets/screenshot_saved.png" width="200" alt="Saved Links"/> | <img src="assets/screenshot_search.png" width="200" alt="Search Results"/> |

## Tech Stack

*   **Platform**: Android (Min SDK 24, Target SDK 36)
*   **Language**: Kotlin
*   **UI Framework**: Jetpack Compose, Material 3
*   **Architecture**: MVVM (Model-View-ViewModel)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Networking**: Retrofit, OkHttp, Moshi
*   **Local Storage**: Room Database
*   **AI Integration**: Firebase AI / Gemini API
*   **Image Loading**: Coil

## Getting Started

Follow these steps to set up the project locally on your machine.

### Prerequisites

*   [Android Studio](https://developer.android.com/studio) (Latest stable version recommended)
*   JDK 11+
*   A Gemini API Key (See instructions below)

### Installation

1.  **Clone the repository:**

    ```bash
    git clone https://github.com/yourusername/LinkHive.git
    cd LinkHive
    ```

2.  **Open in Android Studio:**
    Open Android Studio, select "Open an existing project," and navigate to the cloned `LinkHive` directory.

### Setting up the Gemini API Key

LinkHive requires a Gemini API key to function correctly. The project uses the Secrets Gradle Plugin to securely manage API keys.

1.  Get a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey).
2.  In the root directory of the project, duplicate the `.env.example` file and rename it to `.env`.
3.  Open the newly created `.env` file and replace the placeholder with your actual API key:

    ```env
    GEMINI_API_KEY=your_actual_api_key_here
    ```

    *(Note: The `.env` file is included in `.gitignore` to prevent you from accidentally committing your secret key.)*

### Build and Run

1.  Sync the project with Gradle files in Android Studio.
2.  Select a target device or emulator.
3.  Click the "Run" button (or use `Shift + F10`) to build and launch the app.

## Testing

The project includes configuration for both unit tests and instrumented tests.

*   To run unit tests (including Roborazzi snapshot tests):
    ```bash
    ./gradlew testDebugUnitTest
    ```
*   To run instrumented tests:
    ```bash
    ./gradlew connectedDebugAndroidTest
    ```

## Contributing

Contributions are welcome! If you have a feature request, bug report, or want to contribute code, please feel free to open an issue or submit a pull request.

## License

*(Add your license information here, e.g., MIT License)*
