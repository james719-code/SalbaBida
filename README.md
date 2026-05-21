# SalbaBida

Offline-capable flood preparedness and evacuation support app for Philippine communities.

SalbaBida is a Kotlin Android app for flood readiness, weather monitoring, evacuation mapping, safety tools, and Filipino disaster-preparedness content. It combines OpenWeatherMap weather data, OSMDroid maps, Firebase, Room, DataStore, WorkManager, and Material 3.

## Status

- Current app version: `2.0.0`
- Release candidate notes: [docs/release-v1.0.0.md](docs/release-v1.0.0.md)
- CI: Android debug build and unit tests run through GitHub Actions
- Test focus: flood risk scoring, offline marker defaults, weather cache behavior

## Core Features

### Weather Monitoring

- Real-time weather data from OpenWeatherMap
- 12-hour weather cache through Room
- Temperature, humidity, wind, pressure, visibility, and cloudiness
- Pull-to-refresh weather updates
- Flood risk scoring from weather, nearby flood zones, and manual alert notes

### Evacuation Map

- OSMDroid map centered on user location or selected city
- Firebase evacuation centers
- Offline markers stored locally
- Background sync when connectivity returns
- Marker categories:
  - Evacuation Center
  - Flood Zone
  - Safe Area
  - Resource Center

### Offline Support

- Weather cache remains available for 12 hours
- Preparedness tips are bundled with the app
- Offline markers are saved in Room
- WorkManager sync retries pending markers when a connection is available

### Preparedness and Safety

- Filipino preparedness tips for before, during, and after floods
- Safety tools and emergency hotline access
- Firebase Authentication with email/password
- DataStore-backed settings and onboarding preferences

## Disaster Data

The disaster-data model is documented in:

- [docs/disaster-data.md](docs/disaster-data.md)
- [docs/firebase-schema.md](docs/firebase-schema.md)
- [docs/evacuation-center-sample.json](docs/evacuation-center-sample.json)
- [docs/risk-level-logic.md](docs/risk-level-logic.md)
- [docs/offline-sync.md](docs/offline-sync.md)

These documents define marker categories, Firestore fields, sample evacuation-center records, flood-risk scoring, and offline sync behavior.

## Tech Stack

- Kotlin
- Jetpack Compose and Material 3
- MVVM with repository pattern
- Hilt dependency injection
- Room
- DataStore
- Retrofit and OkHttp
- Firebase Auth and Firestore
- OSMDroid
- Google Play Services Location
- WorkManager

## Requirements

- Android SDK 24+
- Target SDK 35
- JDK 17
- Google Play Services for location features
- Firebase project for authentication and Firestore
- OpenWeatherMap API key

## Configuration

### Firebase

1. Create a Firebase project.
2. Add an Android app using package name `com.project.salbabida`.
3. Download `google-services.json`.
4. Place it at `app/google-services.json`.
5. Enable Email/Password authentication.
6. Create the Firestore collections documented in [docs/firebase-schema.md](docs/firebase-schema.md).

### Weather API

Add your OpenWeatherMap key to `local.properties`:

```properties
OPENWEATHER_API_KEY=your_api_key_here
```

## Build and Test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Install a debug APK:

```bash
./gradlew installDebug
```

## Project Structure

```text
app/src/main/java/com/project/salbabida/
├── data/
│   ├── api/             # Retrofit services and API client
│   ├── database/        # Room entities, DAOs, and database
│   ├── model/           # Weather and location models
│   ├── preferences/     # DataStore user preferences
│   ├── repository/      # Weather and map repositories
│   ├── risk/            # Flood risk scoring
│   └── sync/            # WorkManager offline-marker sync
├── di/                  # Hilt modules
├── navigation/          # Navigation routes and graph
├── ui/                  # Compose screens and theme
└── SalbaBidaApplication.kt
```

## Testing

Current unit-test coverage includes:

- Flood risk scoring for low, moderate, and emergency scenarios
- Offline marker default sync status

Recommended next tests:

- Weather repository cache behavior
- Marker repository offline saves
- Sync worker retry behavior
- Authentication routing
- ViewModel loading, success, and error states

## Permissions

- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `INTERNET`
- `ACCESS_NETWORK_STATE`

## Developer

James Ryan S. Gallego

## License

All Rights Reserved 2026
