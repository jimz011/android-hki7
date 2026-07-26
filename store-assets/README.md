# Store assets

Graphics for the Google Play store listing. These are **not** bundled into the
app — Play Console hosts them separately from the APK/AAB, so updating the
launcher icon in `app/src/main/res` does not change what the store shows.

## Files

- `play_store_icon_512.png` — the Play Store app icon. 512×512, 32-bit PNG,
  fully opaque (no alpha); Play rounds the corners itself. The mark is sized to
  ~60% of the tile so it matches the on-device launcher icon's visible size
  (the launcher crops to the central 72dp of the adaptive icon, but Play shows
  the full square).
- `play_feature_graphic_1024x500.png` — the Play Store feature graphic (the
  banner at the top of the listing / used for promos). 1024×500, 24-bit PNG,
  no alpha. Uses the same blue gradient as the icon, with the house mark and the
  "HKI 7" wordmark + tagline. Keep important content away from the edges — Play
  can crop it and may overlay a play button when a promo video is set.

## Updating the store icon

Play Console → the app → **Grow → Store presence → Main store listing** →
**App icon** → upload `play_store_icon_512.png` → **Save**. The feature graphic
lives on the same screen under **Feature graphic**. Keep both in sync with
`app/src/main/res/drawable-*/ic_launcher_foreground.png` and
`ic_launcher_background.xml` whenever the launcher mark or gradient changes.
