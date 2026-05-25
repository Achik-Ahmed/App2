# Achik Android WebView App

Lightweight Android WebView wrapper for **https://app.achik.us** with Google AdMob.

## Open in Android Studio
1. Android Studio (Hedgehog+) → **Open** → select this folder.
2. Let Gradle sync (downloads Gradle 8.5 / AGP 8.2).
3. Run on a device/emulator with **Android 8.0+ (API 26)**.

## AdMob IDs (already wired)
- App ID: `ca-app-pub-5654505690672290~5079133520`
- Rewarded: `ca-app-pub-5654505690672290/3056439561`
- Interstitial: `ca-app-pub-5654505690672290/6367507923`

## Ad behavior
- **Rewarded**: shown only after **≥30 s** session time **AND** user interacted.
- **Interstitial**: shown only after **≥90 s** session time **AND** user interacted (touch or navigation). A background tick attempts to show it every 30 s once eligible. Minimum 60 s spacing between any two ads.
- Ads preload in the background and never block the WebView.

## Features
- Material 3 UI, light & dark themes
- Splash screen (AndroidX SplashScreen API)
- Top loading progress bar
- Pull-to-refresh (SwipeRefreshLayout)
- Offline screen with Retry
- Hardware back button navigates WebView history
- Secure WebView: HTTPS only, mixed content blocked, external links open in browser
- Min SDK 26 (Android 8.0), Target SDK 34

## Generate launcher icons
Use Android Studio's **Image Asset Studio** (right-click `res` → New → Image Asset) to generate `ic_launcher` and `ic_launcher_round` for all densities. Placeholder mipmap directories are included.

## Release build
```
./gradlew assembleRelease
```
Configure your own signing config in `app/build.gradle` before publishing.
