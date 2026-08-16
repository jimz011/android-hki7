# Appearance

**Settings › Appearance** covers how the app looks — colour, typography and component shape. What
the dashboard is *made of* (dashboards themselves, the navigation bar, media players and popups)
lives under **Settings › Dashboard** instead.

## Theme

**Mode**

: **System**, **Light** or **Dark**.

**Colour**

: **System** (Material You, from your wallpaper), **Rose**, **Green**, **Blue**, **Amber**, or
  **Custom**. With Custom you set the light and dark schemes yourself.

## Fonts

**Font size**

: Scales the app's text independently of the Android system font size.

**Boldness**

: Six steps, from **Thinner (−200)** through **Default** to **Boldest (+300)**. This shifts the
  weight of the whole type scale rather than bolding individual labels.

**Font family**

: With a live preview line so you can see what you are choosing before you commit.

## Language

HKI 7 ships in English plus 35 translations: Dutch, German (also Austrian and Swiss), French, Spanish (also Latin American and
Mexican), Italian, Portuguese, Brazilian Portuguese, Turkish, Japanese, Korean, Simplified
Chinese, Traditional Chinese, Norwegian, Swedish, Danish, Finnish, Estonian, Latvian,
Lithuanian, Polish, Czech, Slovak, Hungarian, Romanian, Bulgarian, Croatian, Greek, Russian,
Thai, Arabic and Hebrew.

Arabic and Hebrew mirror the whole interface right to left, rather than only reversing the text.

By default the app follows the system language. Pick a specific one under **Settings › Appearance
› Language** to override that — the list has a search box, and keeps "Follow the device" and your
current language pinned at the top so there is always a way back if you pick one by accident.

!!! note "Entity names come from Home Assistant"

    Changing the app language translates HKI 7's own interface. Your entity and area names are
    whatever Home Assistant calls them.

## Corners

One global roundness setting — **Sharp**, **Modern** or **Round** — applied to every dashboard
button, widget, stack and room card. Individual items can override it with their own corner
radius.

## Icons and animation

**Animated icons**

: Entity icons gently glow, spin or pulse while the thing they represent is active — a light that
  is on, a door that is open, a camera that is recording. Each button can override the global
  setting with `auto`, off, or a forced **glow** / **spin** / **pulse**.

**Weather animation**

: Weather artwork is animated Lottie, and where it animates is **four separate switches** — the
  header pill, the weather dialog, the forecast strips, and the weather widget. They are separate
  because the cost is wildly uneven: the forecast strips run a dozen compositions side by side and
  are the only surface that ever caused stutter, so switching those off need not mean losing the
  single small icon in the header pill.

    All four are on by default, and appear both here and in the header pill's own settings sheet.

!!! info "Two different things"

    Entity-icon animation and weather animation used to be one setting. They are not the same
    thing — one is icons glowing and spinning, the other is Lottie weather artwork — so they are
    now controlled separately.

### Icon packs

The complete **Material Design Icons** and **Simple Icons** sets are bundled as webfonts, with
lookup tables in the app's assets. Icons render with no network dependency, and the picker
searches both packs.

## Header

The dashboard header comes in two forms:

**Full**

: The expanded header, with the left header pill, the persons subtitle, and the right header pill.

**Compact**

: Keeps only the title, the right header pill and the back affordance. Compact mode hides the left
  header pill, the persons subtitle and status information, and the room counters — that is what
  makes it compact.

    The gestures still work: swipe down on the compact bar to open Search, Flows, Edit and
    Settings. [:octicons-arrow-right-24: Gestures](../getting-started/gestures.md)

## Force high refresh rate

Locks the screen to its highest refresh rate while HKI 7 is in the foreground. Some devices drop
to 60 Hz for apps they consider static; this stops that at the cost of a little battery.
