[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20A%20Coffee-Support-yellow.svg)](https://www.buymeacoffee.com/w8Jnf6Hit)
[![PayPal](https://img.shields.io/badge/PayPal-Donate-blue.svg)](https://paypal.me/JimmySchings)

# HKI 7

<img width="1024" height="500" alt="play_feature_graphic_1024x500" src="https://github.com/user-attachments/assets/452d7a45-3aa8-4306-acbd-cbb0722425a9" />

<p align="center">
  <a href="https://play.google.com/store/apps/details?id=com.jimz011apps.hki7">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
         alt="Get it on Google Play"
         height="80">
  </a>

A modern [Home Assistant](https://www.home-assistant.io/) client for Android, built with Jetpack Compose and Material 3.

> **Status:** `1.1.0`. Issues and feedback are welcome.

**📖 [Documentation](https://jimz011.github.io/android-hki7/)** — setup guide, feature reference,
[FAQ](https://jimz011.github.io/android-hki7/faq/) and
[troubleshooting](https://jimz011.github.io/android-hki7/troubleshooting/).

**☁️ [HKI 7 Cloud Component](https://github.com/jimz011/HKI7-Cloud-Component)** — the optional Home
Assistant integration behind family sharing, parental controls and presence.

**💬 [Home Assistant Community thread](https://community.home-assistant.io/t/hki-7-a-highly-customizable-android-companion-download-from-the-play-store-today/1017467)**
— questions, screenshots and feature requests.

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
- **36 languages** — English, Dutch, German (also Austrian and Swiss), French, Spanish (also Latin
  American and Mexican), Italian, Portuguese (and Brazilian), Turkish, Japanese, Korean, Chinese
  (Simplified and Traditional), Norwegian, Swedish, Danish, Finnish, Estonian, Latvian, Lithuanian,
  Polish, Czech, Slovak, Hungarian, Romanian, Bulgarian, Croatian, Greek, Russian, Thai, Arabic and
  Hebrew — with the whole interface mirrored right to left for Arabic and Hebrew
- **Guided onboarding** — Home Assistant discovery and a quick-start setup flow

## Requirements

- Android 12+ (minSdk 31)
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
`tools/localization/` holds the scripts that translate and backfill the other thirty-five locales, and
`verify_translations.py` is what decides whether a locale is complete.

The full development setup, project layout and localization workflow are in the
[contributing guide](https://jimz011.github.io/android-hki7/contributing/).

### Translations

Most of the thirty-six languages started out machine-translated, so they are complete without
always being *right*. [`translations/`](translations/) has one CSV per language — every string in
the app, English beside the translation — so a native speaker can read their whole language in one
file and fix what reads wrong, without touching any Android code.

**🌍 [How to translate](translations/README.md)** ·
[report a wording problem](https://github.com/jimz011/android-hki7/issues/new?template=translation.yml)

## Documentation

The docs site is [MkDocs](https://www.mkdocs.org/) with
[Material for MkDocs](https://squidfunk.github.io/mkdocs-material/); sources live in `docs/` and
`mkdocs.yml`. Pushing to `main` publishes it to GitHub Pages. To work on it locally:

```
pip install -r docs/requirements.txt
mkdocs serve
```

## License

HKI 7 uses an open-core model: the community source code in this repository is licensed under the
[Mozilla Public License 2.0](https://www.mozilla.org/MPL/2.0/), while separately marked premium
assets remain proprietary. See [LICENSE](LICENSE) for details.

Home Assistant is a trademark of its respective owners; this project is an independent client and
is not affiliated with or endorsed by Home Assistant.

## Other Information

This project was created with help of AI like Claude and OpenAI. If you dislike AI being used in
projects, then do not install this!
