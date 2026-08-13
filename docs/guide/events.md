# Event timeline

The **Events** tab in the notification panel is a feed of what your home has actually been doing:
the front door opening, a light going off, someone arriving home — each line with the time and,
where Home Assistant knows it, who caused it.

!!! info "Requirements"

    Events needs [HKI 7 Cloud](family-sharing.md) **0.9.0** or newer. Whole-domain rosters need
    **0.10.0**.

## Where the data comes from

The timeline reads Home Assistant's **own logbook**, live, over a `logbook/event_stream`
subscription. It records nothing on the device.

That distinction matters more than it sounds. The recorder already saw what happened overnight
while HKI was closed; a timeline whose history begins when you opened the app is missing precisely
the events worth showing.

The subscription is only held while the tab is actually on screen.

## Reading it

Raw entity states are **phrased** rather than shown as-is, so you get "Front door was opened"
instead of `binary_sensor.front_door: on`. The wording is chosen from an entity's **device class**
first and its **domain** second — device class being the more specific of the two and the one
carrying the meaning, since a bare `binary_sensor` can only manage "was triggered" while the same
entity marked as a door earns "was opened". Where Home Assistant phrases an event itself, that
phrasing is used unchanged.

### Time range

The timeline opens on the last **3 hours** — it answers "what just happened", and a whole day of a
busy household buries that under hundreds of older rows. **6h**, **12h**, **24h**, **48h** and
**72h** are one tap away for when the question really is about yesterday.

### Filters

Events are labelled and filterable by what kind of thing they happened to — Doors, Windows,
Motion, Lights, Locks. The filter row is built from **what the timeline actually contains**, so a
household with no water sensors is never offered a Water filter that could only come back empty,
and the categories are ordered by how much of the timeline each accounts for.

Grouping is by device class first and domain second, since `binary_sensor` alone covers doors,
windows, motion and smoke and would otherwise pile all of them into one heap.

Days remain the list's own headings. A timeline that reorders itself by category stops being a
timeline.

## Setting it up (admins)

The roster is configured **once for the whole household**, under
**Settings › Family Sharing › Events** — not on each phone, which is the difference between
setting this up and setting it up eleven times.

The roster takes **whole domains** as well as named entities, so "every lock" is one choice rather
than eleven. Domains are stored unexpanded and resolved against your entity list each time the
timeline opens, which means a lock added next month joins the timeline on its own instead of the
roster quietly meaning whatever existed the day it was set.

Because one domain can cover hundreds of entities, the roster editor shows what each one currently
resolves to, and warns before a subscription would exceed what a phone should reasonably carry.

If nothing is being tracked yet, admins see a button straight to that screen; family members who
cannot configure it are not shown a button that leads nowhere for them.

## Hiding events from someone

An admin can take parts of the timeline away from individual family members — a whole domain, or
named entities — from the same Events tab.

It is **subtractive**: everyone sees the full roster unless something is taken out for them, so
adding someone to the household does not mean assembling their timeline from nothing.

The filtering happens **inside the HKI 7 Cloud component**, not in the app, so a restricted device
is never handed the ids it is being kept away from and cannot subscribe to them by ignoring a
client-side filter.

!!! warning "This hides the timeline, not the entities"

    As with hidden views and rooms, this hides the *timeline*. Home Assistant has no per-entity
    read permission for non-admin accounts, so anyone in the family can still read those entities
    through Home Assistant directly. Worth knowing before relying on it for anything that actually
    matters.
