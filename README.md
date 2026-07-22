# HKI 7

A modern [Home Assistant](https://www.home-assistant.io/) client for Android, built with Jetpack Compose and Material 3.

> **Status:** beta (`1.0.0-beta.1`). Expect rough edges — issues and feedback are welcome.

## Features

- **Dashboards & rooms** — automatic dashboard import from your Home Assistant setup, room views with per-room device detail
- **Rich entity controls** — lights, climate, covers, fans, locks, media players, vacuums, humidifiers, alarms, cameras, and more, each with a dedicated dialog UI
- **Dedicated screens** — climate, energy, security, vacuum, and battery overviews
- **Widgets** — weather, calendar, media player, sensor graphs, markdown, parcels, and waste collection
- **Battery-friendly presence** — event-driven location (geofences + WorkManager) instead of continuous tracking, designed for battery parity with the official app
- **Full Material Design Icons support** — the complete MDI set rendered via a bundled icon font
- **Guided onboarding** — Home Assistant discovery and quick-start setup flow

## Requirements

- Android 14+ (minSdk 34)
- A reachable Home Assistant instance (local URL, remote URL, or Nabu Casa)

## Building

Open the project in a recent Android Studio and run the `app` configuration, or build from the command line:

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## License

HKI 7 uses an open-core model: the community source code in this repository is licensed under the [Mozilla Public License 2.0](https://www.mozilla.org/MPL/2.0/), while separately marked premium assets remain proprietary. See [LICENSE](LICENSE) for details.

Home Assistant is a trademark of its respective owners; this project is an independent client and is not affiliated with or endorsed by Home Assistant.
