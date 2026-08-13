# Troubleshooting

## Start here: the connection log

**Settings › Connection › Connection log** shows what the app's connection has actually been
doing — attempts, reconnects, sync, and subscriptions. It holds the last 300 lines and starts
empty every launch.

If something is wrong and you do not know why, reproduce it and then read the log. It is also the
right thing to attach to a bug report; there is a copy button.

!!! note

    The log can name entities and Home Assistant addresses. Have a look before pasting it into a
    public issue.

---

## Connecting

### Discovery finds nothing

- The phone must be on the **same network** as Home Assistant — not mobile data, not a guest
  VLAN, not a separate IoT SSID.
- Some routers block mDNS between wireless clients ("AP isolation", "client isolation"). Turn it
  off, or skip discovery.
- Choose **Enter an address manually** and type the URL. This always works when discovery does
  not.

### "Can't reach the server"

1. Open the same URL in the phone's browser. If the browser cannot reach it either, the problem is
   the network or the server, not HKI 7.
2. Check the **scheme and port**: `http://192.168.1.20:8123`, not `192.168.1.20`.
3. If you are using `https://` with a self-signed or private-CA certificate, Android must trust
   that certificate. Install the CA in Android's user credential store.
4. If Home Assistant is behind a reverse proxy, make sure it allows **WebSocket upgrades**. HKI 7
   uses the websocket API for everything live, so a proxy that passes plain HTTP but drops the
   upgrade produces a server that "loads" and then never updates.
5. Check Home Assistant's `http:` configuration — `use_x_forwarded_for` and `trusted_proxies` need
   to match your proxy, or Home Assistant will reject the connection.

### Login completes but returns "no authorization code"

The login page finished but the redirect back to the app was blocked. Try again; if it keeps
happening, set your default browser to a mainstream one (some minimal browsers do not handle the
redirect scheme).

### It connects at home but not away — or the reverse

You have one URL configured, and it only works from one place. Set **both**:

- **Remote access** — external URL or Nabu Casa URL
- **Local network** — internal URL, plus your home Wi-Fi names

