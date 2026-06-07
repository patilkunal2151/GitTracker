# Git Tracker 🚀

Git Tracker is a modern, standalone Android application designed to track GitHub repository releases and updates. Built with the latest Android technologies, it provides a seamless experience for developers and enthusiasts to stay updated with their favorite open-source projects.

## ✨ Features

- **Repository Tracking**: Easily add GitHub repositories via URL to monitor their releases.
- **Adaptive UI**: Built with Jetpack Compose and Material 3, featuring an adaptive layout for phones and tablets.
- **Background Sync**: Automated background checks for new releases using WorkManager.
- **Notifications**: Get notified immediately when a new version of a project is released.
- **Offline First**: Uses Room database for local persistence, ensuring you can browse tracked projects anytime.
- **Efficiency**: Leverages OkHttp Native ETag caching to minimize API rate limit usage and save battery.
- **Data Portability**: Import and export your list of tracked repositories via JSON.

## 🤖 Built with AI

In the spirit of transparency, this project was built entirely using AI-assisted development. This approach was used to showcase how modern AI tools can be leveraged to design, architect, and implement a fully functional, production-ready Android application from scratch.

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: Retrofit & OkHttp
- **Background Tasks**: WorkManager
- **Async Programming**: Coroutines & Flow

## 🚀 Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Ladybug or newer).
3. Sync the Gradle files.
4. Run the app on an emulator or a physical device.

## 🔒 Security & Privacy

Git Tracker uses the public GitHub REST API. It does not require personal access tokens for public repositories, ensuring your credentials stay safe. All tracked data is stored locally on your device.

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
Made with ❤️ by Git Tracker Team
