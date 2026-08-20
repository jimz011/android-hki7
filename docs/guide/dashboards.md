# Dashboards

A dashboard in HKI 7 is a set of **pages** — Home, Rooms, and any custom pages you add — filled
with **items**: buttons, stacks, badges and widgets. This page covers how to build one.

## Edit mode

Everything on a dashboard is edited in place. There is no separate YAML view and no separate
editor screen.

**Pull down on the page header** to reveal Search, Flows, **Edit** and Settings. That is how you
get into edit mode from anywhere in the app.
[:octicons-arrow-right-24: Gestures](../getting-started/gestures.md)

While in edit mode you can:

- Add items to a page.
- Drag to reorder, including into and out of stacks.
- Open any item's settings to change what it shows and how it behaves.
- Delete items.

!!! info "Automatic vs Manual"

    A freshly auto-generated dashboard is in **Automatic** mode and is rebuilt from Home Assistant
    when things change there. The first edit switches it to **Manual**, and HKI 7 stops
    regenerating it. The current mode is the subtitle under **Settings › Dashboard**.

!!! note "Family members may not have edit mode"

    An admin can turn editing off for a person entirely, or limit them to aesthetic changes only.
    See [Family sharing](family-sharing.md#permissions).

## Buttons

A button is the basic unit: an entity, an icon, a name, and a state line.

### Layout

Four layouts, available on both single buttons and every button in a stack:

| Layout | Shape |
|---|---|
| **Standard** | A wide card, icon on the left, text beside it |
| **Square** | A square card |
| **Tile** | A compact tile |
| **Centered** | A square card with the icon centred in the space above the text, and the name and state centred beneath it |

Centered is what makes a grid of buttons read like a keypad, which is what makes a usable remote
control.

### Elements

A button's **icon**, **name** and **state** are three independent switches under **Elements** in
its settings. Turn off the name and the state and you have an icon-only button; turn off the icon
and keep the text, if that reads better.

### The state line

By default the second line shows the entity's state. It can instead show:

- **An attribute** — pick any attribute of the entity, with an optional unit suffix (`°C`, `%`,
  `W`, `kW`).
- **A countdown** — for entities whose value is a completion timestamp (a washer's "finished at"
  time), the value is rendered as a live descending timer. Because some integrations keep a stale
  future timestamp while the appliance is off, you can gate the countdown on a separate
  operation-state entity so it only runs when the machine actually is.

### Actions

Each button has a **tap**, **double-tap** and **hold** action, and each can be set to:

| Action | Does |
|---|---|
| `default` | Whatever is sensible for the domain |
| `none` | Nothing |
| `toggle` | Toggles the entity |
| `more_info` | Opens the entity's dialog — optionally a *different* entity's dialog |
| `call_service` | Calls a Home Assistant service, with arbitrary service data, targeting the button's own entity, another entity, or nothing |
| `navigate` | Jumps to Home, Rooms, Energy, Climate, Security, Battery, or a specific room |
| `url` | Opens a URL |
| `custom_popup` | Opens one of your [custom popups](#custom-popups) |

Buttons can also carry **quick-access buttons** of their own, shown in the nav bar of the dialog
they open — handy for putting a scene or a script next to the thing it acts on.

### Locking a button

Any button can be locked so it is not triggered by accident. Choose **double-tap** or a **PIN**,
and how many seconds it stays unlocked before re-locking (30 by default). Useful for the garage
door, the alarm, or anything with consequences.

### Domain extras

Some domains get extra settings on the button itself:

- **Lights** — an optional Google Home-style full-height brightness slider.
- **Locks** — an optional door/contact sensor whose open state turns the card red, so a locked
  lock on an open door is visibly wrong.
- **Climate** — separate temperature and humidity sensor entities, graphed in the dialog's
  Activity tab, and a choice of slider or dial control.
- **Vacuums** — how the button renders (static image, live camera, or external image URL) and
  which map, battery, water and bin entities to read.
- **Humidifiers** — an optional fan/select entity supplying speed options, plus auxiliary entities
  (current humidity, tank level, PM2.5, error, bucket full, clean filter, defrost, ioniser, pump,
  sleep, beep), which can be auto-filled by picking the Home Assistant device.
- **Cameras** — an entity or a custom URL, with a refresh interval.

## Stacks

A **stack** groups items. There are three kinds:

**Button stack**

: A grid of buttons with a configurable column count and an optional heading. The heading has its
  own **Show name** / **Show icon** / **Centre** switches, so a heading-less stack does not mean
  deleting its name.

    A stack carries **Show icon / Show name / Show state** switches for all of its children, so a
    stack of twenty buttons need not be set twenty times. These *mask* the children rather than
    override them: turning one off on the stack hides it on every button; leaving it on lets each
    button decide for itself. Icon size works the same way — the stack hands one down, and a
    button's own size still wins.

    Stacks can be collapsible, and can start collapsed.

**Swiping stack**

: Holds other widgets and cycles between them. Optional autoplay with a configurable interval and
  animation duration, and an optional heading.

**Empty stack**

: A plain container with a column count — a way to group arbitrary widgets under one heading.

Stacks can nest: a button stack can live inside a swiping stack.

## Badges

Badges are the small pills along the top of a page. A badge targets an entity, or **several**
entities that aggregate into one badge. They have their own icon, colour and tap behaviour, and
they live in the badge bar, configured per page.

## Custom popups

A popup is a dialog you design once and open from anywhere. Because popups are shared, several
buttons, badges or widgets can point at the same one — a "Living room lights" popup reachable from
the room card, the hallway button, and a badge, all pointing at a single definition you only
maintain once.

Popups are managed under **Settings › Dashboard › Popups**, and opened with a
`custom_popup` action.

## Pages and navigation

**Home** and **Rooms** always exist. Beyond those you can add **custom pages** — as many as you
like, each with its own name, icon and contents.

The navigation bar order is set under **Settings › Dashboard › Navigation Bar**. Home and Rooms are
always shown; the rest can be reordered and hidden.

[:octicons-arrow-right-24: Custom pages](custom-pages.md)

### Global search

Global search reaches every entity on your Home Assistant, whether or not it is on a page. It is
the answer to "I need this one thing occasionally and do not want a button for it".

An admin can turn search off for a family member, or restrict what it exposes with per-person
visible/hidden domain and entity lists. See
[Family sharing](family-sharing.md#permissions).

## Visibility rules

Any item — button, stack, badge or widget — can be shown or hidden by rule. This is HKI 7's
equivalent of a conditional card, and the rules combine.

**Manual**

: Simply hidden until you unhide it.

**Scheduled**

: A start and end date-time, and whether that window is when the item is **shown** or **hidden**.
  The window can repeat **daily**, **weekly**, **monthly** or **yearly** — a yearly window ignores
  the year, so a Christmas button set to *show* for 24–26 December recurs every year.

**Entity state**

: Gated on whether another entity's state does, or does not, equal a given value.

**Person**

: Gated on which Home Assistant user is signed in on this device — so one shared dashboard can
  show a different button to each family member.

Multiple conditions can be combined into one expression, each block carrying its own AND/OR
connector, so time, entity and person rules can be mixed.

## Appearance of an item

Every item shares the same appearance controls:

- **Width** — full, half or third of the row.
- **Corner radius** — per item, on top of the global roundness setting.
- **Icon** — any icon from the bundled Material Design Icons or Simple Icons sets, with an
  optional per-icon animation override (`auto`, off, or a forced glow / spin / pulse).
- **Background** — colour or image.

Global defaults for corners, icons and animation live under
[**Settings › Appearance**](appearance.md).
