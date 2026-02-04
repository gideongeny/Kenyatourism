# 🇰🇪 Kenya Tourism - World Class Safari Companion 2026

<div align="center">

![Kenya Tourism Icon](app/src/main/ic_launcher-web.jpg)

**Experience the Magic of Kenya - Powered by Gemini AI & Open Source Maps**

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Gemini AI](https://img.shields.io/badge/AI-Gemini%201.5%20Flash-orange.svg)](https://deepmind.google/technologies/gemini/)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-ffca28.svg)](https://firebase.google.com/)

</div>

---

## 📱 App Preview

<p align="center">
  <img src="docs/assets/screen_1.jpg" width="250" alt="Home Screen" style="border-radius: 20px; box-shadow: 0 10px 20px rgba(0,0,0,0.5);"/>
  <img src="docs/assets/screen_2.jpg" width="250" alt="Safari AI Guide" style="border-radius: 20px; box-shadow: 0 10px 20px rgba(0,0,0,0.5);"/>
  <img src="docs/assets/screen_3.jpg" width="250" alt="Explore Map" style="border-radius: 20px; box-shadow: 0 10px 20px rgba(0,0,0,0.5);"/>
</p>

## ✨ "World Class" Transformation

This application has been transformed from a basic tourism app into a **World-Class Commercial Platform**. It combines premium aesthetics with cutting-edge technology to offer a seamless experience for global travelers.

## 🌟 Key Features

### 🤖 Safari AI Guide (Gemini 1.5 Flash)
- **Real-time AI Assistant**: Powered by the latest **Gemini 1.5 Flash** (SDK 0.9.0) for lightning-fast responses.
- **Expert Safari Persona**: Meets users as 'Jambo', a professional guide with deep knowledge of Kenyan wildlife and culture.
- **Swahili Integration**: Professional yet friendly tone using authentic Swahili phrases.

### 🗺️ Fee-Free Global Mapping (OSM)
- **OpenStreetMap Integration**: Switched from Google Maps to **OSMDroid** to eliminate API billing.
- **Zero-Cost Scaling**: Perfect for millions of users without surprise fees.
- **Interactive Exploration**: High-performance markers and intuitive navigation across all 100+ destinations.

### 🌍 Global Community & Reviews
- **Firebase Firestore Sync**: User reviews and ratings sync instantly to a global real-time database.
- **Authentic Feedback**: Help fellow travelers discover the best spots with community-driven insights.

### 🎨 Ultra-Premium UI/UX
- **Refreshed Aesthetic**: High-contrast gradient overlays for crystal-clear readability on stunning safari photography.
- **Fluid Core**: 3D rotation animations, smooth transitions, and a savannah-inspired design system.
- **Modern Architecture**: Built with **Jetpack Compose** and **MVVM** for robust performance.

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| **Core** | Kotlin, Coroutines, Flow |
| **UI** | Jetpack Compose, Material 3 |
| **AI** | Google Generative AI (Gemini 1.5 Flash) |
| **Maps** | OSMDroid (OpenStreetMap) |
| **Database** | Room (Offline Support), Firebase Firestore (Live Reviews) |
| **Analytics** | Firebase Analytics & Google Services |
| **Images** | Coil, Lottie |

## 🚀 Deployment & Build

### Package Name: `com.gideongeng.kenyatourism`

#### 🔧 Required Configuration
1.  **Firebase**: Ensure `google-services.json` is present in the `app/` directory.
2.  **API Key**: The Gemini API key is integrated in `MainActivity.kt`.

#### 📦 Build Commands
```bash
# Clean and Build Debug APK
./gradlew clean assembleDebug

# Build Production App Bundle (AAB)
./gradlew bundleDebug
```

## 📂 Project Structure

```
Kenyatourism/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/gideongeng/kenyatourism/
│   │   │   │   ├── ai/            # Gemini AI Logic
│   │   │   │   ├── data/          # Room & Firestore Repositories
│   │   │   │   ├── ui/            # Screens & Components
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/               # Premium Resources
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── google-services.json
└── build.gradle.kts
```

## 👨‍💻 Author

**Gideon Geny**
- GitHub: [@gideongeny](https://github.com/gideongeny)

---

<div align="center">

**Made with ❤️ for Kenya**

*Discover. Explore. Experience. Magically.*

</div>
