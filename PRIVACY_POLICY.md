# HKI 7 Privacy Policy

**Last updated: July 23, 2026**

HKI 7 ("the app") is an Android client for [Home Assistant](https://www.home-assistant.io/), a home automation platform that you host yourself. This policy explains what data the app handles and where it goes.

**The short version: your data goes to *your own* Home Assistant server and stays on your device. The app has no developer-operated servers, no analytics, no advertising, and no tracking. The developer never receives, sees, or stores any of your data.**

## Data the app handles

### Connection details and credentials

To connect to your Home Assistant instance, the app stores the server URL(s) and an access token you provide. These are stored locally on your device and are only ever sent to the Home Assistant server *you* configured, in order to authenticate with it.

### Location (including background location)

If you enable presence detection, the app collects your device's location — including in the background via geofencing — and reports it **only to your own Home Assistant server**, so your home automations can respond to you arriving or leaving. Specifically:

- Location is used for geofence-based presence (home/away) and, if enabled, periodic location updates to your Home Assistant instance.
- Location data is **never** sent to the developer or to any third party.
- Presence detection is optional. You can decline the location permissions or disable the feature in settings, and the rest of the app works normally.
- Revoking location permission in Android system settings stops all location collection immediately.

### Device sensors and telemetry

If you enable sensor reporting, the app reads device state such as battery level, charging state, network/Wi-Fi state, and similar device sensors, and reports them **only to your own Home Assistant server** (mirroring what the official Home Assistant companion app does). This is optional and configurable in settings.

### Home Assistant data (entities, cameras, media)

Dashboards, entity states, history, camera streams, and media are fetched **from your own Home Assistant server** for display on your device. Camera streams are viewed live and are not recorded or stored by the app.

### Notifications

Notifications are delivered directly from your own Home Assistant server to the app over your configured connection. No third-party push service operated by the developer is involved.

### App settings and backups

Your app settings (dashboards, preferences, connection configuration) are stored locally on your device.

Optionally, you can enable **cloud backup to Google Drive**. If you do, the app asks for Google's `drive.appdata` permission and stores a backup of your app configuration in the private app-data area of **your own Google Drive account**. This data is accessible only to the app and to you; the developer has no access to it. Google's handling of your Drive data is governed by the [Google Privacy Policy](https://policies.google.com/privacy). You can disable backups and delete them at any time from the app's settings, or by removing the app's access in your Google account settings.

## What the app does NOT do

- No data is sent to the developer — there are no developer-operated servers.
- No analytics, crash-reporting, advertising, or tracking SDKs.
- No sale or sharing of personal data with third parties.
- No collection of contacts, messages, photos, or files.

## Third-party services

The app uses **Google Play services** on your device for location (fused location / geofencing) and, if you enable cloud backup, for **Google Drive** authorization. These services are provided by Google and are subject to the [Google Privacy Policy](https://policies.google.com/privacy). Your self-hosted Home Assistant server is operated by you (or your server's administrator) and is outside the scope of this policy.

## Data retention and deletion

All app data lives on your device, on your Home Assistant server, or (if you enabled backups) in your own Google Drive. To delete it:

- **On device:** clear the app's data or uninstall the app.
- **On your Home Assistant server:** remove the device/entities from your Home Assistant instance.
- **Google Drive backups:** delete them from the app's settings, or revoke the app's access at [myaccount.google.com/permissions](https://myaccount.google.com/permissions).

## Children's privacy

The app is not directed at children under 13 and does not knowingly collect personal information from children.

## Changes to this policy

Changes to this policy will be published at this same location, with the "Last updated" date revised. Significant changes will be noted in the app's release notes.

## Contact

If you have questions about this policy or your data, open an issue at [github.com/jimz011/android-hki7](https://github.com/jimz011/android-hki7/issues) or contact the developer at **jimz011apps@gmail.com**.
