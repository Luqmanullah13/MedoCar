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

1. Ensure JDK 21 (or 17+) and Android SDK (API 36) are installed.
2. Ensure `app/google-services.json` (with the OAuth 2.0 Web Client ID `client_type: 3` for `com.aistudio.docmeds.kxpqzm`) and `app/src/main/res/values/firebase_applet_config.xml` are present.
3. Run:
   ```bash
   gradle :app:assembleDebug
   ```
   Output APK: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions CI
Pushing to `main` or triggering **Actions → DocMeds Android Debug APK** automatically runs `:app:testDebugUnitTest` and `:app:assembleDebug`, publishing `docmeds-debug-apk` as a downloadable build artifact.

- **Firebase Config in CI**: Commit `app/google-services.json` to the repository, or configure the `GOOGLE_SERVICES_JSON_BASE64` (or `GOOGLE_SERVICES_JSON`) GitHub Actions secret.
- **Debug Signing in CI**: The workflow decodes `DEBUG_KEYSTORE_BASE64` (secret) or `debug.keystore.base64` (repo), and falls back to generating an ephemeral CI `debug.keystore` if neither is present.


## Firestore Rules Testing
```bash
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```
