# odk-areamap

A small Android prototype for tracing a land perimeter and sending the computed area back to ODK / CommCare.

## What it does

The app is a Google Maps screen with two ways to define a polygon:

1. **Walk Mode** — GPS updates (high accuracy, about every 5 seconds) drop points as you walk. Points closer than 0.5 meters to the last one are ignored.
2. **Manual Entry** — intended to let you tap the map to place vertices.

Once there are at least three points, **Connect Points** closes the polygon, triangulates from the first vertex, and shows the area in square meters. **Use This Area** returns that number to the calling ODK app via the extra `odk_intent_data` and then finishes.

## Stack

Eclipse ADT / Ant Android project (not Gradle):

- Package: `org.commcare.areamap`
- Eclipse project name: AreaMap
- Manifest package is still `com.example.myapp`; the launcher label is still **MyApp**
- min SDK 11, target Google APIs 19 (KitKat)
- Google Maps v2 and the old `LocationClient` Play Services API
- Android Support v4 / v7 AppCompat (referenced from hardcoded local Mac ADT SDK paths)

Main classes:

- `MainActivity` — map UI, location updates, and the `idle` → `walking` / `manualEntry` → `finished` state machine
- `Utilities` — LatLng conversion and haversine distance
- `SphericalTriangle` — triangle area for the fan triangulation
- `ErrorDialogFragment` — Play Services error dialog wrapper

## Area math

`Utilities` uses the haversine formula for distance. Despite the name, `SphericalTriangle` uses the planar formula \( \frac{1}{2}ab\sin C \), on the assumption that the sides are tiny compared with Earth’s radius. The polygon is fanned into triangles from the first point.

## Known limitations

This is an early prototype, not a finished product:

- **Manual Entry is likely broken** — `mMap.setOnMapClickListener(this)` is commented out in `onCreate`.
- **Play Services APIs are obsolete** — `LocationClient` / `GooglePlayServicesClient` were replaced years ago.
- **A Google Maps API key is stored in plaintext** in `res/values/strings.xml`.
- **Library paths are machine-specific** (`adt-bundle-mac-x86_64-20140321`), so the project will not build as-is on another machine.
- There are no tests and no Gradle build.
