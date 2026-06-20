# Smart Water Irrigation Management System

<div align="center">

![Android](https://img.shields.io/badge/Android-7DDC73?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)

*A comprehensive Android application for intelligent water quality monitoring and automated irrigation management in agriculture.*

[Demo](#) • [Report Bug](#) • [Request Feature](#)

</div>

---

##  Table of Contents

- [About The Project](#-about-the-project)
- [Features](#-features)
- [Screenshots](#-screenshots)
- [Prerequisites](#-prerequisites)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Usage](#-usage)
- [Architecture](#-architecture)
- [Technologies Used](#-technologies-used)
- [Contributing](#-contributing)
- [License](#-license)
- [Contact](#-contact)
- [Acknowledgments](#-acknowledgments)

---

##  About The Project

**Aqua Smart** is an intelligent Android application designed to help farmers monitor water quality and manage irrigation systems efficiently. The app provides real-time sensor data monitoring, automated alerts, pump control, and AI-powered assistance to optimize agricultural water usage.

### Key Objectives

- **Water Quality Monitoring**: Real-time tracking of pH, temperature, and conductivity
- **Smart Irrigation**: Automated pump control based on water quality analysis
- **Alert System**: Instant notifications when water parameters exceed safe thresholds
- **AI Assistant**: Voice and text-based AI assistant for agricultural guidance
- **Weather Integration**: Real-time weather data to optimize irrigation schedules

---

## Features

### Core Functionality

- **Real-time Sensor Monitoring**
  - pH level tracking (optimal range: 6.5 - 8.4)
  - Temperature monitoring (safe range: 15°C - 35°C)
  - Conductivity measurement (acceptable range: 200 - 1200 µS/cm)
  - Water quality scoring system (0-100)

- **Smart Pump Control**
  - Manual pump activation/deactivation
  - Automatic pump control based on water quality
  - Real-time flow rate monitoring
  - Volume calculation and session tracking
  - Irrigation session summaries with detailed analytics

- **Alert Management**
  - Automatic alert generation for out-of-range parameters
  - Alert history and tracking
  - Real-time notifications
  - Alert severity classification (high, medium, low)

- **AI-Powered Assistant**
  - Voice and text interaction
  - Agricultural guidance and recommendations
  - Water quality analysis
  - Irrigation best practices
  - Text-to-speech feedback for all actions

- **Weather Integration**
  - Real-time weather data from OpenWeatherMap
  - Location-based forecasts
  - Temperature and humidity tracking
  - Irrigation scheduling recommendations
    
### User Interface

- **Modern Material Design**
  - Clean, intuitive interface
  - Bottom navigation for easy access
  - Real-time gauge displays
  - Color-coded status indicators

- **Accessibility**
  - Text-to-speech for voice feedback
  - High contrast visual indicators
  - Clear status messages
  - French language support

---



---

##  Prerequisites

### Development Environment

- **Android Studio**: Koala or later
- **JDK**: 11 or higher
- **Android SDK**: API 24 (Android 7.0) or higher
- **Gradle**: 8.0 or higher

### Required Accounts & Services

- **Firebase Account**
  - Realtime Database
  - Project configured for Europe-West1 region

- **OpenWeatherMap API Key**
  - Free tier available at [OpenWeatherMap](https://openweathermap.org/api)

- **Hugging Face API Token**
  - Free token available at [Hugging Face](https://huggingface.co/settings/tokens)

---

##  Installation

### 1. Clone the Repository

```bash
git clone https://github.com/soltanioumayma/smart_water_projet.git
cd smart_water_projet
```

### 2. Open in Android Studio

```bash
# Open the project in Android Studio
# Android Studio will automatically detect the project structure
```

### 3. Configure API Keys

Create a `local.properties` file in the project root directory:

```properties
# OpenWeatherMap API Key
OPENWEATHER_API_KEY=your_openweathermap_api_key_here

# Hugging Face API Token
HF_TOKEN=hf_your_huggingface_token_here
```

### 4. Add Firebase Configuration

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Add an Android app to your Firebase project
3. Download `google-services.json` and place it in `app/` directory
4. Configure Realtime Database for Europe-West1 region

### 5. Sync Gradle

```bash
# In Android Studio, click "Sync Now" or run:
./gradlew build
```

### 6. Run the Application

```bash
# Connect your Android device or start an emulator
# Click the Run button in Android Studio or run:
./gradlew installDebug
```

---

##  Configuration

### Firebase Database Structure

```
smartwater-53b6b-default-rtdb/
├── capteurs/
│   ├── ph: 7.2
│   ├── temperature: 24.3
│   └── conductivity: 420
├── pompe/
│   ├── etat: false
│   ├── debit_reel: 5.0
│   ├── heure_debut: 0
│   └── volume_total: 0
└── alerts/
    ├── alert_pH_ACIDE/
    ├── alert_COND_HAUTE/
    └── alert_TEMP_HAUTE/
```

### Water Quality Thresholds

| Parameter | Min | Max | Unit |
|-----------|-----|-----|------|
| pH | 6.5 | 8.4 | - |
| Temperature | 15 | 35 | °C |
| Conductivity | 200 | 1200 | µS/cm |

---

##  Usage

### Monitoring Water Quality

1. Open the app and navigate to the **Accueil** (Home) tab
2. View real-time sensor data displayed on gauges
3. Check the water quality score (0-100)
4. Review individual parameter readings

### Controlling the Pump

**Automatic Mode:**
- When water quality is acceptable, tap "OK — Ouvrir pompe" in the quality dialog
- The app will automatically activate the pump
- Voice feedback confirms the action

**Manual Mode:**
- Tap the "Arrêter pompe" button to stop the pump
- View the irrigation session summary
- Voice feedback provides session details

### Viewing Alerts

1. Navigate to the **Alertes** tab
2. View active alerts with severity indicators
3. Read detailed explanations and recommendations
4. Alerts automatically clear when parameters normalize

### Using the AI Assistant

1. Navigate to the **Assistant** tab
2. Type or speak your question
3. Receive AI-powered agricultural guidance
4. Voice feedback is provided for all interactions

---

##  Architecture

### Project Structure

```
app/
├── src/main/
│   ├── java/com/example/smart_water_projet/
│   │   ├── MainActivity.java              # Main activity & UI logic
│   │   ├── chat/                          # AI chat integration
│   │   │   ├── GeminiChatClient.java     # API client for AI
│   │   │   └── ChatMessage.java          # Chat message model
│   │   ├── fragments/                     # UI fragments
│   │   │   ├── AlertsFragment.java       # Alerts display
│   │   │   └── FarmerAssistantFragment.java # AI assistant UI
│   │   ├── managers/                      # Business logic
│   │   │   └── AlertManager.java          # Alert generation & management
│   │   ├── models/                        # Data models
│   │   │   └── Alert.java                # Alert data model
│   │   └── adapters/                      # RecyclerView adapters
│   │       └── AlertsAdapter.java         # Alerts list adapter
│   ├── res/
│   │   ├── layout/                        # XML layouts
│   │   ├── values/                        # Resources (strings, colors, etc.)
│   │   └── drawable/                      # Drawables and icons
│   └── AndroidManifest.xml                # App manifest
├── build.gradle.kts                       # App-level Gradle config
└── proguard-rules.pro                     # ProGuard rules
```

### Data Flow

```
Firebase Realtime Database
         ↓
   MainActivity
         ↓
   AlertManager
         ↓
   UI Updates & Alerts
```

---

## Technologies Used

### Core Technologies

- **Language**: Java
- **UI Framework**: Android Jetpack
- **Architecture**: MVVM (Model-View-ViewModel)
- **Build System**: Gradle with Kotlin DSL

### Libraries & Dependencies

- **Firebase**
  - Firebase BOM: 33.1.0
  - Firebase Database
  - Google Services

- **UI Components**
  - AndroidX AppCompat
  - Material Design Components
  - ConstraintLayout
  - RecyclerView
  - Navigation Component

- **Networking**
  - OkHttp 4.x
  - Retrofit (optional)

- **AI & ML**
  - Hugging Face Inference API
  - OpenRouter API

- **Weather**
  - OpenWeatherMap API

### Development Tools

- **Android Studio**: Koala
- **Git**: Version control
- **Gradle**: Build automation

---

##  Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

### How to Contribute

1. **Fork the Project**
   ```bash
   git clone https://github.com/soltanioumayma/smart_water_projet.git
   ```

2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/AmazingFeature
   ```

3. **Commit Your Changes**
   ```bash
   git commit -m 'Add some AmazingFeature'
   ```

4. **Push to the Branch**
   ```bash
   git push origin feature/AmazingFeature
   ```

5. **Open a Pull Request**

### Contribution Guidelines

- Follow the existing code style
- Write clear commit messages
- Add tests for new features
- Update documentation as needed
- Ensure the code compiles without errors

---



---

##  Contact

**Oumayma Soltani**

- **GitHub**: [@soltanioumayma](https://github.com/soltanioumayma)
- **Project Link**: [https://github.com/soltanioumayma/smart_water_projet](https://github.com/soltanioumayma/smart_water_projet)

---

## Acknowledgments

- **Firebase** - For providing the Realtime Database infrastructure
- **OpenWeatherMap** - For weather data API
- **Hugging Face** - For AI model hosting
- **Material Design** - For UI component library
- **Android Developers Community** - For documentation and support

---

##  Project Status

![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)
![Version](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)
![Maintenance](https://img.shields.io/badge/Maintained-Yes-green?style=for-the-badge)

### Roadmap

- [ ] Add support for multiple sensor nodes
- [ ] Implement data export functionality
- [ ] Add historical data visualization
- [ ] Support for multiple languages
- [ ] Cloud backup and sync
- [ ] Web dashboard for remote monitoring

---

<div align="center">

**Built with ❤️ for Smart Agriculture**

[⬆ Back to Top](#-smart-water-irrigation-management-system)

</div>
