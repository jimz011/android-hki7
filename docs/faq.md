# FAQ

## About the app

??? question "What is HKI 7?"

    An Android client for [Home Assistant](https://www.home-assistant.io/), built with Jetpack
    Compose and Material 3. It connects to your own Home Assistant server and gives you a
    dashboard, per-domain controls, notifications, presence and optional family sharing.

    It is an independent project and is not affiliated with or endorsed by Home Assistant or Nabu
    Casa.

??? question "Do I need Home Assistant to try it?"

    No. There is a built-in demo home that runs entirely on the device — pick **Try the demo
    home** on the welcome screen. Everything behaves normally against an in-memory sample house.

??? question "Does it replace the official companion app?"

    It does the same job and more of it, but they are separate apps and can be installed side by
    side. HKI 7 registers as its own mobile_app device, so it gets its own
    `notify.mobile_app_<device>` service and its own device tracker — the official app's stay
    where they are.

??? question "Which Android versions are supported?"

    Android 12 and newer (`minSdk 31`).

??? question "Is it free? What is Premium?"

    The app and its community source are free and open. HKI 7 uses an **open-core** model: the
    community source code is MPL-2.0, while separately marked premium materials — premium icon
    packs, animated icon and artwork collections, premium themes — are proprietary and paid.

    The open-source core stays fully usable without Premium. No Premium term limits the rights
    MPL-2.0 grants you over the community source.

??? question "Is HKI 7 open source?"

    The community core is, under [MPL-2.0](https://www.mozilla.org/MPL/2.0/), at
    [jimz011/android-hki7](https://github.com/jimz011/android-hki7). Premium assets are not. See
    [LICENSE](https://github.com/jimz011/android-hki7/blob/main/LICENSE).

??? question "Was this built with AI?"

    Yes — with the help of AI, including Claude and OpenAI models. If you dislike AI being used in
    projects, then do not install this.

## Privacy and data

??? question "Where does my data go?"

    To your own Home Assistant server, and nowhere else. There are no developer-operated servers,
    no analytics, no advertising and no tracking. The developer never receives, sees or stores any
    of your data.

    [:octicons-arrow-right-24: Privacy policy](reference/privacy.md)

??? question "What about Google Drive backups?"

    Only if you turn them on. They go to the private app-data area of **your own** Google Drive,
    which only HKI 7 and you can read. Disable them at any time, or revoke the app's access from
    your Google account settings.

??? question "Does family sharing send anything to the internet?"

    No. The HKI 7 Cloud component stores everything on your own Home Assistant. "Cloud" is the
    feature's name, not its architecture.

## Setup

??? question "Can I connect to more than one Home Assistant?"

    Yes. Each home keeps its own login, dashboard, notification settings and location settings.
    Swipe left from the upper-right edge of the dashboard to switch.

    [:octicons-arrow-right-24: Multiple homes](guide/multiple-homes.md)

??? question "Do I need Nabu Casa?"

    No. Home Assistant Cloud works and is the easiest remote setup, but a plain external URL or a
    local-only setup are both fine.

??? question "Why set both an internal and an external URL?"

    So the app uses the LAN address at home and the remote one everywhere else. Hairpinning out to
    the internet and back in is slower and, on some routers, does not work at all — which matters
    most for camera streams.

    [:octicons-arrow-right-24: Local and remote access](getting-started/connect.md#local-and-remote-access)

??? question "Does HKI 7 read my Home Assistant dashboards?"

    No. It builds its own dashboard from your areas, floors, devices and entities. Your Lovelace
    configuration is untouched, and nothing you do in HKI 7 changes it.

## Dashboards

??? question "I added an entity in Home Assistant and it did not appear. Why?"

    Auto-generation is a one-time starting point. The first edit you make switches the dashboard
    from **Automatic** to **Manual**, after which HKI 7 stops regenerating it and your arrangement
    stays put. Add new entities yourself in edit mode, or reach them through global search.

??? question "How do I get to edit mode / settings? I cannot find a button."

    **Pull down on the page header.** That reveals Search, Flows, Edit and Settings, from any page
    — including the compact header.

    The app shows all five gestures in a Quick start dialog the first time your dashboard opens.
    They are written down in [Gestures](getting-started/gestures.md).

??? question "Can I write YAML?"

    No. Everything is edited in the app, in place. There is no YAML mode.

??? question "Can I show a button only sometimes?"

    Yes — by schedule (including yearly recurrence, for a Christmas button), by another entity's
    state, or by which family member is signed in. Rules combine with AND/OR.

    [:octicons-arrow-right-24: Visibility rules](guide/dashboards.md#visibility-rules)

??? question "Can I stop someone triggering a button by accident?"

    Lock it behind a double-tap or a PIN, with a configurable re-lock delay.

## Notifications

??? question "How do I send a notification?"

    Call the notify service for the device, from any automation or script:

    ```yaml
    service: notify.mobile_app_<device_name>
    data:
      title: Doorbell
      message: Someone is at the front door
    ```

??? question "Do my existing notification automations work?"

    The common fields do: `title`, `message`, `data.tag`, `data.channel`, `data.clickAction`,
    `data.sticky`, `data.actions`, and the `clear_notification` message. Action taps fire the same
    `mobile_app_notification_action` event the official app fires, so automations written for it
    work unchanged.

??? question "Do I need Firebase or Google's push service?"

    No. Notifications ride the app's own connection to your server. There is no third-party push
    service in the path.

??? question "Why does a notification say the app is connected?"

    Android requires a visible notification for a persistent background connection. It is on its
    own channel, so you can turn just that channel off and keep the connection.

    [:octicons-arrow-right-24: The connection notification](guide/notifications.md#the-connection-notification)

## Presence

??? question "Will presence drain my battery?"

    It is designed not to. HKI 7 uses geofences and a batched WorkManager job rather than
    continuous tracking — the same model the official app uses. The radio only wakes on a zone
    crossing.

    High accuracy mode is the exception, and it says so.

??? question "Can I use the app without location at all?"

    Yes. Decline every location permission and everything except the device tracker works.

## Family sharing

??? question "Can I hide entities from my kids?"

    You can hide views, rooms and individual items from specific people in the app's interface.

    That is **not** a security boundary. Home Assistant has no per-entity read permission for
    non-admin users, so anyone in the household can still reach those entities through Home
    Assistant directly. Treat it as decluttering, not as a lock.

??? question "Who counts as an admin?"

    Whoever is an owner or administrator on your Home Assistant. HKI 7 does not maintain its own
    account system.

??? question "Do I have to pass backup files around to share a dashboard?"

    No, that is exactly what family sharing removes. An admin publishes once and everyone pulls
    it; changes are pushed to the same published copy.

## Miscellaneous

??? question "What languages does it speak?"

    English plus 35 translations: Dutch, German (also Austrian and Swiss), French, Spanish (also
    Latin American and Mexican), Italian, Portuguese, Brazilian Portuguese, Turkish, Japanese,
    Korean, Simplified Chinese, Traditional Chinese, Norwegian, Swedish, Finnish, Danish, Arabic,
    Hebrew, Polish, Russian, Czech, Slovak, Hungarian, Romanian, Bulgarian, Greek, Croatian,
    Lithuanian, Latvian, Estonian and Thai.

    Arabic and Hebrew mirror the whole interface right to left, rather than only reversing text.

??? question "How do I report a bug?"

    Open an issue at
    [github.com/jimz011/android-hki7/issues](https://github.com/jimz011/android-hki7/issues).
    Attach the **Connection log** from **Settings › Connection** if the problem is
    connection-shaped — it has a copy button.

??? question "How do I help without buying Premium?"

    **Settings › HKI 7 › Support** lists the ways. Bug reports with the connection log attached
    are genuinely among the most useful.
