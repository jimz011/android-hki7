# Backup and restore

**Settings › Backup & Restore** saves and restores your dashboard configuration. Everything goes
to a destination **you** control — a file, your own Google Drive, or your own Home Assistant.

## What a backup contains

A backup is a single JSON file holding the way you have arranged the app:

- Rooms, floors and their order, and every room's widgets and configuration.
- Page configurations, custom pages and custom popups.
- Navigation bar order and hidden entries.
- Theme mode and colour, including the custom light and dark schemes.
- Fonts — scale, weight adjustment, family.
- Corner radius, icon pack, icon animation settings, high refresh rate.
- Header configuration: weather, sun, moon, AQI, season and rain entities, clock format, alarm
  entities.
- Media player names and which are hidden from the media bar.

**A backup does not contain credentials.** No tokens, no server URLs, no passwords. Restoring one
onto a fresh install gives you your dashboard back, and you sign in separately.

## Backup names

All three destinations share one naming scheme, so a file is identifiable wherever it ends up:

```
HKI7-1.0.0-2026-08-13_21-04-17.json
```

That is `HKI7-<app version>-<date>_<time>.json`, in your local time zone.

## Destinations

=== "Local file"

    Writes a JSON file through Android's file picker — to the device, to an SD card, or to any
    cloud storage provider the picker can see. Restoring reads one back the same way.

    This is the destination that needs nothing set up, and the one to use when moving to a new
    phone.

=== "Google Drive"

    Backs up to **your own** Google Drive, in the app's private `drive.appdata` area. That area is
    accessible only to HKI 7 and to you — not to the developer, and not to other apps.

    Once enabled, a backup runs **daily** in the background, and the newest **14** are kept; older
    ones are pruned automatically. You can also back up on demand, and restore any of the
    retained backups from a list.

    Disable it at any time, and remove the app's access entirely from your Google account
    settings.

=== "Home Assistant"

    Stores the same backup on your own Home Assistant, through the
    [HKI 7 Cloud](family-sharing.md) component. Nothing leaves your house at all.

    This is an **addition** to Google Drive, never a replacement — both can be enabled at the
    same time, which is a reasonable thing to do: Drive survives your server dying, and Home
    Assistant survives you losing the Google account.

## Restoring

Restore from **Settings › Backup & Restore**, or during onboarding on a fresh install — see
[Your first dashboard](../getting-started/first-dashboard.md#restore-a-backup).

Restoring replaces your current dashboard configuration with the backup's. Connections and
profiles are not touched.

??? question "Backup could not be read"

    The file is not an HKI 7 backup, or it is truncated. Check that you picked a
    `HKI7-*.json` file and that it downloaded fully. If it came from Google Drive's web
    interface rather than from the app, make sure it was not saved as a shortcut.

## Sharing a dashboard with family

Backups are a fine way to move *your own* setup between devices. They are the wrong tool for
giving your household a dashboard — that is what [family sharing](family-sharing.md) is for. An
admin publishes once and everyone pulls it, instead of passing files around and re-restoring after
every change.
