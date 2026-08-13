# Presence and location

Presence is **entirely optional**. HKI 7 works normally with location switched off — you simply do
not get a device tracker.

## How it works

HKI 7 uses the same battery-friendly model as the official Home Assistant companion app:

**Geofences do the watching.**

: HKI 7 fetches your Home Assistant **zones** and registers a geofence around each one. The
  *operating system* watches for boundary crossings, not the app, so the radio only wakes on a
  transition. When you cross one, a single fresh location is pushed to Home Assistant. Passive
  zones are skipped — those are for automations, not presence.

**WorkManager keeps things fresh.**

: A periodic background job refreshes battery and location and re-registers the geofences, which
  do not survive a reboot. Android batches this work into its Doze maintenance windows rather than
  keeping the process awake.

The result is that presence updates *instantly* on a zone crossing while the app is doing nothing
at all in between.

!!! info "No persistent service in normal mode"

    Normal presence uses **no** foreground service. The only thing that runs one is High accuracy
    mode, below — and background notifications, which is a separate feature.

## High accuracy mode

**Settings › Location › High accuracy mode** switches to continuous GPS for live tracking. It runs
a foreground service and uses **much** more battery. Turn it on when you specifically want to
watch a device move on a map; turn it off again afterwards.

## What HKI 7 reports

Telemetry goes to Home Assistant through the **mobile_app webhook**, which needs no access token
once the device is registered. That creates persistent entities that survive Home Assistant
restarts:

| Entity | Type |
|---|---|
| `device_tracker.<device>` | Location and zone membership |
| `<device> Battery Level` | `sensor`, device class `battery`, diagnostic |
| `<device> Charging` | `binary_sensor`, device class `battery_charging`, diagnostic |
| `<device> Geocoded Location` | `sensor` — the current address |

All of it goes to **your own server** and nowhere else.

## Permissions

Android splits this into steps, and HKI 7 shows the current state of each under
**Settings › Location** with a button straight to the relevant system screen.

**Location permission**

: *Allowed all the time* is what background presence needs. *While using the app* means presence
  stops when you close HKI 7. Android deliberately makes "all the time" a separate, second
  decision made in system settings — the app cannot grant it for you.

**Battery optimisation**

: Should be **unrestricted**. Optimised means Android may delay or skip the periodic refresh.

**Background usage**

: Should be **unrestricted**. Restricted means the same thing from a different angle: Android
  stops the background job from running.

The Location screen warns about each of these when they are not set the way presence needs.

## Manual update

**Update location now** pushes a fresh fix immediately. Useful for testing an automation, or for
confirming the connection works at all.

## Turning it off

Revoke the location permission in Android settings, or turn the device tracker off under
**Settings › Location**. Collection stops immediately. The entities already created in Home
Assistant remain until you remove them there.
