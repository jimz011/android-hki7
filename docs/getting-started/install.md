# Install HKI 7

## Requirements

- **Android 14 or newer.** HKI 7 targets a modern Android baseline (`minSdk 34`) so it can rely on
  current Compose, notification and location APIs without carrying compatibility shims.
- **A Home Assistant instance you can reach** from the phone — on the local network, at a remote
  URL, or through Home Assistant Cloud (Nabu Casa).
- **Google Play Services** if you want geofence-based presence, Google Drive backups, or in-app
  updates. The app runs without them; those specific features do not.

Family sharing has one extra requirement — the
[HKI 7 Cloud](https://github.com/jimz011/HKI7-Cloud-Component) integration on your Home Assistant.
See [Family sharing](../guide/family-sharing.md).

## Where to get it

=== "Google Play"

    HKI 7 is published on Google Play as **HKI 7** (`com.jimz011apps.hki7`). Installing from Play
    is what makes in-app updates work, including the family "please update" prompt described in
    [Family sharing](../guide/family-sharing.md#devices).

=== "Build it yourself"

    The community source is on
    [GitHub](https://github.com/jimz011/android-hki7) under MPL-2.0. Open the project in a recent
    Android Studio and run the `app` configuration, or from a terminal:

    ```bash
    ./gradlew assembleDebug
    ```

    The debug APK lands in `app/build/outputs/apk/debug/`. See
    [Contributing](../contributing.md) for the full development setup.

    !!! warning "Sideloaded builds and updates"

        A copy that did not come from the Play Store cannot use Play's in-app update flow. If your
        family admin requires a minimum version, you will get an explanation and a way past the
        prompt for that session rather than a working update button.

## Try it without a server

You do not need a Home Assistant instance to look around. On the welcome screen, choose
**Try the demo home — no server needed**.

The demo runs entirely on the device against an in-memory sample house. Everything else in the app
behaves normally: dashboards auto-generate, states update in real time, and service calls appear to
work. Nothing is sent anywhere, because there is nowhere to send it.

To leave the demo, open **Settings › Connection** and add a real Home Assistant instance.

## What happens on first launch

HKI 7 stores nothing about you before you tell it something. On first launch it:

- Reads no location, contacts, or accounts.
- Asks for no permissions until the permissions step, which you can skip entirely.
- Creates no server registration until you have signed in and named the device.

The details of what the app collects and where it goes are in the
[Privacy policy](../reference/privacy.md).
