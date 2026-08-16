# Gestures

HKI 7 keeps its chrome out of the way, which means a few things live behind a gesture rather than
behind a visible button. There are not many, and the app teaches them on first launch.

## The quick-start guide

The first time your dashboard is ready, a **Quick start** dialog appears: *"Five simple gestures
are all you need to get around."* Each one is shown with an animation of the movement, and you
page through them before tapping **Got it, let's explore**.

If you skipped past it, everything it covers is below.

## The five

### Notifications

**Swipe right from the upper-left edge.**

Opens the notification panel — unread, history, archived, and the
[Events timeline](../guide/events.md).

[:octicons-arrow-right-24: Notifications](../guide/notifications.md#the-notification-panel)

### Switch homes

**Swipe left from the upper-right edge of the header.**

Cycles between your Home Assistant instances. Works from any page header, at any time. Only
relevant if you have added more than one home.

[:octicons-arrow-right-24: Multiple homes](../guide/multiple-homes.md)

### Quick actions

**Pull down on any page header.**

Reveals **Search**, **Flows**, **Edit** and **Settings**. This is the main one to remember — it is
how you reach edit mode and settings from anywhere, and it works on the compact header too.

Several empty screens tell you this directly: an empty Rooms page says to swipe down on the header
and enable edit mode; an empty Security or Energy view says to swipe down and open its settings.

### Media player

**Swipe up on the bottom bar**, once the handle appears.

Restores the media player after you have dismissed it. The handle only shows when there is a
player to bring back.

### Make it yours

**In edit mode: drag to reorder, tap to configure.**

Add and reorder rooms, floors and widgets by dragging them; tap any item to open its settings.

[:octicons-arrow-right-24: Dashboards](../guide/dashboards.md#edit-mode)

## Also worth knowing

**Swipe horizontally to change page**

: Left and right anywhere on a page moves between your pages — Home, Rooms, and any custom pages,
  in the navigation bar's order.

    The gesture is deliberately forgiving: it locks to an axis once your finger commits, so a
    vertically scrolling list cannot swallow a horizontal swipe, and it navigates as soon as you
    have travelled far enough rather than waiting for you to lift. A quick flick works too.

    Controls that legitimately need horizontal movement — sliders, nested pagers, horizontal rows
    — keep the gesture for themselves, so dragging a brightness slider never pages the dashboard.

    Opening a view's own page — Solar inside Energy, a group inside Security — suspends paging
    until you leave it, so a sideways swipe cannot carry you out of what you just opened.

    Tabs inside dialogs are changed by tapping them. They do not swipe: a tab strip that scrolls
    and a page that swipes were competing for the same drag.

**Back**

: Back steps through the pages you actually visited rather than jumping to the first one, and
  leaves the app once there is nothing behind the page you are on.

**Tap the tab you are already on**

: Returns that view to the top. Views otherwise keep their scroll position indefinitely — there is
  no timer after which they forget where you were.

**Swiping stacks**

: A [swiping stack](../guide/widgets.md) holds several widgets in one slot; swipe horizontally
  through them. They can also autoplay on a timer.

**Weather season toggle**

: Where the app shows *Astronomical* or *Meteorological*, you can tap **or** swipe to switch
  between them.

## If a swipe is not registering

- **Edge swipes need to start at the edge.** The notification and switch-home gestures both begin
  from the very edge of the screen, in the *upper* portion of it.
- **Android's own back gesture uses the same edges.** On devices with gesture navigation, starting
  too far from the top can trigger the system back instead. Start higher up.
- **Horizontal controls win.** A swipe that starts on a slider adjusts the slider; it does not
  page. Start the swipe on empty space or on a plain card.
