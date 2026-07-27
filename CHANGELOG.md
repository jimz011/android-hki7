# Changelog

The release notes shown in the app's "What's new" dialog, newest first. Keep this
file in sync with `app/src/main/java/com/jimz011apps/hki7/ui/components/WhatsNewDialog.kt`.

Items marked with \* require the HKI 7 Cloud Component integration.

## 1.0.0-beta.8

- Child locks are never auto-imported. A washer, air conditioner, or valve's keypad/child-lock is a safety toggle, not a room control, so it no longer shows up among your switches.
- Vacuums now appear in a room's badge bar. They sit on the left next to your cameras (or on their own on the left when there's no camera), and tapping one opens its controls.
- Hide or schedule individual buttons in a multi-item widget. In a stack's "Manage items" screen, each item has a visibility button: hide it until you unhide it, or pick a date/time window for when it shows or hides — with a graphical date-and-time picker. The window can repeat daily, weekly, monthly, or yearly, so, for example, a set of Christmas buttons appears every 24–26 December without touching the year. The schedule travels with the dashboard, so cloud backups and family sharing keep it.
- Header badges with several cameras now open one paged dialog to flip through them, matching how blinds and vacuums already aggregate.
- Family Sharing › Dashboards now lists every dashboard you've published to your family — even after reinstalling the app, because they live in the cloud. Import one to edit or re-share it, or delete it to remove it from everyone. When a shared dashboard is deleted, anyone still using it falls back to an auto-generated dashboard the next time they open the app.
- Individual buttons within a multi-card widget can now be hidden or scheduled. In the sort menu, hide a button until you unhide it, or set a visible/hidden window — as a one-off or a recurring daily, weekly, monthly, or yearly schedule (so a set of Christmas buttons can appear every December on its own).

## 1.0.0-beta.7

