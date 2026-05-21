# SalbaBida v1.0.0 Release Notes

## Release Summary

SalbaBida v1.0.0 is the first public milestone for the flood-preparedness Android app. It provides weather monitoring, evacuation mapping, offline marker support, flood preparedness content, authentication, and background marker sync.

## APK

The debug APK is produced by:

```bash
./gradlew assembleDebug
```

GitHub Actions uploads the debug APK as the `salbabida-debug-apk` artifact after each successful CI build.

## Included Features

- Weather monitoring through OpenWeatherMap
- 12-hour weather caching
- Flood risk scoring
- OSMDroid evacuation map
- Firebase evacuation-center markers
- Offline marker creation and editing
- WorkManager marker sync
- Firebase Authentication
- Filipino preparedness tips
- Material 3 UI with dark theme support

## Known Limitations

- Official evacuation-center data must be configured in Firestore.
- Weather requires `OPENWEATHER_API_KEY` in `local.properties`.
- Firebase requires `app/google-services.json`.
- Risk scoring is a local decision-support aid and should not replace official advisories.
- Screenshots are intentionally not included yet.

## Setup

1. Configure Firebase using `app/google-services.json`.
2. Add `OPENWEATHER_API_KEY` to `local.properties`.
3. Run `./gradlew assembleDebug`.
4. Install the APK on a device or emulator.

## Verification

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```
