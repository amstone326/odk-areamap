# odk-areamap

Android prototype that traces a land perimeter on Google Maps and returns the area to ODK / CommCare via `odk_intent_data`.

Two ways to capture a polygon:

1. **Walk Mode** — GPS (high accuracy, ~5s) drops vertices as you walk; points closer than 0.5 m are ignored.
2. **Manual Entry** — tap the map to place vertices.

**Connect Points** (3+ vertices) fans the polygon from the first point and shows area in m². **Use This Area** sends that number back and finishes.

Area uses haversine distances and the planar formula \(\frac{1}{2}ab\sin C\) (sides assumed tiny vs. Earth).

## Stack

Eclipse ADT / Ant (not Gradle), package `org.commcare.areamap`, min SDK 11 / Google APIs 19. Maps v2 and the old `LocationClient` Play Services API. Manifest package and launcher label are still `com.example.myapp` / **MyApp**.

## Limitations

- Manual Entry is broken (`setOnMapClickListener` is commented out).
- Play Services APIs (`LocationClient`) are obsolete.
- Maps API key is in plaintext in `res/values/strings.xml`.
- Library paths are machine-specific (`adt-bundle-mac-x86_64-20140321`); will not build as-is.
- No tests, no Gradle build.
