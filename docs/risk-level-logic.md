# Flood Risk Logic

SalbaBida includes a lightweight flood-risk scorer in `FloodRiskScorer`.

## Inputs

- Weather condition text from OpenWeatherMap
- Humidity
- Cloudiness
- Wind speed
- User distance to nearest flood-zone marker
- Manual alert flag from a flood-zone marker note containing `alert`

## Levels

| Score | Level | Meaning |
| --- | --- | --- |
| 0-29 | Low | No immediate flood indicators |
| 30-54 | Moderate | Weather or nearby hazard suggests increased caution |
| 55-74 | High | Conditions justify evacuation readiness |
| 75-100 | Emergency | Severe conditions or manual alert requires urgent action |

## Current Formula

```text
Risk Level =
  weather condition points
  + humidity points
  + cloudiness points
  + wind points
  + flood-zone distance points
  + manual alert points
```

## Recommendations

- Low: monitor updates and keep supplies ready.
- Moderate: check routes and avoid flood-prone roads.
- High: prepare to evacuate and follow official advisories.
- Emergency: move to a safe area or evacuation center immediately if advised.

This is decision-support logic, not an official hazard forecast. Official advisories from PAGASA, NDRRMC, LGUs, and DRRMOs remain authoritative.
