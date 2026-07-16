# CHAMA-HUB — Production Upgrade (Groq AI Edition)

**Premium Digital Savings Group Manager and Intelligent AI Financial Assistant.**

CHAMA-HUB is a cloud-first Android mobile application designed to empower community savings groups (Chamas) with professional financial management tools, real-time synchronization, and AI-driven insights powered by Groq Cloud.

---

## 🚀 Production Upgrade Summary

This repository has been upgraded from a local-first prototype to a production-ready SaaS architecture. Key enhancements include:

### 1. Cloud-First Data Layer
- **Firestore Primary Storage**: All group data, memberships, and financial records are now stored in Google Cloud Firestore.
- **Room Offline Cache**: Local Room database now acts as a high-performance offline cache, automatically synchronized with Firestore real-time listeners.
- **Conflict Resolution**: Implemented "Cloud Wins" synchronization logic for reliable data consistency across multiple devices.

### 2. Enterprise Authentication
- **Firebase Auth Integration**: Replaced simulated authentication with production-ready Firebase Authentication.
- **Secure Sessions**: Full support for Email/Password registration, secure login, and session persistence.
- **Security Features**: Implemented Password Reset and Email Verification flows.

### 3. Multi-Tenant Architecture
- **Dynamic Group Management**: Support for creating multiple groups and switching between them seamlessly.
- **Invite System**: Users can join existing groups using unique, secure invite codes.
- **Role-Based Access Control (RBAC)**: Fine-grained permissions for **Owners**, **Admins**, **Treasurers**, **Secretaries**, and **Members**.

### 4. AI Financial Advisor (Powered by Groq)
- **Groq AI Integration**: Real-time financial analysis and advice powered by Groq Cloud (Llama 3).
- **Smart Fallbacks**: Local rule-based expert system ensures functionality even when API keys are missing or offline.
- **Loan Risk Assessment**: Automated credit scoring based on member contribution history.

---

## 🛠 Prerequisites

- **Android Studio** (Latest Stable Version)
- **Firebase Project**: A configured Firebase project with Authentication and Firestore enabled.
- **Groq API Key**: Obtainable from [Groq Cloud Console](https://console.groq.com/).

---

## 🏃 Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/learninghub44/CHAMA-HUB.git
   ```

2. **Configure Firebase**:
   - Download your `google-services.json` from the Firebase Console.
   - Place it in the `app/` directory of the project.

3. **Environment Variables**:
   Create a `.env` file in the project root:
   ```env
   GROQ_API_KEY=your_groq_api_key
   FIREBASE_AUTH_DOMAIN=your_project.firebaseapp.com
   ```

4. **Build and Run**:
   - Open the project in Android Studio.
   - Sync Gradle files.
   - Run on an emulator or physical device.

---

## 📁 Project Structure

| Package | Description |
|---|---|
| `com.example.data.local` | Room database entities and DAOs (Offline Cache). |
| `com.example.data.repository` | Repository layer managing Firestore/Room synchronization. |
| `com.example.ui.screens` | Jetpack Compose UI screens for all features. |
| `com.example.ui.viewmodel` | Business logic and state management (MVVM). |
| `com.example.ui.theme` | Premium design system (Dark mode by default). |

---

## 📜 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
