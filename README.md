# FLOW

FLOW is a comprehensive productivity Android application designed to help users manage their daily activities seamlessly. It integrates task management, note-taking, and personal finance tracking into a single, intuitive interface. 

## Features

- **User Authentication**: Secure login and registration powered by Firebase Authentication.
- **Biometric Security**: Supports fingerprint/face unlock for enhanced privacy and quick access.
- **Task Management**: Create, track, and manage your daily tasks.
- **Note-Taking**: Keep your thoughts organized with rich notes.
- **Finance Tracking**: Monitor your expenses and income easily.
- **Cloud Sync**: Data is synced in real-time across devices using Firebase Firestore.
- **Offline Support**: Local caching and data persistence using Room Database, ensuring the app works even without an internet connection.
- **Media Storage**: Upload and view images using Firebase Cloud Storage and Glide.

## Tech Stack & Architecture

- **Language**: Kotlin
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **UI Components**: XML Layouts with ViewBinding, Navigation Component
- **Database (Local)**: Room Database
- **Database (Remote)**: Firebase Firestore
- **Authentication**: Firebase Auth, Android Biometric Prompt
- **Asynchronous Operations**: Kotlin Coroutines
- **Image Loading**: Glide

## Prerequisites

To build and run this project, you need:
- Android Studio (latest version recommended)
- Android SDK (API 34)
- A connected Android device or emulator running API 26 or higher.

## Getting Started

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Sasikumaran-SA/FLOW.git
   ```
2. **Open the project in Android Studio:**
   - Select `File > Open` and choose the `FLOW` directory.
3. **Setup Firebase:**
   - Go to the [Firebase Console](https://console.firebase.google.com/).
   - Create a new project and register an Android app with the package name `com.example.flow`.
   - Download the `google-services.json` file and place it in the `app/` directory of the project.
   - Enable Authentication (Email/Password), Firestore Database, and Storage in your Firebase console.
4. **Build and Run:**
   - Click the **Run** button (`Shift + F10`) in Android Studio to deploy the app to your emulator or device.
