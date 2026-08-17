# Installation & Build Guide

## Prerequisites
- JDK 17
- Android Studio (Koala+), Android SDK 35
- A device/emulator on Android 10 (API 29)+

## First-time setup
```bash
cp local.properties.sample local.properties   # set sdk.dir
gradle wrapper --gradle-version 8.9            # generate wrapper jar (binary, not committed)
```

## Build & run
```bash
./gradlew testDevDebugUnitTest        # JVM tests (ISO 6346, golden payload, wire compat, retention)
./gradlew assembleDevDebug            # dev debug APK
./gradlew connectedDevDebugAndroidTest  # instrumented tests (needs device/emulator)
```
Install the `devDebug` variant on a device, grant camera permission when prompted.

## Variants
Flavors `dev` / `staging` / `prod` × build types `debug` / `release`. Each flavor sets its own
`MAIN_BASE_URL` / `EXTRA_BASE_URL`. Use `staging` for the backend wire-validation run.
