# NFCare

This Android application was developed for the 2024-25 Final Year Project, with the scope of improving the healthcare industry through improving patient identification and medication errors. The application allows tracking of patients and medication. It integrates with **Firebase Firestore** to fetch medication and batch-specific information.

---

## Features

- Secure Authentication System
- Searching medications by name
- Scanning NFC-enabled medication boxes
- View different medication information, such as dosage, description, batch number, and expiry date
- Firebase Firestore backend integration
- Patient Identification using NFC wristbands
- Medication verification when administering to patients

---

## Built With
- Android Studio
- Kotlin
- Firebase Firestore
- XML

---

## Requirements to run

- Android device or emulator with NFC (for tag scanning - application has limited features without NFC)
- Firebase Firestore project 

--- 

## Requirements to Develop

- Android Studio
- Android device or emulator with NFC (for tag scanning - application has limited features without NFC)
- Firebase Firestore project 

---

## Project Setup

### 1. **Clone the repository**
```bash
git clone https://github.com/leo-lungu/NFCare-Final-Year-Project.git
cd NFCare-Final-Year-Project
```

### 2. **Open in Android Studio**
- Open Android Studio
- Select "Open an Existing Project"
- Choose the cloned project folder
- Wait for Gradle Sync to complete

### 3. **Set Up Firebase**
- Go to [Firebase Console](https://console.firebase.google.com/)
- Create a new Firebase project (or use an existing one)
- Enable Cloud Firestore in the Build > Firestore Database section
- Download the google-services.json file
- Place it in the app/ directory:

- **Disclaimer: If using the .zip within the submission, Firebase has already been set up!**


```bash
your-project/
└── app/
    └── google-services.json
```


### 4. **Run Application on Physical Device**
- Enable USB or Wireless Debugging (USB Debugging is preferred)
- Connect to Android Studio (Wired/Wi-Fi)
- In Android Studio, press Run 

