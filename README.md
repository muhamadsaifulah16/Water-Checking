# AquaSense: IoT Water Quality Monitoring System

AquaSense is an integrated IoT solution designed to monitor water quality parameters in real-time. This project was developed as a Final Year Project for an Information Security Assurance degree.

## 🚀 Overview
The system utilizes an **ESP32** microcontroller equipped with various water sensors to collect data, which is then transmitted to a **Firebase Real-time Database**. The **AquaSense Android App** (this repository) fetches this data to provide users with live monitoring, historical trends, and alerts.

## 🛠️ Tech Stack
* **Mobile:** Android Studio (Java/Kotlin)
* **Backend:** Firebase Real-time Database & Authentication
* **Hardware:** ESP32, pH Sensor, Turbidity Sensor, Temperature Sensor
* **Firmware:** C++ (Arduino IDE/PlatformIO)

## 🔐 Security Features (Infosec Focus)
As an Information Security project, AquaSense implements:
* **Environment Variable Protection:** Sensitive API keys and Firebase configurations are managed via `google-services.json` and are excluded from version control to prevent credential leakage.
* **Secure Data Transit:** Utilizes Firebase's encrypted channels for data transmission between the IoT hardware and the mobile application.
* **Input Validation:** Sanitize sensor data inputs to prevent data corruption within the Firebase node.

## 📱 App Features
* **Real-time Dashboard:** View live pH, Turbidity, and Temperature levels.
* **Status Alerts:** Instant notifications if water parameters fall outside safe thresholds.
* **Firebase Integration:** Seamless synchronization across multiple devices.

## 📂 Project Structure
* `/app` - Contains the Android source code.
* `.gitignore` - Configured to protect sensitive project secrets and ignore bulky build files.

---
Developed by **Muhamad Saifulah**
