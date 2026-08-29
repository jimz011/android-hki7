# Android Auto and Wear OS

HKI 7 runs in the car and on your wrist. Neither can show a dashboard, so both read one short list
you curate: **quick actions**.

## Why a separate list

Android Auto does not draw an app's own interface at all — it renders Google's templates, and hides
anything past a limit it sets while the car is moving. A watch face has room for a handful of rows.
Neither can render a dashboard, and neither should try.

So instead of deriving something from your dashboard, you pick the handful of things worth reaching
for away from the phone. **Settings › Android Auto & Wear OS.**

Each entry has its own name, icon and tap action — edited with the same action editor as dashboard
buttons — and can be shown or hidden per surface. Drag to reorder; the car shows the top few.

!!! info "The list is yours, not a dashboard's"

    Quick actions are stored per device rather than per dashboard, so switching dashboards leaves
    them alone. They travel in your backup.

## What a tap does

The action types are deliberately narrower than on the dashboard. A dashboard tap can open a
dialog; a car screen and a watch cannot draw one.

**Toggle** and **Home Assistant action** work everywhere.

: A light, a switch, a scene, a script, a cover — anything with one unambiguous outcome.

**Default** works where the domain has an obvious meaning.

: Lights and switches toggle. Scenes and scripts run. Buttons press.

**Locks, vacuums and alarm panels** ask you to choose.

: There is no safe guess for a lock — locking and unlocking are equally plausible, and the wrong
  one at speed is worse than nothing. Settings marks these entries and asks you to pick Toggle or
  a specific action instead of guessing for you.

Settings flags any entry that would do nothing, so a shortcut never fails silently somewhere you
cannot see why.

## Family permissions

The household search policy applies to both surfaces. An entity an admin has restricted never
appears in the picker, never reaches the watch, and stops working in the car the moment access is
revoked — without anyone editing their list.

---

## Android Auto

Connect your phone and HKI 7 appears on the car's app launcher. The screen is a single grid: each
quick action with its current state, and a tap runs it.

State is **live**. HKI 7 follows Home Assistant for as long as the car screen is up, so a light
someone switches at the wall updates by itself. There is no refresh button because there is nothing
to refresh.

!!! warning "Android Auto needs an install from Google Play"

    Android Auto refuses to show apps like this one unless they came from Google Play. Its
    "Unknown sources" developer option does **not** cover them.

    A copy of HKI 7 installed from GitHub works normally in every other respect, but the car
    screen will never appear. Settings says so on such a copy rather than leaving you to find out
    in the car.

### What the car will not do

Choosing *what* is on the grid happens on the phone. Android Auto limits how deep a task may go
and hides content while the car is moving, so anything behind a second tap would be unreachable
exactly when it is wanted.

---

## Wear OS

The watch app shows your quick actions with live state, and your rooms underneath.

### Setting it up

Usually nothing: **if HKI 7 is on your phone, the watch is set up from there**. The phone hands it
the server address, a session, your quick actions and your rooms. Change what appears on the watch
on your phone and it is there immediately.

If HKI 7 is not on the phone — a reset watch, or a phone that never had it — the watch can sign in
by itself. Type the Home Assistant address on the watch and the login page opens **on your phone's
browser**, so your password and any two-factor step happen on a screen with a real keyboard and
your password manager. Nothing sensitive is ever typed on a watch.

Either way the watch ends up with a session of its own and keeps it alive by itself.

!!! info "The watch does not need your phone to work"

    It talks to Home Assistant directly rather than relaying through the phone, so it keeps working
    with the phone in another room, or on an LTE watch with the phone switched off.

    The one requirement is that Home Assistant is reachable from where the watch is. A local-only
    setup — a `192.168.x.x` address and nothing else — works on your home Wi-Fi and nowhere else.
    Nabu Casa or a reverse proxy covers the rest.

### Rooms

Rooms on the watch are the rooms from your dashboard, with the same names, icons and contents —
including anything you hid. They come from the phone rather than from Home Assistant's area
registry, which is what makes them *your* rooms rather than a raw list.

Sensors and cameras are left out. A watch row is a name, a state and a tap, so things you cannot
tap would crowd out the things you can.

Because rooms come from the phone, a watch signed in on its own gets quick actions but no rooms
until HKI 7 is on the phone too.

### Tiles

One swipe from the watch face, no app to open.

**Quick actions**

: The top few entries, each a tap away.

**Thermostat**

: One thermostat: its target large, the current reading underneath, and −/+ to nudge it. Choose
  which thermostats appear under **Settings › Android Auto & Wear OS › Watch thermostat** — pick
  several and tap the name on the tile to move between them.

Add either from the watch: long-press the watch face, swipe to **+**, and pick it.

Tiles refresh on their own schedule rather than following Home Assistant live, because they update
with nobody looking at them — a connection held open for that would cost battery around the clock.
A change made on the phone reaches them straight away.

### Complication

A watch-face complication shows the state of your first quick action. Long-press the watch face,
choose **Customise**, tap a complication slot and pick HKI 7. It shows a dash when Home Assistant
cannot be reached, rather than a number that quietly stopped being true.

### Watch battery in Home Assistant

**Settings › Home Assistant › Report watch battery** on the watch adds it to Home Assistant as its
own device, so its battery can drive automations — *remind me to charge my watch*.

Off until you turn it on, and battery is all that is sent. A watch is full of sensors; none of the
others leave your wrist.

## Which devices

| | |
|---|---|
| **Android Auto** | Any car or head unit that supports it. HKI 7 must be installed from Google Play |
| **Wear OS** | Wear OS 3 and later. Sideloading works |

The watch app is delivered by Google Play alongside the phone app — installing HKI 7 on your phone
offers it for a paired watch.
