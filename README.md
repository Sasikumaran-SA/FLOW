<div align="center">
  <h1>FLOW 🌊 (Android)</h1>
  <p><strong>A comprehensive productivity Android application designed to help users manage their daily activities seamlessly.</strong></p>

  <p>
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android" alt="Android" />
    <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF.svg?logo=kotlin" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Architecture-MVVM-blue.svg" alt="MVVM" />
    <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28.svg?logo=firebase" alt="Firebase" />
  </p>
</div>

<hr/>

FLOW integrates task management, note-taking, and personal finance tracking into a single, intuitive interface directly on your mobile device.

## ✨ Key Features

* **🔐 User Authentication:** Secure login and registration powered by Firebase Authentication.
* **👆 Biometric Security:** Supports fingerprint/face unlock for enhanced privacy and quick access.
* **✅ Task Management:** Create, track, and manage your daily tasks.
* **📝 Note-Taking:** Keep your thoughts organized with rich notes.
* **💰 Finance Tracking:** Monitor your expenses and income easily.
* **☁️ Cloud Sync:** Data is synced in real-time across devices using Firebase Firestore.
* **📶 Offline Support:** Local caching and data persistence using Room Database ensures the app works even without an internet connection.
* **🖼️ Media Storage:** Upload and view images using Firebase Cloud Storage and Glide.

## 🛠️ Tech Stack & Architecture

| Category | Technology |
| :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **UI Components** | XML Layouts with ViewBinding, Navigation Component |
| **Local Database** | [Room Database](https://developer.android.com/training/data-storage/room) |
| **Remote Database**| [Firebase Firestore](https://firebase.google.com/docs/firestore) |
| **Authentication** | Firebase Auth, Android Biometric Prompt |
| **Concurrency** | Kotlin Coroutines |
| **Image Loading** | [Glide](https://github.com/bumptech/glide) |

## 🚀 Getting Started

Follow these instructions to build and run this project on your local machine.

### Prerequisites

* **Android Studio** (latest version recommended)
* **Android SDK** (API 34)
* A connected Android device or emulator running **API 26 (Android 8.0)** or higher.

### Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Sasikumaran-SA/FLOW.git
   ```

2. **Open the project:**
   * Launch Android Studio.
   * Select `File > Open` and choose the cloned `FLOW` directory.

3. **Configure Firebase:**
   The project uses Firebase for its backend services. To configure your own instance:
   * Go to the [Firebase Console](https://console.firebase.google.com/).
   * Create a new project and register an Android app with the package name `com.example.flow` (or update it to match your own).
   * Enable **Authentication** (Email/Password), **Firestore Database**, and **Storage**.
   * Download the `google-services.json` file and place it inside the `app/` directory of the project.

4. **Build and Run:**
   * Click the **Run** button (`Shift + F10`) in Android Studio to deploy the app to your emulator or connected device.
