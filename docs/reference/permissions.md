# Android permissions

Every permission HKI 7 declares, why it is there, and what stops working without it.

## Always required

| Permission | Why |
|---|---|
| `INTERNET` | Talking to your Home Assistant. Nothing works without it. |
| `ACCESS_NETWORK_STATE` | Noticing when the network changes so the connection can reconnect rather than sitting there dead. |

## Local network detection

| Permission | Why |
|---|---|
| `ACCESS_WIFI_STATE` | Reading the current Wi-Fi network name, so the app can pick the internal URL when you are on your home network. |
| `CHANGE_WIFI_MULTICAST_STATE` | mDNS discovery of Home Assistant on the local network during setup. |

## Notifications

| Permission | Why |
|---|---|
| `POST_NOTIFICATIONS` | Showing notifications at all. Denying it means Home Assistant's notifications never appear; the rest of the app is unaffected. |
| `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC` | The persistent connection behind **Background notifications** — delivery while the app is closed. |

## Location — all optional

| Permission | Why |
|---|---|
| `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` | Presence: reporting the device tracker and geocoded location to your server. |
| `ACCESS_BACKGROUND_LOCATION` | Geofence transitions while the app is closed. Without it, presence only updates while HKI 7 is open. Android requires this to be granted separately, in system settings. |
| `FOREGROUND_SERVICE_LOCATION` | The service used **only** by High accuracy mode. Normal presence runs no such service. |

Decline all of these and HKI 7 works normally — you simply get no device tracker.

[:octicons-arrow-right-24: Presence and location](../guide/presence.md)

## Background reliability

| Permission | Why |
|---|---|
| `WAKE_LOCK` | Finishing a short background job — a location push, a backup — without the device sleeping mid-way. |
| `RECEIVE_BOOT_COMPLETED` | Re-registering geofences after a reboot, since Android does not preserve them, and restarting the notification connection if it was enabled. |

## Not permissions, but asked for anyway

**Battery optimisation exemption**

: Requested, not required. When the app is battery-optimised, Android may delay or drop the
  periodic refresh that keeps location and battery reporting current.

**Unrestricted background usage**

: Same effect from the other direction — a *restricted* app has its background work stopped
  outright.

**Google Drive (`drive.appdata`)**

: Only if you turn on Drive backups. It grants access to the app's own private folder in your
  Drive and nothing else.

## What HKI 7 does not ask for

No contacts, no camera, no microphone, no storage-wide access, no phone state, no advertising id,
no accounts beyond the Google sign-in you explicitly initiate for Drive backups.

[:octicons-arrow-right-24: Privacy policy](privacy.md)
