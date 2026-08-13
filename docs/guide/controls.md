# Controls and screens

## Per-domain dialogs

Tapping an entity opens a dialog built for that kind of device, not a generic more-info sheet.

**Lights**

: Brightness, colour temperature, colour picker, and effects, with a Google Home-style
  full-height brightness control available on the button itself.

**Climate**

: Target temperature as a **slider** or a **dial**, HVAC and preset modes, fan and swing, and an
  Activity tab graphing the room's temperature and humidity sensors.

**Covers**

: Position and, where the device supports it, **tilt**, with a dedicated tilt slider.

**Fans**

: Speed, oscillation, direction and preset modes.

**Locks**

: Lock and unlock, with an optional door/contact sensor whose open state turns the card red.
  Buttons can be locked behind a double-tap or a PIN — see
  [Dashboards](dashboards.md#locking-a-button).

**Media players**

: Transport, volume, source and artwork.

**Vacuums**

: Start, pause, return, fan speed, and — for [Valetudo](https://valetudo.cloud/) robots — a live
  cleaning map with per-segment cleaning.

**Humidifiers**

: Target humidity and modes, plus auxiliary readings the integration exposes (current humidity,
  tank level, PM2.5, filter, defrost, ioniser, pump and so on).

**Alarms**

: Arm home / away / night, disarm, with keypad entry.

**Cameras**

: Live stream, full screen, with a configurable refresh interval for snapshot cameras.

**Weather**

: Current conditions, daily and hourly forecasts, wind, and sun horizon.

Every dialog can carry **quick-access buttons** you add yourself, shown in its nav bar — a scene,
a script, or a related entity, right next to the thing it belongs to.

## History and Activity

Any entity's dialog can show its history, read from Home Assistant's recorder, with each change
matched to who or what triggered it — an automation, a person, a device.

## Global search

Global search reaches every entity on the server, whether or not it appears on a dashboard. Open
it from the dashboard header.

An admin can hide the search action from a family member entirely, or restrict what it exposes
with per-person visible/hidden domain and entity lists.

## Flows

The flows action lists your Home Assistant automations and scripts and lets you run or toggle
them. Like search, it can be hidden per person.

---

# Dedicated screens

Five full screens sit beside the dashboard, each reachable from the navigation bar (their order,
and whether they appear at all, is set under **Settings › Dashboard**).

## Climate

Every thermostat, humidifier, air purifier and environmental sensor in the house on one screen.

- **Thermostats** render as a **dial** or a card, at full, half or third width, in standard or
  square shape — set page-wide with per-device overrides.
- **Sensors** are grouped into temperature, humidity, pressure, CO₂ and air quality, discovered
  from their device class.
- **Outside** is its own tile: outside sensors can never be auto-discovered (nothing marks a
  sensor as outdoors), so you name them, or point the page at a `weather.*` entity and let its
  attributes supply the readings.
- **Fans** can be flagged as air purifiers, since a fan carries no device class to say so.

Anything the auto-discovery gets wrong can be added, hidden, renamed, re-iconed or reordered, and
there is a **manual only** switch that turns discovery off entirely so only your explicit choices
appear.

## Energy

A power-flow visualisation and a full set of tabs over your energy data.

!!! tip "Configure Home Assistant's Energy dashboard first"

    The single biggest thing you can do for this screen is set up **Home Assistant's own Energy
    dashboard** before using it — **Settings → Dashboards → Energy** in Home Assistant, where you
    name your grid, solar, battery, gas and water sources.

    HKI 7 can then **import those preferences** in one step instead of asking you to identify
    every sensor again by hand. You get the flow diagram, costs and per-source breakdowns
    immediately, and they match what Home Assistant itself reports rather than being a second,
    slightly different guess.

    Without it the screen still works — you just point it at each sensor yourself.

- **Import from Home Assistant.** If you have configured Home Assistant's own Energy dashboard,
  HKI 7 can import those preferences rather than making you name every sensor again. Importing
  also switches off class-wide auto-discovery, so your explicit configuration is what the screen
  uses.
- **Electricity** — grid import and export, per-phase power, current and voltage for a P1-style
  meter, and tariff-split counters.
- **Solar** — production, last 7 days, lifetime total, and multi-entity forecasts, including Home
  Assistant's own solar forecast config entries.
- **Battery** — home battery charge and flow.
- **Gas**, **Water** and **District heating** — totals, live flow rates, and cost.
- **Top consumers** and **Device energy** — individual power and energy sensors you choose to
  track, each addable or removable by hand.
- **Cost** and **carbon footprint** where the sensors exist.

Cards can be reordered and renamed, and any card can be lifted out onto a normal dashboard page as
an [energy card widget](widgets.md#energy-and-climate-cards).

## Security

Doors, windows, motion, smoke, water, locks and cameras, grouped by what they are, discovered from
device class and domain. Entities can be added, hidden, renamed, re-iconed and reordered, and each
camera carries its own name and refresh interval.

As with the other screens, **manual only** turns discovery off so nothing appears unless you put
it there.

## Vacuum

Every vacuum in the house: state, battery, water and bin, controls, and live maps for Valetudo
robots.

## Battery

Every battery-powered entity, sorted by level, with a configurable low threshold. Supports
[Battery Notes](https://github.com/andrew-codechimp/HA-Battery-Notes) for battery types and
replacement dates, and lets you add devices the discovery missed, hide ones you do not care about,
rename them, and set the order.
