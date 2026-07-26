# Changelog

The release notes shown in the app's "What's new" dialog, newest first. Keep this
file in sync with `app/src/main/java/com/jimz011apps/hki7/ui/components/WhatsNewDialog.kt`.

Items marked with \* require the HKI 7 Cloud Component integration.

## 1.0.0-beta.6

- Fixed: in edit mode, the Add Widget and Add Room / Floor bars no longer overlap the system navigation buttons when you use three-button navigation instead of gesture navigation.

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
