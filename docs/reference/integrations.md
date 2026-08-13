# Integrations

HKI 7 works with a stock Home Assistant out of the box. These integrations are ones it knows how
to read *specifically*, unlocking a feature that would otherwise not exist.

None of them are required.

## Companion component

### HKI 7 Cloud

[jimz011/HKI7-Cloud-Component](https://github.com/jimz011/HKI7-Cloud-Component) · install via HACS

Unlocks everything under **Family sharing**: shared dashboards, parental controls, per-user
permissions, room following, family device management, the event timeline, and Home
Assistant-hosted backups. It stores that state on your own server.

Version `0.10.0` or newer covers every family feature the current app has.

[:octicons-arrow-right-24: Family sharing](../guide/family-sharing.md)

## Devices and control

### Adaptive Lighting

[basnijholt/adaptive-lighting](https://github.com/basnijholt/adaptive-lighting)

Per-room Adaptive Lighting controls as a dashboard stack, in a full or compact two-row layout.
Auto-generated room widgets are scoped to their own room so they cannot operate another area's
profile.

### Valetudo

[valetudo.cloud](https://valetudo.cloud/)

Live cleaning maps for cloud-free robot vacuums, including per-segment cleaning. HKI 7 decodes the
map directly; no extra Home Assistant component is needed beyond whatever exposes the robot.

### Battery Notes

[andrew-codechimp/HA-Battery-Notes](https://github.com/andrew-codechimp/HA-Battery-Notes)

Battery types and replacement dates on the Battery screen and the battery card widget.

## Presence

### ESPresense / mqtt_room

[espresense.com](https://espresense.com/) · Home Assistant's `mqtt_room`

Room-level presence: which room a person is in, feeding the per-room people counters and
[room following](../guide/family-sharing.md#room-following).

!!! note "No MQTT in the app"

    HKI 7 never talks to MQTT. These integrations publish the room name as an ordinary sensor
    state, and HKI 7 reads that sensor like any other.

## Widgets

### F1 Sensor

[Nicxe/f1_sensor](https://github.com/Nicxe/f1_sensor)

The Formula 1 widget: next race, calendar, standings, starting grid, results and live timing.
Standings can include a championship prediction when F1 Sensor's optional F1TV Auth is set up.

The widget finds its sensors through the entity registry by platform and translation key, so
renaming or localising the entities does not break it.

### Parcel integrations

`ha-parcel-integrations`

The Parcels widget. Supported carriers:

| Carrier | Manual `track_parcel` |
|---|---|
| PostNL | — |
| DHL | — |
| DPD | — |
| GLS | Yes (accepts postal code) |
| Dragonfly | Yes |
| Cainiao | Yes |
| Correos | Yes |
| Packeta | Yes |
| Hermes | Yes |
| Trunkrs | Yes (accepts postal code) |
| Vinted Go | — |
| Parcels (aggregator) | — |

Account-based carriers only track what is already in the account, which is why they have no
manual add-by-tracking-number.

### Afvalbeheer

[pippyn/Home-Assistant-Sensor-Afvalbeheer](https://github.com/pippyn/Home-Assistant-Sensor-Afvalbeheer)

The Waste collection widget. Anything that exposes waste-type sensors with the next pickup date in
their state or attributes works the same way.

## Core Home Assistant features HKI 7 uses

These are not integrations you install; they are parts of Home Assistant the app leans on.

**Areas and floors**

: Become rooms and their groupings. Auto-generation reads them directly — so **set your areas up
  in Home Assistant before importing**, or there will be no rooms to build.
  [:octicons-arrow-right-24: Rooms](../guide/rooms.md)

**Device classes**

: Drive entity discovery on the Climate, Security and Battery screens, and the phrasing on the
  event timeline.

**Zones**

: Become geofences for presence.

**Logbook**

: Feeds the event timeline, over a `logbook/event_stream` subscription.

**Recorder**

: Feeds entity history and the sensor graph widget.

**Energy dashboard preferences**

: Can be imported so the Energy screen does not need every sensor named again. **Configure Home
  Assistant's Energy dashboard first** and this is a one-step setup instead of a long one.
  [:octicons-arrow-right-24: Energy](../guide/controls.md#energy)

**mobile_app**

: Device registration, the `notify.mobile_app_<device>` service, the
  `mobile_app_notification_action` event, and the webhook that carries device tracker and battery
  telemetry.
