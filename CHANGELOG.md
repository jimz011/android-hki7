# Changelog

The release notes shown in the app's "What's new" dialog, newest first. Keep this
file in sync with `app/src/main/java/com/jimz011apps/hki7/ui/components/WhatsNewDialog.kt`.

Items marked with \* require the HKI 7 Cloud Component integration.

## 1.0.0-beta.12

- Button widgets and button stacks can hold an empty, fully transparent button. It takes up a grid cell without drawing anything, so the buttons around it line up exactly where you want them. Add one from a stack's Add-entity screen or from the widget picker under Layout.
- Buttons can show just their icon. The new "Icon only" switch under a button's Appearance settings drops the name and state and centers a larger icon on the button, in all three button styles.
- New "Custom popup" action for buttons, badges, and dialog buttons. Point any tap, hold, or double tap at a popup you build yourself: it opens with the usual dialog title, status, history, and close controls, and you fill it with any widget the app offers. Popups are listed under Settings › Appearance › Popups, where "Edit contents" opens one ready to arrange, and they are shared, so several buttons can open the same one. Giving a popup a status entity fills its status line and enables the history view. While a popup is being arranged, a Done button sits beside Add widget to leave edit mode without closing the dialog, and stacks inside a popup can use up to six columns instead of the three a dashboard page allows.
- Fixed the Energy detail views (Solar, Electricity, Gas, Water, City heating, Battery) drawing their cards flush against each other with their headings missing. Each card and its heading were two separate grid items, which the layout stacked on top of one another, so "Production", "Forecast", "Inverters" and the rest were hidden behind the card below them. The cards now carry their headings and the same spacing as everywhere else.
- Fixed Parental Controls refusing to save. An out-of-date HKI 7 Cloud component rejects the Visible and Invisible lists, which previously failed the whole policy: the permissions now save regardless, and the message says the component needs updating instead of claiming nothing was saved. The Visible and Invisible lists only ever filter entity lists such as global search — a button on a family dashboard stays visible unless it is hidden for that person explicitly.\*

## 1.0.0-beta.11

- Rooms, Climate, Security, Energy, and Battery dashboards now make better use of tablets and foldables. Large cards follow the Home and individual-room 1/2/3-column layout, while compact tiles auto-fit without becoming cramped. Climate, Security, and Energy cards use a height-aware masonry layout on wider displays so shorter cards no longer leave large empty gaps.
- Climate sensor/device tabs, Security groups, and every Energy detail tab now follow the same responsive one-, two-, or three-column masonry rules, including their edit and reorder layouts.
- State-colored icons on entity buttons and header/status pills now preserve their semantic hue while automatically shifting lighter or darker when they would blend into the current theme.
- Selecting a family dashboard during onboarding now subscribes that installation to it. Owner edits are published when Save or Done is tapped, active clients refresh immediately, and offline clients reconcile when reopened. Non-admin subscribers cannot create or duplicate dashboards unless the app is fully reset.\*
- Dashboard setup during onboarding can now restore a local, Google Drive, or Home Assistant backup. New Google Drive and Home Assistant backups use consistent, informative names containing the app version and creation date.\*
- Calendar widgets become vertically scrollable when their height is too small for every event, and hidden Home widgets are removed from the layout instead of leaving an empty space.
- Parcel carrier cards now show the earliest expected delivery date and time for active incoming or outgoing shipments, including integrations that expose a separate next-delivery sensor.
- Family permissions can now independently allow dashboard switching and creation. Fully restricted subscribers see a locked Dashboard settings tab; onboarding removes its placeholder dashboard when switching is denied, and a deleted family dashboard opens a permission-aware recovery screen instead of silently creating a replacement.\*
- Family permissions now also control manual re-importing. Non-admins cannot open Family Sharing settings, disabled onboarding choices explain why they are unavailable, and aesthetics-only editors see only visual controls. Their local names, icons, colors, backgrounds, styles, sizes, and layouts take priority when owner updates arrive.\*
- Aesthetics-only family editors can no longer change header-pill configuration or the dashboard's people; those functional settings remain controlled by the owner.\*
- Parental Controls now replaces manual entity-ID entry with Visible and Invisible lists. Admins can select complete Home Assistant domains or individual entities, and global search hides restricted entities and devices. Invisible entries always take precedence.\*
- Energy setup now imports SmartGateways and DSMR Reader P1 meters even when the Home Assistant Energy dashboard has not been configured and the sensors are not attached to a device — power, both import and export tariffs, per-phase power, current and voltage, and gas are recognised from their DSMR names. Related-entity discovery falls back to the integration's config entry when a device is missing, and DSMR's "delivered" is read as grid import while "returned" is read as export. Usage charts and period totals add tariff 1 and tariff 2 together when no single grid import or export sensor is set, and re-importing no longer clears device lists that Home Assistant does not provide.

