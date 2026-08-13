# Your first dashboard

Onboarding offers four starting points. Everything stays editable afterwards, whichever you pick.

!!! tip "Set up your areas in Home Assistant first"

    HKI 7 builds its rooms from Home Assistant's **areas**. If you have not assigned your devices
    to areas yet, do that before auto-generating — otherwise there is nothing for the importer to
    build rooms from, and you will get a dashboard with no rooms in it.

    In Home Assistant: **Settings → Areas, labels & zones**. Create an area per room, optionally
    group them into floors, then assign each device to one under **Settings → Devices & Services →
    Devices**.

    It is worth ten minutes. Areas are also what make the room screens, the per-room counters and
    the room-role slots work, and Home Assistant itself uses them for area-targeted automations.

## Auto-generate

**Recommended for most people.** HKI 7 reads your Home Assistant areas, floors and entities and
builds a dashboard from them: a Home page with the things that matter, and a room for each area
you have.

**No areas means no rooms.** Auto-generation has nothing to group entities by, so you get a Home
page and an empty Rooms page. Set the areas up in Home Assistant and re-run the import, or add
rooms by hand afterwards.

Auto-generation is a **one-time starting point**, not a live mirror. The moment you edit anything,
the dashboard switches from *Automatic* to *Manual* mode and HKI 7 stops regenerating it — your
arrangement is yours, and a new entity in Home Assistant will not shuffle it around. You can see
which mode you are in as the subtitle under **Settings › Dashboard**.

!!! info "Adding things later"

    In Manual mode, new Home Assistant entities do not appear on their own. Add them in edit mode,
    or use [global search](../guide/dashboards.md#global-search) to reach them without putting them
    on a page at all.

## Start empty

Builds nothing. You get the Home and Rooms pages and an empty canvas, and you add every button,
stack and widget yourself in edit mode. Choose this if you have a specific layout in mind and
would rather not undo someone else's guesses.

## Restore a backup

If you have used HKI 7 before, restore instead of rebuilding. Three sources are offered:

| Source | What it needs |
|---|---|
| **Local file** | A backup file on the device or in cloud storage the file picker can see |
| **Google Drive** | Sign-in to your own Google account; backups live in the app's private Drive area |
| **Home Assistant** | The [HKI 7 Cloud](../guide/family-sharing.md) component installed on your server |

See [Backup and restore](../guide/backup.md) for how each one works and what a backup contains.

## Import from family

If someone in your household is an HKI 7 admin and has published a dashboard to you, it shows up
here — pick it and it is imported, complete with its rooms, widgets, pages, navigation order and
theme.

If the list is empty, one of two things is true, and the screen says which:

- **No HKI 7 Cloud component.** An admin needs to install it on your Home Assistant first.
- **Nothing shared with you yet.** The component is there, but no dashboard names you as a
  recipient. Ask your admin to publish one.

[:octicons-arrow-right-24: Family sharing](../guide/family-sharing.md)

## Permissions

The last onboarding step offers three permissions, with a progress counter. All three are
optional; the app is fully usable with none of them.

**Notifications**

: Lets Home Assistant alerts reach the phone, including actionable notifications with buttons.
  See [Notifications](../guide/notifications.md).

**Background location**

: Keeps presence detection and zone automations working while the app is closed. Android requires
  this to be granted in two moves: allow location, then choose **Allow all the time** in system
  settings. See [Presence and location](../guide/presence.md).

**Unrestricted background usage**

: Stops Android from delaying battery reporting and presence updates when it decides the app has
  been idle too long.

Each one can be turned on later — notifications under **Settings › Notifications**, the other two
under **Settings › Location**, which also shows the current state of each permission and links
straight to the relevant Android settings screen.

## Next

Your dashboard opens with a **Quick start** dialog showing the five gestures that get you around
the app — including the pull-down on the header that reaches edit mode and settings. It is worth
the thirty seconds.

[:octicons-arrow-right-24: Gestures](gestures.md) ·
[:octicons-arrow-right-24: The dashboard editor](../guide/dashboards.md)
