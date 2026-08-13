# Settings map

Settings is grouped into four sections. This page lists what is on each screen, in the order the
app shows them.

## Your Home

*Identity and connection.*

### Account

Your profile as this app knows it.

- **Name** and **avatar image**
- **Person entity** — which `person.*` entity is you
- **Birthday**

### Connection

- **Home Assistant instances** — the list of homes, the active one, rename and remove, and
  **Add Home Assistant instance**. Each home keeps its own login, dashboard, notification and
  location settings. Swipe left from the upper-right edge of the dashboard to switch.
- **Active connection** — current status and which route is in use, with **Refresh connection**.
- **Remote access (optional)** — external URL or Nabu Casa URL.
- **Local network (optional)** — internal URL plus home Wi-Fi names, with **Add current network**.
  On those networks the app connects via the internal URL.
- **Connection log** — a live diagnostic of connection attempts, reconnects, sync and
  subscriptions. Last 300 lines, cleared on every app launch, with copy and clear buttons.
- **Maintenance** — **Restart Home Assistant**. Administrative, disruptive, and deliberately the
  last thing on the screen.

[:octicons-arrow-right-24: Connect to Home Assistant](../getting-started/connect.md)

### Location

*Device tracker and geocoded location.*

- **Device tracker** — the master switch
- **Android location permission** — current state: allowed always / while using / not allowed,
  with **Allow all the time**
- **Battery optimisation** — with **Disable battery optimisation**
- **Background usage** — unrestricted is what presence needs
- **Update location now**
- **High accuracy mode** — continuous GPS for live tracking; uses much more battery

[:octicons-arrow-right-24: Presence and location](../guide/presence.md)

## Personalize

*Dashboards, visual style, and everyday navigation.*

### Dashboard

The subtitle shows whether the dashboard is in **Automatic** or **Manual** mode.

- **Dashboards** — create, switch, duplicate, delete
- **Navigation bar** — order and visibility. Home and Rooms are always shown
- **Custom pages** — create a page of your own
- **Media players** — rename players and choose which may appear in the media bar
- **Popups** — create and edit shared custom popups

[:octicons-arrow-right-24: Dashboards](../guide/dashboards.md)

### Appearance

*Theme and navigation bar.*

- **Visual style** — colour, typography and component shape
- **Theme** — mode (System / Light / Dark) and colour (System / Rose / Green / Blue / Amber /
  Custom), plus separate system light and dark theme colours
- **Fonts** — text size, boldness (Thinner −200 → Boldest +300), and font family with a preview
- **Language** — follow the system, or pick one of 14 locales
- **Corners** — Sharp / Modern / Round, applied to every button, card and widget
- **Icons** — animated entity icons, and the four weather-animation switches
- **Header** — full or compact dashboard header
- **Force high refresh rate**

[:octicons-arrow-right-24: Appearance](../guide/appearance.md)

## Services & Data

*Messages, safety and portability.*

### Notifications

*Push delivery and history.*

- An explanation that notifications are delivered over the app's live connection whenever it is
  open — send them from Home Assistant with the notify service for this device, and swipe in from
  the left edge to see the history
- **Background notifications** — keeps a persistent connection while the app is closed; required
  while multiple homes receive notifications
- **Hide Connection Notification** — turns off the "Notification connection" channel; the
  connection keeps working, only its notification disappears
- **Event timeline** — a pointer to where it is configured, since that is set once for the whole
  family rather than per device

[:octicons-arrow-right-24: Notifications](../guide/notifications.md)

### Backup & Restore

*Save or restore dashboard configuration.*

- Local file, Google Drive, and Home Assistant destinations
- Daily automatic Google Drive backup, keeping the newest 14
- Restore from any listed backup

[:octicons-arrow-right-24: Backup and restore](../guide/backup.md)

### Family Sharing

*Parental controls, dashboard sharing and permissions.* Needs the HKI 7 Cloud component, and is
admin-only.

Tabs: **Dashboards**, **Parental controls**, **Permissions**, **Presence**, **Devices**,
**Events**. Each says for itself if it needs a newer component than the one installed.

[:octicons-arrow-right-24: Family sharing](../guide/family-sharing.md)

## HKI 7

*Project information, licensing and community support.*

### About

What HKI 7 is and how it is built.

### License

Open-source and premium licensing.

### Support

Ways to help the project without buying Premium.
