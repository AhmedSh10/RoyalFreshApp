# 🍎 Royal Fresh App

<div align="center">
  
  ![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
  ![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
  ![Bluetooth](https://img.shields.io/badge/IoT-Bluetooth-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white)
  
</div>

## 📋 Overview

**Royal Fresh App** is a smart inventory and freshness management application designed for grocery stores, supermarkets, and food retailers. The application integrates with IoT sensors via Bluetooth to monitor product freshness, track inventory levels, and provide real-time alerts for optimal stock management.

## ✨ Key Features

- **🌡️ Real-Time Monitoring**: Track temperature and humidity levels of stored products
- **📊 Inventory Management**: Keep track of stock levels and product locations
- **🔔 Smart Alerts**: Receive notifications when products approach expiration dates
- **📱 Bluetooth Integration**: Connect seamlessly with IoT sensors and devices
- **📈 Analytics Dashboard**: View trends and insights about product freshness
- **🔐 Secure Access**: Password-protected interface for authorized personnel only
- **🎨 Modern UI**: Clean and intuitive interface built with Jetpack Compose
- **💾 Offline Support**: Local data storage for uninterrupted operation

## 🛠️ Tech Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **UI Framework**: Jetpack Compose
- **Bluetooth Communication**: Android Bluetooth API (isolated module)
- **Local Database**: Room Database
- **Dependency Injection**: Hilt
- **Coroutines**: For asynchronous operations
- **StateFlow/LiveData**: For reactive data handling

## 🔌 Bluetooth Communication

The app features a dedicated **Bluetooth communication module** that handles all device connectivity and data reception:

- **Isolated Architecture**: Bluetooth logic is separated into its own package for easy maintenance
- **Automatic Data Updates**: Parking/storage space locations update automatically based on Bluetooth data
- **Real-Time Sync**: Continuous data streaming from connected IoT sensors
- **Connection Management**: Robust connection handling with auto-reconnect capabilities

## 🔐 Security Features

- **One-Time Password Screen**: Appears only on first installation
- **Persistent Authentication**: Password required only once unless app is uninstalled
- **Secure Data Storage**: Encrypted local storage for sensitive information
- **Access Control**: Role-based permissions for different user types

## 📱 Screenshots

*Coming soon - Screenshots will be added to showcase the app's interface*

## 🚀 Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 24 or higher (Bluetooth LE requires SDK 18+)
- Kotlin 1.8+
- Bluetooth-enabled Android device for testing

### Installation

1. Clone the repository:
```bash
git clone https://github.com/AhmedSh10/RoyalFreshApp.git
```

2. Open the project in Android Studio

3. Sync Gradle dependencies

4. Build and run the project on a physical device (Bluetooth features require hardware)

### Bluetooth Setup

1. Ensure Bluetooth is enabled on your device
2. Pair with IoT sensors following the in-app instructions
3. Grant necessary Bluetooth permissions when prompted

## 📂 Project Structure

```
app/
├── bluetooth/      # Isolated Bluetooth communication module
├── data/           # Data layer (repositories, data sources)
├── domain/         # Business logic and use cases
├── presentation/   # UI layer (composables, viewmodels)
├── di/             # Dependency injection modules
├── utils/          # Utility classes and helpers
└── models/         # Data models and entities
```

## 🎯 Use Cases

- **Grocery Stores**: Monitor fresh produce and dairy products
- **Restaurants**: Track ingredient freshness in storage areas
- **Warehouses**: Manage temperature-sensitive inventory
- **Food Delivery**: Ensure product quality during storage and transit

## 🔄 Data Flow

1. **IoT Sensors** collect temperature, humidity, and location data
2. **Bluetooth Module** receives and processes sensor data
3. **Local Database** stores data for offline access
4. **UI Layer** displays real-time updates and analytics
5. **Alert System** notifies users of critical conditions

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is available for educational and commercial use.

## 👨‍💻 Developer

**Ahmed Sherif**

- GitHub: [@AhmedSh10](https://github.com/AhmedSh10)
- LinkedIn: [Ahmed Sherif](https://linkedin.com/in/dev-ahmed-sherif)

## 🙏 Acknowledgments

- Built with modern Android development best practices
- Designed for real-world inventory management challenges
- Inspired by IoT and smart retail solutions

---

<div align="center">
  
  **⭐ If you find this project useful, please consider giving it a star!**
  
</div>
