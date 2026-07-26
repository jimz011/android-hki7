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

## Updating the store icon

Play Console → the app → **Grow → Store presence → Main store listing** →
**App icon** → upload `play_store_icon_512.png` → **Save**. Keep this file in
sync with `app/src/main/res/drawable-*/ic_launcher_foreground.png` whenever the
launcher mark changes.
