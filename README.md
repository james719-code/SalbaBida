# SALBA-bida

A flood disaster management Android application built with Kotlin and Jetpack Compose.

## Overview

SALBA-bida is designed to help communities in the Philippines prepare for, respond to, and recover from flood events. The application provides real-time weather updates, evacuation center mapping, and comprehensive flood preparedness information.

## Features

### Location Services
- GPS-based location detection during onboarding
- Location permission request with user-friendly prompts
- Update location option in Settings for users who move
- Automatic map centering based on user location

### Weather Monitoring
- Real-time weather data from OpenWeatherMap API
- 12-hour automatic caching with manual refresh option
- Temperature, humidity, wind speed, and visibility display
- Location-based weather updates

### Evacuation Map
- Interactive OSMDroid map centered on user location
- Evacuation center markers fetched from Firebase
- Home location tracking with distance calculations
- Nearest evacuation center display with distance in kilometers
- Offline map marker addition stored locally
- Marker categories: Evacuation Center, Flood Zone, Safe Area, Resource Center
- Edit and delete individual markers
- Delete all markers option in Settings for data refresh
- Cohesive blue-themed UI optimized for light and dark modes
- Background synchronization when connectivity returns

### Flood Preparedness
- Categorized tips: Before, During, and After flood events
- Searchable tip database
- Expandable cards for detailed information
- Content in Filipino for local accessibility

### Theming and Customization
- Dark theme support
- Dynamic colors support (Android 12+)
- Material 3 design system

### Authentication
- Firebase Authentication with email/password
- Secure user management
- Terms and conditions acceptance tracking

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Repository pattern
- **Local Database**: Room
- **Cloud Database**: Firebase Firestore
- **Networking**: Retrofit with OkHttp
- **Maps**: OSMDroid with Mapnik tiles
- **Location**: Google Play Services Location
- **Authentication**: Firebase Auth
- **Preferences**: DataStore
- **Background Work**: WorkManager

## Requirements

- Android SDK 24+ (Android 7.0 Nougat)
- Target SDK 35 (Android 15)
- Google Play Services for location features
- Internet connection for weather updates and sync
- Location permission for map and weather features

## Project Structure

```
app/src/main/java/com/project/salbabida/
├── data/
│   ├── api/             # Retrofit services and API client
│   ├── database/        # Room entities, DAOs, and database
│   ├── model/           # Data models
│   └── preferences/     # DataStore user preferences
├── navigation/          # Navigation routes and graph
├── ui/
│   ├── screens/
│   │   ├── auth/        # Login, SignUp, LocationSelection
│   │   ├── home/        # Weather display
│   │   ├── map/         # OSMDroid map with evacuation markers
│   │   ├── preparedness/# Flood preparedness tips
│   │   ├── settings/    # App settings with location update
│   │   └── about/       # About screen
│   └── theme/           # Material 3 theming
└── SalbaBidaApplication.kt
```

## Building the Project

1. Clone the repository
2. Open in Android Studio (Ladybug or newer recommended)
3. Sync Gradle files
4. Add your Firebase configuration
5. Build and run on device or emulator

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Configuration

### Firebase Setup
1. Create a Firebase project at console.firebase.google.com
2. Add Android app with package name `com.project.salbabida`
3. Download `google-services.json` and place in `app/` directory
4. Enable Authentication with Email/Password provider
5. Create Firestore database with collections:
   - `evacuation_centers` - Evacuation center locations
   - `users` - User data

### Weather API
The app uses OpenWeatherMap API for weather data. Configure the API key in the weather API service.

## Permissions

The app requires the following permissions:
- `ACCESS_FINE_LOCATION` - Precise location for map centering
- `ACCESS_COARSE_LOCATION` - Approximate location fallback
- `INTERNET` - Weather data and Firebase sync
- `ACCESS_NETWORK_STATE` - Network availability checking

## Offline Support

The app supports comprehensive offline functionality:
- Weather data cached locally for 12 hours
- Map tiles cached for offline viewing
- Custom markers can be added offline and sync when connected
- Flood preparedness tips available offline
- User preferences stored locally

## Developer

**James Ryan S. Gallego**

## License

All Rights Reserved 2026