- Cleaner automatic room import. When a device's main purpose is a light, thermostat, blind, fan, humidifier, lock, or vacuum, HKI no longer clutters the room with the device's built-in helper controls — its own child lock, the little status light on an air conditioner or blind, and its sleep or display switches are all skipped. Genuine extras are still kept, like a ceiling fan's separate light or a standalone smart door lock.
- Climate devices from different integrations are no longer merged together. A room that used to collapse every thermostat and AC into one combined tile now gives each integration its own control — your Tado thermostats stay together, and a Tuya AC gets its own tile — so you can operate them separately.
- Climate devices now show an icon that matches what they actually are: air conditioners (units that cool but don't heat) get an AC icon, humidifiers and dehumidifiers get a humidifier icon, and fans recognised as air purifiers get a purifier icon — instead of everything sharing the generic thermostat or fan icon.
- Air purifier and humidifier icons now pulse gently instead of spinning while the device is running. A spinning purifier or humidifier looked wrong, so they use the same calm pulse as other climate devices.
- Door locks now use door-shaped icons — a closed, locked door when locked and an open door when unlocked — instead of a plain padlock, so a room's lock state is clearer at a glance.
- Header badges that summarise several devices at once can now be reordered. Open the badge's settings to choose which entity leads — it decides the badge's icon, the state it shows, and the order of the pop-up list.
- Animated icon effects are now on by default for new installs, so a fresh setup feels alive out of the box. Existing setups keep whatever you already chose, and you can turn effects on or off any time under Settings › Appearance › Icons.
- Buttons and header badges can now show one of an entity's attributes instead of its state — for example a sensor's battery level or a media player's current source — and you can add a unit like °C, %, W, or kW to the shown value. Open the item's settings and pick State or one of the listed attributes. (The unused per-button Label field has been removed.)
- You can now copy a dashboard. In Settings › Dashboards, tap the copy icon on any dashboard to create a full duplicate — every room, widget, and page setting included — as a starting point for a new layout.
- New Settings › Family Sharing section brings parental controls, family dashboard sharing, and per-user permissions together under three tabs. Admins can set per person whether they can edit at all, whether they're limited to aesthetic changes only (icons, names, layout, wallpaper — no adding or removing widgets, buttons or rooms), and whether they see the global search and flows buttons. Everyone else sees it locked, and can choose to use a dashboard an admin shared with them — including during onboarding.
- Shared dashboards now update automatically. When the owner pushes a change, it's pulled in the next time you open the app — while keeping your own aesthetic tweaks (icons, names, layout, wallpaper) intact.
- Non-admin family members now get the automatic per-room Adaptive Lighting controls too (previously only admins saw them), when the HKI 7 Cloud component is installed.
- Fixed: in light mode with the default (system) header, the header title/subtitle and the pull-down header menu labels (Edit, Home Settings, Settings) were near-white and unreadable. Their color is now taken from the surface the text actually sits on, so both the header and the pull-down menu are readable on every theme, custom color, and wallpaper.
- The Energy view's hero card label now reads "Power" instead of "Home Power".
- Settings › About now has "What's new" (opens the release-notes dialog) and "Full changelog" (opens the changelog on GitHub) buttons.
- Fixed: the "Unable to connect" screen — with its Refresh and Log in again buttons — now appears reliably when Home Assistant can't be reached. In some cases, such as signing in on a trusted network and then leaving Wi-Fi, the app used to keep showing a stale dashboard instead of offering to reconnect.

## 1.0.0-beta.6

The notes below repeat beta.4/beta.5 so anyone updating straight from beta.3
still sees those changes.

- Fixed: in edit mode, the Add Widget and Add Room / Floor bars no longer overlap the system navigation buttons when you use three-button navigation instead of gesture navigation.
- The Backup and Restore screen now shows when each automatic backup last ran — both Google Drive and Home Assistant — so you can tell at a glance that your latest changes are safely saved.
- Added a "Back up now" button to each cloud backup, so you can create an immediate backup any time without waiting for the daily schedule.
- Fixed: the app icon looked oversized after the recent themed-icon (monochrome) update. The mark now sits with proper padding, matching other app icons on your home screen and in the themed-icon style.

## 1.0.0-beta.5

Icon and branding refresh only — the notes below repeat beta.4 so anyone updating
straight from beta.3 still sees those changes.

- The Backup and Restore screen now shows when each automatic backup last ran — both Google Drive and Home Assistant — so you can tell at a glance that your latest changes are safely saved.
- Added a "Back up now" button to each cloud backup, so you can create an immediate backup any time without waiting for the daily schedule.
- Fixed: the app icon looked oversized after the recent themed-icon (monochrome) update. The mark now sits with proper padding, matching other app icons on your home screen and in the themed-icon style.

## 1.0.0-beta.4

- The Backup and Restore screen now shows when each automatic backup last ran — both Google Drive and Home Assistant — so you can tell at a glance that your latest changes are safely saved.
- Added a "Back up now" button to each cloud backup, so you can create an immediate backup any time without waiting for the daily schedule.
- Fixed: the app icon looked oversized after the recent themed-icon (monochrome) update. The mark now sits with proper padding, matching other app icons on your home screen and in the themed-icon style.

## 1.0.0-beta.3

- Added three new icon packs alongside Material Design Icons: Simple Icons (brand & service logos like Spotify and Philips Hue), Tabler, and Phosphor. Switch packs right in the icon picker when choosing an icon for any button.
- New: animated icons. Entity icons can gently glow, spin, or pulse while the device is active — lights glow, fans and vacuums spin, playing media and active climate pulse. Only active devices animate; turn it on and tune the effects under Settings › Appearance › Icons.
- New: family dashboard sharing. An admin can share a dashboard with specific family members (or everyone), and they import it into their own app — no more passing backup files between phones.\*
- New: parental controls. Admins can hide certain views and rooms from specific people, keeping a dashboard simple for kids or guests (UX-level hiding, not a Home Assistant security restriction).\*
- New: automatic local cloud backup. Back up your dashboard and appearance settings to your own Home Assistant every day, and restore them any time.\*
- Media player source icons are now colored, and tapping the app logo opens the installed app — for example, if music is playing from Spotify, tapping the Spotify logo in the media player bar opens the Spotify app.
- New widget: iFrame — embed any web page on your dashboard.
- Fixed: Energy views now show negative values when you're exporting power.
- Fixed: onboarding could auto-complete when tabbing out of the app before finishing.
- Fixed: the thermostat dial's mode-selection button was too large and overlapped nearby items.
- Many other smaller bug fixes.

## 1.0.0-beta.2

- Fixed an issue with smaller displays when either the height was too small or the width too narrow (or both), the content would look bad. It now falls back to a single column design on smaller screens. This has been changed across all elements.
- Dialog headers now show 2 rows on narrower screens so that the title no longer cuts off.
- Fixed an issue where light sliders that have adaptive lighting feature would overlap other elements.
- Added visual to nav bars if there is more content on the navbar than the screen can display.
- Fixed an issue where camera's wouldn't respect screen orientation lock.
- Fixed an issue where we could not zoom in on camera dialogs or full-screen camera's.
