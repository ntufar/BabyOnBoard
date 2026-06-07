<p align="center">
  <img src="web/icon.svg" alt="Baby on Board icon" width="120">
</p>

# Baby on Board — Safe-Driving Telemetry for Families

Baby on Board is an Android-native application designed to provide parents and caregivers with clear, non-judgmental telemetry on driving smoothness and safety. By utilizing on-device sensor fusion (GNSS, accelerometer, gyroscope), the app transforms raw motion data into meaningful insights, specifically tailored for those carrying children.

## 🚀 Key Features

- **Driving Telemetry Engine**: Real-time monitoring of speed, hard braking, hard acceleration, and cornering.
- **Baby Mode**: User-activated stricter safety thresholds that prioritize smoothness and jerk reduction for infant comfort.
- **Safe-Driving Score**: A 0–100 score based on telemetry, normalized by trip distance.
- **Crash Detection & SOS**: Best-effort crash detection with a user-cancellable 60-second countdown before alerting emergency contacts and initiating a 112 dial.
- **Back-seat Reminder**: A "hot-car" backstop reminder at the end of trips in Baby mode.
- **Privacy First**: All telemetry computation happens on-device. No raw sensor data is transmitted.

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt
- **Persistence**: Room (SQLCipher encrypted), DataStore
- **Concurrency**: Coroutines & Flow
- **Background Execution**: Foreground Services

## 🏗 Architecture Overview

- **Domain Layer**: Pure Kotlin use-cases and models.
- **Data Layer**: Repositories, Room DAOs, and DataSources.
- **Sensing Layer**: High-frequency sensor acquisition and the core Telemetry Engine.
- **UI Layer**: Compose-based screens and StateFlow-driven ViewModels.

## 🚦 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/ntufar/BabyOnBoard.git
   ```
2. Open in Android Studio.
3. Build and run the app on a physical Android device (sensors are required).

## 🗺 Roadmap

- [x] MVP: Telemetry Engine, Baby Mode, Trip History, Local Persistence.
- [ ] v1.1: Road roughness, gradient detection, and arrival sharing.
- [ ] v1.2: On-device ML scoring and cloud sync.
