# HKI 7

<img width="1024" height="500" alt="play_feature_graphic_1024x500" src="https://github.com/user-attachments/assets/452d7a45-3aa8-4306-acbd-cbb0722425a9" />

A modern [Home Assistant](https://www.home-assistant.io/) client for Android, built with Jetpack Compose and Material 3.

> **Status:** `1.0.0`. Issues and feedback are welcome.

Connects to your own Home Assistant server and nothing else — no developer servers, no
analytics, no ads, no tracking. Not set up yet? A built-in demo home runs without a server or an
account. Running more than one Home Assistant? Connect to all of them.

## Features

### Dashboards

- **Automatic import** — builds a dashboard from your existing Home Assistant setup, then gets out
  of the way so you can rearrange it
- **Rooms** — per-room device detail and controls, with motion, presence and people counters
- **Design your own** — buttons, button stacks, badges, and custom popups, each with its own
  layout (Standard, Square, Tile, Centered), width, corner radius, icon and background
- **Icon packs** — the complete Material Design Icons and Simple Icons sets, rendered from bundled
  webfonts with no network dependency

### Controls

- **Every domain** — lights, climate, covers, fans, locks, media players, vacuums, humidifiers,
  alarms, cameras and more, each with a dedicated dialog rather than a generic more-info sheet
- **Dedicated screens** — climate, energy, security, vacuum and battery overviews
- **Live camera streams**, and live cleaning maps for [Valetudo](https://valetudo.cloud/) robots
- **Adaptive Lighting** — per-room controls for the
  [adaptive_lighting](https://github.com/basnijholt/adaptive-lighting) integration
- **Global search** across every entity

### Widgets

Weather, calendar, media player, sensor graphs, markdown, parcels, waste collection, iFrame,
shared to-do lists, Find my devices, and Formula 1 (via
[F1 Sensor](https://github.com/Nicxe/f1_sensor)).

### Notifications and events

- **Push notifications** from your own server, including the `actions` buttons the official app
  supports — tapping one fires the same `mobile_app_notification_action` event, so automations
  written for the official app work unchanged
- **Event timeline** — a feed of what the house has been doing, read live from Home Assistant's
  logbook: the front door opening, a light going off, someone arriving home, each with the time
  and who caused it

### Family sharing

Optional, and powered by the companion
[HKI 7 Cloud](https://github.com/jimz011/HKI7-Cloud-Component) integration — which stores
everything on your own Home Assistant, so none of this leaves your home.

- **Shared dashboards** — an admin builds one and publishes it to specific people, or everyone
- **Parental controls** — hide views, rooms or individual entities from particular family members
- **Per-user permissions** for editing, global search, and switching or creating dashboards
- **Room following** — open the room someone is in, from an ESPresense or `mqtt_room` presence
  sensor (nothing in the app talks to MQTT; those publish the room name as an ordinary sensor state)
- **Devices** — which HKI version each family device runs, with an optional household minimum
  that prompts anyone behind to update through Google Play

> Parental controls are UX-level hiding for a friendlier dashboard, **not** a Home Assistant
> security boundary. Home Assistant has no per-entity read permission for non-admin users, so
> anyone in the household can still reach those entities through Home Assistant directly.

### Presence

- **Event-driven location** — geofences and WorkManager rather than continuous tracking, designed
  for battery parity with the official app
- Fully optional; the app works without location access

### Elsewhere

- **Backups** — to your own Google Drive, or to your own Home Assistant
- **13 languages** — English, Dutch, German, French, Spanish (and Latin American Spanish),
  Italian, Portuguese (and Brazilian Portuguese), Turkish, Japanese, Korean, and Chinese
  (Simplified and Traditional)
- **Guided onboarding** — Home Assistant discovery and a quick-start setup flow

## Requirements

- Android 14+ (minSdk 34)
- A reachable Home Assistant instance (local URL, remote URL, or Nabu Casa)
- Family sharing additionally needs [HKI 7 Cloud](https://github.com/jimz011/HKI7-Cloud-Component)
  `0.10.0` or newer, installed on your Home Assistant via HACS

## Building

Open the project in a recent Android Studio and run the `app` configuration, or build from the
command line:

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Contributing

Release notes live in [CHANGELOG.md](CHANGELOG.md) and, for the in-app "What's new" dialog, in the
`cr_whats_new_*` string arrays. New user-facing strings ship in `values/` (English) only —
`tools/localization/` holds the scripts that translate and backfill the other twelve locales, and
`verify_translations.py` is what decides whether a locale is complete.

## License

HKI 7 uses an open-core model: the community source code in this repository is licensed under the
[Mozilla Public License 2.0](https://www.mozilla.org/MPL/2.0/), while separately marked premium
assets remain proprietary. See [LICENSE](LICENSE) for details.

Home Assistant is a trademark of its respective owners; this project is an independent client and
is not affiliated with or endorsed by Home Assistant.

## Other Information

This project was created with help of AI like Claude and OpenAI. If you dislike AI being used in
projects, then do not install this!