## 1.0.0-beta.10

- Visibility rules now work across every widget and individual widget items, with one-off or recurring time schedules, entity-state conditions, per-user visibility, and AND plus OR freely mixed in the same rule set. Admins using a family-shared dashboard can add a Person condition directly to an item; it matches the signed-in Home Assistant user, offers Visible or Hidden behavior, and shows the HKI 7 Cloud Component requirement when the integration is unavailable. The visibility editor has its own tab, and a new conditional setup starts empty so you can explicitly choose Time, Entity, or Person.
- Room-status counter badges now reserve a consistent two-digit width instead of resizing when a count passes nine. Rooms and individual-room headers responsively fit one, two, or three counters per row, matching the home-page and person-badge behavior without pushing the title down.
- Energy setup now recognises SmartGateways and DSMR Reader MQTT naming, including consumption/production, delivered/returned, Dutch terminology, and alternate tariff spellings. Selecting a source device refills empty sensor roles while retaining existing overrides. City heating now has separate settings, a live source tile and detail tab, accepts J/kJ/MJ/GJ/Wh/kWh/MWh readings, and shows an estimated natural-gas equivalent based on 35.17 MJ per m³.
- Calendar widget dialogs now use the full available height, fixing agenda and settings content that previously occupied only part of the dialog or was cut off.
- Re-import settings now use one consistently spelled and localized label across Rooms, Climate, Security, Energy, and Battery. Conditional visibility terminology was also corrected across all supported languages.
- Fixed PostNL and the current parcel-integration parser, so incoming shipments once again show their complete tracking details and delivered state. Carrier support has also been brought up to date.
- Added an in-app language selector with full coverage in 13 languages: Dutch, German, French, Spanish (plus Latin American Spanish), Italian, Turkish, Portuguese (plus Brazilian Portuguese), Japanese, Korean, and Chinese (Simplified and Traditional). Each language shows its native name with an English hint beneath it.
- The reconnect status now shows the actual connection error, making Wi-Fi, mobile-data, and VPN handovers much easier to diagnose.
- Screen rotation now follows your device's auto-rotate setting instead of staying locked to portrait.
- The Climate "Outside" tile now shows up as soon as a weather device is linked, even without dedicated temperature/humidity/pressure sensors, counts those weather attributes in its summary, and graphs them with matching sensor colors.
- Parcel status codes from carriers (e.g. "PARCEL_ARRIVED_AT_LOCAL_DEPOT") are now shown as normal sentences. Parcel dates and times follow the selected language and 12/24-hour preference, nearby deliveries use weekday names, the current year is omitted, and same-day windows no longer repeat the date.

## 1.0.0-beta.9

