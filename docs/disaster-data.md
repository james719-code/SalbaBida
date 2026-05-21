# Disaster Data Model

SalbaBida treats disaster information as operational map data that can be stored locally and synced online.

## Marker Categories

| Category | Purpose | Typical owner |
| --- | --- | --- |
| Evacuation Center | Official shelter or temporary evacuation site | LGU, barangay, school, DRRMO |
| Flood Zone | Known flood-prone area, river overflow point, or road section | LGU, DRRMO, barangay |
| Safe Area | Elevated or safer gathering point | Barangay, community responders |
| Resource Center | Relief goods, first aid, charging, water, or logistics point | LGU, NGO, community group |

## Recommended Data Sources

- Barangay DRRM committee lists
- City or municipal DRRMO evacuation-center records
- School or public-building shelter assignments
- Historical flood-prone area reports
- Community-verified field reports reviewed by an admin

## Validation Rules

- Every marker needs a clear name, category, latitude, and longitude.
- Official evacuation centers should include capacity and managing agency when available.
- Flood zones should include a basis such as historical flooding, drainage overflow, or official advisory.
- Community reports should remain editable until verified by an admin or barangay responder.

## Review Flow

1. User or admin adds a marker.
2. Marker is saved locally with `PENDING` sync status.
3. WorkManager uploads the marker when the network is available.
4. Admin reviews category, location, and notes.
5. Verified records can be promoted into official evacuation-center or hazard datasets.
