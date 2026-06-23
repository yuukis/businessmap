# Business Map for Android

Business Map is an Android app that plots your contacts' addresses on a map, so you can see at a glance where your business contacts are located, group them, and get directions to visit them.

It is available on Google Play at https://play.google.com/store/apps/details?id=com.github.yuukis.businessmap

## Features

- Plots addresses from your phone's contacts on a Google Map
- Groups contacts by their contact group and lets you filter the map by group
- Shows contact details, directions, drive navigation, and Street View for a selected location
- Geocodes addresses and caches the results locally for faster, offline-friendly lookups
- Creates a home screen shortcut for quick access to the map
- Lists open source licenses used by the app

## Requirements

- Android Studio (recent version)
- JDK 17
- Android SDK with `compileSdk`/`targetSdk` 35 and `minSdk` 24
- A Google Maps SDK for Android API key

## Build

This is a standard Gradle/Android Studio project.

1. Copy `local.properties.sample` to `local.properties` and set `sdk.dir` and `MAPS_API_KEY` (a Google Maps SDK for Android API key).
2. Open the project in a recent version of Android Studio, or run `./gradlew assembleDebug` from the command line.

## Testing

- Unit tests: `./gradlew testDebugUnitTest`
- Instrumented tests (requires a connected device/emulator): `./gradlew connectedDebugAndroidTest`

## Project structure

- `app/src/main/java/com/github/yuukis/businessmap/app` – activities and fragments (UI)
- `app/src/main/java/com/github/yuukis/businessmap/data` – Room database and geocoding cache
- `app/src/main/java/com/github/yuukis/businessmap/model` – data models
- `app/src/main/java/com/github/yuukis/businessmap/util` – utility classes
- `app/src/main/java/com/github/yuukis/businessmap/view` – custom views
- `app/src/main/java/com/github/yuukis/businessmap/widget` – widgets

## Privacy Policy

https://yuukis.github.io/businessmap/privacy-policy.html

## License

This software is licensed under the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)

## Author

Yuuki Shimizu - yuuki@maxio.jp