- Drag to reorder cameras, thermostats, and energy cards. In edit mode the Security, Climate, and Energy views each get a "Reorder" button that opens a drag-to-sort list — cameras on Security, thermostats/AC units on Climate, and the data cards below the pinned live-source tiles (Electricity, Solar, Gas, Water, Top consumers, and more) on Energy.
- New Climate "Outside" tile. Link a weather entity or choose outside temperature, humidity, and air-pressure sensors under Climate settings. The climate hero shows the outside temperature beneath the indoor average, and opening Outside gives each value its own history graph.
- Hide/schedule moved onto the item itself. Instead of a separate control in the reorder list, open a button's (or header badge's) settings and use the new "Visibility" section under Appearance to hide it or schedule when it shows — one-off or recurring daily/weekly/monthly/yearly.
- Header badges can be renamed. Give a badge a custom label instead of the entity's name, in its Appearance settings.
- Swipe cards autoplay by default, advancing every 3 seconds.
- Weather dialog: the Season card now autoplays (flipping every 3 seconds), a new Wind card shows a compass windrose with speed and direction, and you can add a Rain map card backed by a camera entity or embedded web page (iframe), with selectable aspect ratios and full-bleed rounded presentation. The per-card "dialog layout" settings were removed.
- The Parcels widget has been rebuilt around your carriers. It shows bundled carrier logos, totals across every account, incoming/delivered/outgoing/mail sections, delivery estimates, and tracking history. Supported carrier integrations can also add a parcel directly by tracking number.
- Humidifiers and dehumidifiers now get full native controls from Climate, room buttons, and header badges: power, target/current humidity, distinct mode tabs, linked fan/select speed controls, and optional tank, filter, error, PM2.5, ionizer, pump, sleep, and beep helpers. Related entities are filled automatically from the same Home Assistant device.
- Room status counters are now interactive. Tap a counter to see the lights, switches, doors, windows, motion sensors, or other entities behind it. Visible room lights and switches are counted automatically, and controls owned by counter devices no longer clutter the room as duplicate buttons.
- Buttons and badges backed by a completion-time entity now show a live countdown while the appliance is running, making washing machines, dryers, ovens, and similar devices easier to follow.
- Shared dashboards now publish the owner's latest edits automatically when the app opens. Recipients get those updates on their next sync while keeping their own allowed appearance changes.
- Header display settings — including weather/alarm/date-time pills, linked entities, and the rain map — now belong to each dashboard, so they travel correctly through backups and family sharing.
- Camera widgets now fill their whole tile (no more black bars top and bottom) and use the same corner radius as every other widget.
- Waste collection is easier to scan: collections on the same day use overlapping fraction icons instead of hiding one another, the widget icon styling now matches the rest of the app, and its larger dialog can be dismissed by tapping outside.
- More visual consistency: Battery and Waste artwork use matching rounded icon tiles, camera and weather overlays follow the configured item corners, and About now shows the current HKI 7 app icon.
- Binary sensors now use Home Assistant-style labels such as Open/Closed, Wet/Dry, Detected/Clear, and Connected/Disconnected instead of generic On/Off text.
- Restricted (aesthetic-only) editors can no longer add, remove, or reconfigure header badges — only visual tweaks (name, icon, visibility) to existing ones.
- Fixed: switching between Wi-Fi, mobile data, or a VPN now discards sockets tied to the old Android network and reconnects with the saved Home Assistant session, instead of unnecessarily requiring another login. While reconnecting, the connection toast now shows the actual failure reason instead of only a generic status.

## 1.0.0-beta.8

- Child locks are never auto-imported. A washer, air conditioner, or valve's keypad/child-lock is a safety toggle, not a room control, so it no longer shows up among your switches.
- Vacuums now appear in a room's badge bar. They sit on the left next to your cameras (or on their own on the left when there's no camera), and tapping one opens its controls.
- Hide or schedule individual buttons in a multi-item widget. In a stack's "Manage items" screen, each item has a visibility button: hide it until you unhide it, or pick a date/time window for when it shows or hides — with a graphical date-and-time picker. The window can repeat daily, weekly, monthly, or yearly, so, for example, a set of Christmas buttons appears every 24–26 December without touching the year. The schedule travels with the dashboard, so cloud backups and family sharing keep it.
- Header badges with several cameras now aggregate into one dialog you swipe through, with page dots, exactly like the blinds and vacuum stacks — and every page keeps its live stream and fullscreen button (you can page in fullscreen too).
- Every button now picks up the icon you set in Home Assistant, not just lights. HKI now reads each entity's icon override from the Home Assistant entity registry (where HA stores it) and applies it across all buttons and badges, for every domain.
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
