# Family sharing

Family sharing lets one person build a dashboard and give it to the household, hide things from
particular people, and manage what each family member is allowed to do.

It is entirely optional, and everything it stores lives on **your own Home Assistant**. Nothing
about it leaves your home.

## Install HKI 7 Cloud

Family sharing is powered by the companion
[HKI 7 Cloud](https://github.com/jimz011/HKI7-Cloud-Component) integration — a small, free Home
Assistant add-on that stores the household's shared state.

!!! info "Why a component at all"

    Home Assistant has no place for an app to store per-user configuration. The component gives
    HKI 7 one, on your own server, rather than a developer-run service somewhere else.

Install it through HACS:

1. In Home Assistant, open **HACS → the ⋮ menu in the top-right → Custom repositories**.
2. Paste the repository URL, choose the **Integration** category, and add it.
3. Open the new **HKI 7 Cloud** entry, install it, and restart Home Assistant.
4. Go to **Settings → Devices & Services → Add Integration**, choose **HKI 7 Cloud**, and confirm.
5. Reopen **Settings › Family Sharing** in the app. The features appear automatically.

The app's Family Sharing screen has this checklist built in, with a button straight to the
repository.

### Component versions

The app tells you which component version is installed and, if it is behind, exactly which
features that leaves unavailable. Each Family Sharing tab also carries its own version notice, so
you find out *before* configuring a tab whose settings the installed component cannot store yet.

| Component | Adds |
|---|---|
| `0.5.3` | Manually re-importing or clearing dashboard view data |
| `0.5.4` | Per-person Visible/Invisible global search lists |
| `0.6.0` | Room following |
| `0.6.1` | Room following: launch-only mode |
| `0.7.0` | Seeing which HKI version each family device runs |
| `0.8.0` | Requiring the family to update to a given HKI version |
| `0.9.0` | The event timeline, and hiding parts of it from individual people |
| `0.10.0` | Whole domains on the event roster |

Anything older than `0.6.1` predates version reporting, and shows as "installed (update to see its
version)".

## Who is an admin

Admin status comes from **Home Assistant**, not from HKI 7 — an owner or administrator account on
your server is an admin in the app. Admins and owners are never subject to parental controls
themselves.

The whole Family Sharing settings screen is admin-only. Family members see that it exists and that
it is not theirs to configure.

## Shared dashboards

An admin builds a dashboard the usual way and **publishes** it, choosing **Everyone** or specific
Home Assistant users. Recipients see it under **Import from family** and pull it in.

- **Sync is pull-on-open.** The shared list is fetched when the sharing screen opens, and
  importing is an explicit action. There is no background polling.
- **Push to family dashboard** sends your changes to an already-published dashboard.
- **Manage access** changes who a published dashboard reaches, afterwards — not only at the moment
  of publishing. Revoking republishes the dashboard's stored copy with a shorter recipient list,
  so it works from a device that never had the dashboard locally: an admin's second phone, or the
  same phone after a reinstall.
- **Unpublish** removes it for everybody.

### Losing access

When a dashboard is unpublished, or someone is dropped from its recipient list, everything it
brought with it is removed together: the rooms, widgets, pages, navigation order, header pill,
weather entities and theme.

Home Assistant connections, profiles, notification settings and backup settings are deliberately
left alone. **Losing a dashboard is not being signed out.**

## Parental controls

An admin can hide things from specific family members:

- **Views** — whole navigation routes.
- **Rooms** — by area.
- **Individual items** — a button (by its entity id), or a badge or widget (by its id, which is
  visible in its Appearance settings).

!!! warning "This is not a security boundary"

    Parental controls are **UX-level hiding for a friendlier dashboard**. Home Assistant has no
    per-entity read permission for non-admin users, so anyone in the household can still reach
    those entities through Home Assistant directly.

    Use it to keep a child's phone uncluttered and to keep the thermostat out of reach of
    curiosity. Do not use it as the thing standing between someone and your front door lock.

## Permissions

Per-person switches, all defaulting to permissive:

| Permission | Effect when off |
|---|---|
| **Allow edit** | The person cannot enter dashboard edit mode at all |
| **Aesthetics only** | Editing is allowed, but limited to theme, colours, icons, names and wallpaper — no adding or removing widgets, buttons or rooms |
| **Show global search** | The global search action is hidden |
| **Show flows** | The automations/scripts action is hidden |
| **Allow dashboard switch** | A family-dashboard subscriber cannot load a different dashboard |
| **Allow dashboard create** | They cannot create or duplicate a dashboard |
| **Allow re-import** | They cannot manually re-import or clear view data from Home Assistant |

### Search lists

Global search can also be narrowed per person:

- **Visible** domains and entity ids form an allow-list. When either list has entries, search only
  exposes matching entities. Empty lists keep the unrestricted default.
- **Hidden** domains and entity ids form a deny-list, and **always win** over the allow-list.

## Room following

If your household runs a room-presence sensor — [ESPresense](https://espresense.com/) or
`mqtt_room` — HKI 7 can put a person in the room they are actually in.

Nothing in the app talks to MQTT. Those integrations publish the room name as an ordinary sensor
state; HKI 7 reads that sensor.

Per person, an admin sets:

**Sensor**

: Which entity reports this person's room.

**Open on launch**

: Open the resolved room as soon as the app starts.

**Continue after launch**

: Keep tracking moves afterwards. Turn this off and launch placement is the only thing following
  ever does — no prompts, no silent moves once the app is already open.

**Prompt on move**

: Offer to switch views when the person moves, rather than switching on its own. Only consulted
  while *Continue after launch* is on.

**Dwell time**

: How long a new room must hold before it counts as a real move. Room-presence sensors flap
  between adjacent rooms, so without a dwell window the prompt would fire constantly. Default is
  20 seconds, up to 600.

**State → room mapping**

: For sensor states whose text does not match an area name.

The household's roster of room sensors also feeds the people-per-room counters on room cards.

## Events

The event timeline's roster and per-person visibility are configured here.
[:octicons-arrow-right-24: Event timeline](events.md)

## Devices

A list of every family device: which HKI version it runs, the Home Assistant account it is signed
in as, its device name, Android version, and when it last reported. Devices older than the version
you are on are flagged.

Each install records this through the HKI 7 Cloud component, over the connection the app already
authenticates on. That means it covers **every signed-in device**, including one with Location and
Notifications both switched off, and it adds nothing to Home Assistant's entity list.

A device can only ever report *itself*: which account a record belongs to is decided by Home
Assistant from the authenticated connection, never by anything the phone claims.

An admin can **forget** a phone that has been replaced or had the app removed; one still in use
simply reappears the next time its app opens.

### Requiring an update

**Require everyone to be on `<version>`** sets a household minimum. Anyone below it gets a
full-screen prompt on their next launch instead of the dashboard, and Google Play's own in-app
update handles the install. There is also a per-device **Ask to update to `<version>`** for when
one phone is behind rather than everyone, which clears itself as soon as that device reports a
satisfying version.

Two things are worth understanding about how this works:

- **Play does the actual installing.** No app can install a newer version of itself; silent
  installation needs device-owner privileges HKI does not have. So the requirement can only insist
  when Play genuinely has the update ready for that person.
- **Nobody gets locked out.** When Play has nothing to offer — a rollout that has not reached
  them, or a copy that did not come from the Play Store — the prompt explains why and lets them
  past for that session, rather than locking someone out of their own locks and alarms over an
  update they cannot get.

You can only require a version that some family device already runs, so the requirement can never
be one nobody is able to install.
