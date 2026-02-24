# M-Series Scanner

An Android application built with Kotlin to scan, record, and export live workout data from Keiser M-Series bikes (using the M3i BLE protocol). The protocol itself is documented at https://dev.keiser.com/ (Explore Platforms > M Series Direct Communication)

**Disclaimer:** This project is an independent developer tool and is **not** an official product of Keiser Corporation. It is not affiliated with, maintained by, or endorsed by Keiser Corporation.

## Features

- **Real-time BLE Scanning**: Automatically identifies M-Series bikes via manufacturer-specific advertisement data.
- **Live Workout Stats**: Displays real-time cadence (RPM), power (Watts), gear, heart rate (BPM), distance, and caloric burn.
- **Session Recording**: Records workout data points throughout the session.
- **CSV Export**: Automatically generates and offers to share a CSV file containing all session data upon completion.
- **Convenience**: 
  - Prevents screen lock during active sessions.
  - Intercepts back-press with a confirmation dialog to prevent accidental session termination.

## Screenshots

[Scanning for bikes](screenshots/scanning.png)
[Session in progress](screenshots/session.png)
[Session complete](screenshots/end-session.png)

## Project Structure

- `app/src/main/kotlin/com/keiser/scanner/ble`: BLE scanning and packet parsing logic.
- `app/src/main/kotlin/com/keiser/scanner/ui`: Android Activities and ViewModels for the dashboard.
- `app/src/main/kotlin/com/keiser/scanner/data`: Data models and CSV generation logic.

## Prerequisites

- **JDK 17** or newer.
- **Android SDK** (API 36 supported).
- **Physical Android Device**: Required for BLE functionality.

## Getting Started

### Building with Android Studio

1.  Open this project in Android Studio.
2.  Wait for the project to sync and click the **Run** button.

## Permissions

The app requires the following permissions to function correctly:
- `BLUETOOTH_SCAN` & `BLUETOOTH_CONNECT` (Android 12+)
- `ACCESS_FINE_LOCATION` (Android 11 and below)
- `WAKE_LOCK` (To keep the screen on during workouts)

For details on how data is handled, please see [PRIVACY.md](PRIVACY.md).

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

Developed with ❤️ for Keiser M-Series bike users.
