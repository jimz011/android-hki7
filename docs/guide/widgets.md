# Widgets

Widgets are the larger cards you place on a page, beside buttons and stacks. Add one in edit mode
from the **add** menu.

Every widget shares the same appearance and visibility controls described in
[Dashboards](dashboards.md#appearance-of-an-item): width (full, half or third), corner radius,
optional title and icon, an optional background image, and the full set of
[visibility rules](dashboards.md#visibility-rules).

## Weather

Six styles, each its own card, all reading a `weather.*` entity — or the app's default weather
entity if you do not name one:

| Style | Shows |
|---|---|
| **Current** | Conditions right now |
| **Forecast** | The daily forecast strip |
| **Hourly** | The hourly forecast strip |
| **Horizon** | Sun position through the day |
| **Wind** | Wind speed and direction |
| **Rain map** | An external radar or rain-map image, from a URL you supply |

Weather artwork is animated Lottie, and where it animates is four separate switches — header pill,
weather dialog, forecast strips, and the weather widget. See
[Appearance](appearance.md#icons-and-animation).

## Calendar

One or more `calendar.*` entities, in an **agenda**, **week** or **month** view. Each calendar in
the widget has its own visibility rule, so a shared work calendar can be scheduled to disappear at
the weekend.

## Clock

An analog or digital clock, in one of seven designs each:

| Analog | Digital |
| --- | --- |
| Classic, Minimal, Roman, Railway, Bauhaus, Neon, Skeleton | Plain, Segment, Monospace, Flip, Outline, Stacked, Dots |

Digital faces can show 12 or 24-hour time. Either can show seconds, and the day of the week, the
date and the year underneath, each switched on separately.

Tapping the clock shows **the next alarm** set on the phone, and offers to open a clock app.

!!! info "Why there is no list of alarms"

    Android exposes exactly one alarm to other apps — the next one due, across every app — and
    provides no way to read the full list or to switch alarms on and off. No third-party app can
    show more than this; the official Home Assistant app's sensor has the same shape for the same
    reason.

    Which app the button opens can be pinned under **Clock app** in the widget's settings.
    **Automatic** asks the system, which works on most phones but not all — some vendors ship a
    clock that never claims the standard "show alarms" action.

## Media player

A `media_player.*` entity with full transport controls and artwork. Which players may appear, and
what they are called, is managed under **Settings › Dashboard › Media players**.

For a player that reports what it is playing, the card shows the content rather than just the
state: a series with its season and episode, a track with its artist, or the channel for live TV,
with the app it is coming from underneath. An Android TV that reports only which app is open shows
that app's name.

## Sensor graph

A history graph for one or more numeric sensors, drawn as a **line** (temperature style) or
**bars** (energy style), over a configurable window in hours. There is also a **sensor graph
stack** for grouping several graphs together.

## Markdown

Free-form Markdown text. Useful for a heading, a note, or a block of instructions on a wall
tablet.

## iFrame

Any web page, embedded in the dashboard, at a chosen aspect ratio (1:1, 4:3, 16:9, or tall).

## To-do list

A shared checklist or shopping list the whole family can add to.

- Items carry an optional **quantity**, **category**, **priority** (low / normal / high, shown as
  a flag) and **due date**.
- **Added by** and **Checked by** attribution appears once more than one person edits the list.
- **Tabs** group items by category; a chosen tab can be the **hero list** featured on the square
  card, so what you see at a glance is "Chores" rather than a mix pulled from everywhere.
- Ordering is **manual**, **A–Z**, **priority** or **newest first**; the dialog filters between
  *To do* / *All* / *Done*.
- Editing can be limited to **everyone**, **specific family members** picked from the real Home
  Assistant user list, or **admins only** — enforced the same way the rest of the app's
  [family permissions](family-sharing.md#permissions) are.

## Find my devices

Plots `device_tracker.*` and `person.*` entities on a map — phones, watches, tags and trackers —
auto-framed to fit them all, with pan and pinch. Trackers with no GPS still list as "not home",
which is often the answer you wanted. Each device has its own visibility rule.

The map is drawn from tiles directly rather than through a map SDK, which keeps it dependency-free
and consistent with the rest of the app's maps.

!!! note "Widgets that need an integration"

    Formula 1, Parcels and Waste Collection appear greyed out in the widget picker when nothing
    backing them is found on your Home Assistant, with the missing integration named. They are
    still listed rather than hidden, so it is clear what is needed to use them.

## Parcels

Reads the [ha-parcel-integrations](https://github.com/search?q=ha-parcel-integrations) carrier
integrations and shows incoming and outgoing parcels with brand logos, delivery windows and
status. Carrier logos are bundled in the app, so they work offline.

Every carrier the organisation publishes is supported: **Ampère**, **An Post**, **Budbee**,
**Cainiao**, **Correos**, **Delhivery**, **DHL**, **DPD**, **Dragonfly**, **Dynalogic**, **GLS**,
**Helthjem**, **Hermes**, **InPost**, **Nova Post**, **Österreichische Post**, **Packeta**,
**Planzer**, **PostNL**, **PostNord**, **Quickpac**, **Sameday**, **SunYou**, **Swiss Post**,
**Trunkrs**, **Vinted Go**, and the generic **Parcels** aggregator.

Some carriers expose a manual `track_parcel` service, so you can add a parcel by tracking number
from inside the widget. HKI 7 asks Home Assistant which ones do rather than carrying a fixed list,
so a carrier that gains the service — or one published after your app version — works without an
update. Account-based carriers only ever track what is already in the account, so they have no
manual add.

You can override a carrier's display name and artwork, and merge several accounts of the same
carrier into one tab.

## Waste collection

Reads waste-type sensors — [Afvalbeheer](https://github.com/pippyn/Home-Assistant-Sensor-Afvalbeheer)
and anything shaped like it — and shows the next collection on the card. Tapping opens every
category plus an optional week calendar looking 7, 14 or 28 days ahead. Fractions can be drawn as
coloured MDI icons or as the sensor's own `entity_picture`.

## Formula 1

Race weekends, standings and results from the
[F1 Sensor](https://github.com/Nicxe/f1_sensor) custom integration. Six tabs — **Next**,
**Calendar**, **Standings**, **Grid**, **Results** and **Live** — and you choose which opens
first.

- **Calendar** lists the season in order with circuits and host-country flags.
- **Grid** shows the starting grid with each driver's move from where they qualified.
- **Standings** can show a championship prediction beside the points table when F1 Sensor's
  optional F1TV Auth is configured.
- **Live** gives a timing list: gap to leader, interval to the car ahead, tyre compound and stint
  length, pit and retired status, the leader's lap and the race distance.

The widget stores no entity ids — F1 Sensor lets you pick localised or legacy entity names, so it
finds its sensors through the entity registry by platform and translation key instead.

## Vacuum

A vacuum card that can render as a static image, a live camera feed, or an external image URL,
with battery, water tank and bin state. For [Valetudo](https://valetudo.cloud/) robots it draws
the **live cleaning map**, including segment cleaning.

When your rooms are imported, a Valetudo robot has its map camera bound automatically — the map lives on the same Home Assistant device as the vacuum, so nothing has to be chosen by hand.

## Camera

A live camera stream, from a `camera.*` entity or a custom URL, at a chosen aspect ratio.

## Battery card

Every battery-powered entity in one card, with a configurable **low threshold** (30% by default)
and optional [Battery Notes](https://github.com/andrew-codechimp/HA-Battery-Notes) support for
battery types and replacement dates. Tapping the card opens a list of every battery it watches,
grouped into the same Critical / Low / Watch / Good / Unknown bands the Battery view uses, with a
search field and the battery type where Battery Notes supplies it. **Open battery page** switches to
the Battery view itself.

## Energy and climate cards

Individual cards lifted out of the [Energy](controls.md#energy) and
[Climate](controls.md#climate) screens and placed on any page, plus **stacks** for grouping
several of them. Energy card data always reflects *today*.

## Subtitle

A plain text heading with an optional icon — the way to break a long page into labelled sections.

## Adaptive Lighting

Per-room controls for the
[adaptive_lighting](https://github.com/basnijholt/adaptive-lighting) integration, as a stack. Two
layouts: **full** shows every control, **double row** keeps only the identity line and the two
action buttons, optionally centred. Auto-generated room widgets are scoped to their own room so
they cannot operate a profile from somewhere else in the house.

---

!!! info "Widgets from a newer app version"

    If a family dashboard arrives carrying a widget type your build does not know about, it is
    decoded as a placeholder and skipped rather than failing the whole dashboard. The rest of the
    dashboard keeps working, and the unfamiliar widget appears once you update.