[:octicons-arrow-right-24: Local and remote access](getting-started/connect.md#local-and-remote-access)

### It keeps reconnecting

Check the connection log for a repeating pattern. Common causes:

- A reverse proxy or firewall with a short idle timeout closing the websocket.
- A VPN on the phone routing traffic away from the LAN.
- Aggressive battery management killing the app in the background — see
  [Background work](#background-work-gets-killed).

---

## Notifications

### Notifications never arrive

Work down this list:

1. **Is the Android permission granted?** Check **Settings › Notifications** in the app, and
   Android's own app notification settings.
2. **Are you calling the right service?** It is `notify.mobile_app_<device_name>`, using the name
   you gave the device during setup. Check Home Assistant's Developer Tools → Actions for the
   exact name.
3. **Test with the app open.** If it arrives with the app open but not when closed, you need
   **Background notifications** — that is the switch that keeps a connection alive while HKI 7 is
   closed.
4. **Check the channel.** If your payload sets `data.channel`, that channel may be muted in
   Android settings.
5. **Check battery restrictions.** A restricted app has its background connection killed. See
   [below](#background-work-gets-killed).

### Notifications arrive from one home but not another

Background notifications are **required** once more than one home is set to receive them. Each
home's notification setting is under **Settings › Connection**.

### Action buttons do nothing

The event fires as `mobile_app_notification_action`. Check your automation is listening for that
event type and matching on the `action` value you sent — not on the button's `title`.

Two names are reserved and never fire an event: `URI` opens its `uri`, and `REPLY` (or
`behavior: textInput`) opens a text input.

### The connection notification annoys me

Turn off the **"Notification connection"** channel in Android settings. The connection keeps
working; only the notification disappears. There is a button for this in
**Settings › Notifications**.

---

## Presence

### The device tracker never updates

1. **Location permission must be "Allow all the time"** for background presence. Android makes
   this a separate decision in system settings — the app cannot grant it. **Settings › Location**
   shows the current state and links there.
2. **Battery optimisation must be unrestricted.** Optimised means Android delays or skips the
   periodic refresh.
3. **Background usage must be unrestricted.** Restricted stops the background job outright.
4. **Do you have zones?** Presence is geofence-driven. No `zone.*` entities means nothing to cross.
   Passive zones are deliberately skipped — those are for automations.
5. Try **Update location now**. If a manual push works but automatic ones do not, it is one of the
   three restrictions above.

### Presence stopped working after a reboot

Geofences do not survive a reboot; HKI 7 re-registers them on boot and periodically after that.
Open the app once to force it, and check that the app is not being blocked from starting at boot
by a manufacturer's autostart manager.

### Presence is accurate but slow

That is the design: geofence transitions, not continuous tracking. If you need live movement, turn
on **High accuracy mode** — and turn it off again, because it costs real battery.

---

## Background work gets killed

Some manufacturers — Samsung, Xiaomi, OnePlus, Huawei, Oppo and others — add battery management
well beyond stock Android that kills backgrounded apps regardless of the permissions you granted.
This affects background notifications, presence, and scheduled backups.

The fixes are all in Android's own settings, and vary by manufacturer:

- Set the app's battery usage to **Unrestricted**.
- Disable "put unused apps to sleep" / "deep sleeping apps" for HKI 7.
- Add HKI 7 to the autostart allow-list where one exists.
- Lock the app in the recents screen, on devices that offer that.

[dontkillmyapp.com](https://dontkillmyapp.com/) documents the exact steps per manufacturer.

---

## Gestures

### A swipe does nothing

- **Edge swipes must start at the edge**, in the *upper* part of the screen — notifications from
  the upper-left, switch-homes from the upper-right.
- **Android's back gesture uses the same edges.** With gesture navigation on, starting too low
  triggers the system back instead. Start higher.
- **Horizontal controls keep the gesture.** A swipe beginning on a slider or a horizontal row
  adjusts that control rather than paging. Start on empty space or a plain card.

[:octicons-arrow-right-24: Gestures](getting-started/gestures.md)

### I dismissed the Quick start guide and forgot the gestures

All five are written down in [Gestures](getting-started/gestures.md). The one worth memorising is
**pull down on the page header** — that is Search, Flows, Edit and Settings.

### The rooms page is empty and says to enable edit mode

Two different things can cause that:

- You have **no areas in Home Assistant**, so there was nothing to import. See
  [Rooms](guide/rooms.md).
- The dashboard was started empty. Swipe down on the header, enable edit mode, and add floors and
  rooms.

---

## Dashboards

### The dashboard did not pick up a new entity

Auto-generation runs once. After your first edit, the dashboard is in **Manual** mode and is no
longer regenerated. Add the entity in edit mode, or reach it through global search. The current
mode is the subtitle under **Settings › Dashboard**.

### A shared family dashboard is not updating

- **Sync is pull-on-open**, not background polling. Open the sharing screen to fetch.
- Check with your admin that the dashboard is still shared with you — access can be revoked
  afterwards.
- If you are on an older app build than the admin, a widget type your build does not recognise is
  skipped rather than breaking the dashboard. The rest still updates; that one widget appears once
  you update.

### A family dashboard disappeared, and so did my theme

That is intentional. When a dashboard is unpublished, or you are removed from its recipient list,
everything it brought with it goes too — rooms, widgets, pages, nav order, header pill, weather
entities and theme — rather than leaving the next shared dashboard to land on top of leftovers.

Your connections, profile, notification and backup settings are untouched.

### I cannot enter edit mode

An admin may have turned editing off for you, or limited you to aesthetic changes.
[:octicons-arrow-right-24: Permissions](guide/family-sharing.md#permissions)

---

## Family sharing

### The Family Sharing screen says the component is missing

Install [HKI 7 Cloud](guide/family-sharing.md#install-hki-7-cloud) through HACS, restart Home
Assistant, add the integration, then reopen the screen.

### A setting I changed did not stick

The installed component is probably older than the feature. Each Family Sharing tab shows the
version it needs, and the header lists every missing feature at once. Update the component in
HACS.

[:octicons-arrow-right-24: Component versions](guide/family-sharing.md#component-versions)

### Room following keeps switching views

Raise the **dwell time**. Room-presence sensors flap between adjacent rooms, and the dwell window
is what stops every flap counting as a move. The default is 20 seconds; it goes up to 600.

If you only want to be placed correctly at launch, turn **Continue after launch** off.

### "Could not change the required version"

You can only require a version that some family device already runs — otherwise the requirement
could be one nobody is able to install. Get one device onto the target version first.

---

## Widgets

### A widget shows nothing

Most widgets need a specific integration. Check the widget's page in
[Integrations](reference/integrations.md) and confirm the integration is installed and has
entities.

### The Formula 1 widget is empty

It needs [F1 Sensor](https://github.com/Nicxe/f1_sensor). The widget finds its sensors through the
entity registry by platform and translation key, so renaming them is fine — but the integration
itself has to be there.

### The Parcels widget is missing a carrier

Only the carriers listed in [Integrations](reference/integrations.md#parcel-integrations) are
recognised. Each needs its own integration installed.

### The vacuum map is blank

Live maps are for [Valetudo](https://valetudo.cloud/) robots. Check that the map entity is
selected in the widget's settings, and that the entity is producing map data in Home Assistant.

---

## Backups

### "The backup could not be read"

The file is not an HKI 7 backup, or it is truncated. Backups are named
`HKI7-<version>-<date>_<time>.json`. If it came from Google Drive's web interface, check it was
not saved as a shortcut file.

### Google Drive backups stopped running

The daily backup runs under WorkManager, which Android's battery management can suppress. See
[Background work](#background-work-gets-killed). Manual backups always work.

### Home Assistant backups fail

The error names the likely cause: the HKI 7 Cloud component is unreachable. Check the component is
installed and Home Assistant is up.

---

## Updates

### I am told to update but Play has nothing

Play can only install what it has for you — a staged rollout may not have reached your account,
or your copy may not have come from the Play Store. In that case the prompt explains why and lets
you past for that session rather than locking you out.

### The app asks for an update every launch

An admin has set a household minimum version. Once Play installs the update, the prompt stops.
Ask your admin if the requirement is intentional.

---

## Still stuck

Open an issue at
[github.com/jimz011/android-hki7/issues](https://github.com/jimz011/android-hki7/issues) with:

- What you did and what happened.
- Your Android version and device.
- The app version (**Settings › HKI 7 › About**).
- The **Connection log**, if the problem is connection-shaped.
- The HKI 7 Cloud component version, if the problem is family-sharing-shaped.
