# DocMeds — Online Pharmacy & Pharmacist Admin Platform

DocMeds is a bilingual (**English / اردو**) online pharmacy Android application built with **Kotlin**, **Jetpack Compose**, **Material Design 3**, **Firebase Authentication (Google Sign-In)**, and **Cloud Firestore**.

## Features

### Customer Storefront
- Searchable medicine catalog by brand name, generic formula, or manufacturer
- Bilingual categories, promotional banners, OTC/Rx filters, and popular products
- Product detail pages with manufacturer details, discount pricing, stock indicators, and regulatory disclaimers
- Persistent shopping cart, wishlist, and saved delivery addresses
- Cash on Delivery (COD) checkout with transactional stock validation
- Mandatory prescription image upload (via Android Photo Picker) for prescription-only medicines
- Live order tracking timeline and prescription verification history

### Pharmacist / Admin Console (Role-Gated)
- Accessible strictly to verified Admin accounts (`/admins/{uid}` + verified bootstrap email)
- Live dashboard metrics (revenue, orders, pending Rx reviews, low-stock alerts)
- Full Medicine & Category CRUD with Photo Picker image uploads
- Prescription review queue with mandatory pharmacist notes (never auto-approved)
- Order fulfillment status updates and customer role management

## Building the Debug APK

1. Ensure JDK 17+ and Android SDK (API 36) are installed.
2. Place your provisioned `app/google-services.json` and `app/src/main/res/values/firebase_applet_config.xml`.
3. Run:
   ```bash
   gradle :app:assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions CI
Pushing to `main` or triggering **Actions → DocMeds Android Debug APK** automatically runs `:app:testDebugUnitTest` and `:app:assembleDebug`, publishing `docmeds-debug-apk` as a downloadable build artifact.

## Firestore Rules Testing
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
