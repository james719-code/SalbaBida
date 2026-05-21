# Firebase Schema

## `evacuation_centers`

Official or reviewed evacuation-center records.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `name` | string | Yes | Public display name |
| `place` | GeoPoint | Yes | Latitude and longitude |
| `address` | string | Recommended | Street, barangay, city |
| `barangay` | string | Recommended | Barangay name |
| `city` | string | Recommended | City or municipality |
| `province` | string | Recommended | Province |
| `capacity` | number | Optional | Estimated person capacity |
| `manager` | string | Optional | LGU, school, DRRMO, or NGO owner |
| `contact` | string | Optional | Hotline or office number |
| `status` | string | Optional | `open`, `standby`, `full`, `closed` |
| `updatedAt` | timestamp | Recommended | Last official update |

## `markers`

Offline-capable community map markers.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `name` | string | Yes | Marker title |
| `place` | GeoPoint | Yes | Latitude and longitude |
| `category` | string | Yes | `EVACUATION_CENTER`, `FLOOD_ZONE`, `SAFE_AREA`, `RESOURCE_CENTER` |
| `notes` | string | Optional | Field notes, alert context, contact info |
| `createdAt` | number | Yes | Local timestamp in milliseconds |
| `localId` | string | Yes | Room marker ID used for sync reconciliation |

## `users`

User profile and onboarding data.

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `email` | string | Yes | Firebase Auth email |
| `role` | string | Recommended | `resident`, `responder`, `admin` |
| `barangay` | string | Optional | Home barangay |
| `city` | string | Optional | Home city |
| `province` | string | Optional | Home province |
| `acceptedTermsAt` | timestamp | Recommended | Terms acceptance proof |
