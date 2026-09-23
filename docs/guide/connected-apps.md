# Connected apps

Some things a household wants on its dashboard do not live in Home Assistant. **Settings ›
Connected apps** is where HKI 7 is pointed at another app that already holds them, so a widget can
show the real thing instead of a copy that drifts.

TidyShop is the first. The section is a list rather than a single page because more will follow.

## TidyShop

[TidyShop](https://github.com/jimz011) is a family shopping-list app. It syncs through a
**connector you host yourself** — there is no account with anyone else, and no cloud service in
the middle. Once this device is connected, the [to-do widget](widgets.md#syncing-with-tidyshop)
can show the family's real lists.

### Connecting this device

You need two things: the address of your connector (for example
`https://tidyshop.example.com`), and a code from it.

| Code | Where it comes from | What it does |
| --- | --- | --- |
| **Invite code** | The family owner, from TidyShop | Adds you to the household as a new member |
| **Device code** | Another of your own phones | Adds this device to the member you already are |
| **Recovery code** | Written down when you first enrolled | Restores your identity after a reinstall |
| **Master pairing key** | The server's own configuration | The break-glass path, if you run the server |

Use a **device code** rather than an invite when you already use TidyShop on your phone —
otherwise you join your own household twice, as two separate people, and see none of your own
lists.

### How this device proves who it is

There is no password. On connecting, this phone generates a key pair inside the Android Keystore
and sends only the public half to your connector. Every request is signed with the private half,
which:

- never leaves the device,
- cannot be read by HKI 7 itself, and
- **cannot be included in a backup**.

That last point is deliberate and worth knowing: restoring an HKI 7 backup onto a new phone brings
your dashboards but not your family access. The new phone gets back in with one of the codes above.
The upside is that a copied backup file grants nobody access to your family's lists.

**Leave family** deletes that key. The device stops signing immediately, and rejoining needs a new
code.

### Keeping up to date

While HKI 7 is on screen it holds one idle connection to your connector, so a change made by
anyone in the household appears within seconds. When the app is closed or in your pocket it holds
no connection, keeps no wakelock and schedules no background work — the same rule the rest of the
app follows for [presence](presence.md).

Whatever changed while you were away arrives in a single update the moment you look again, so
nothing is missed by not polling for it.

### Permissions

TidyShop decides, per list, whether you may **view**, **tick off**, or **edit**. HKI 7 shows
exactly that and no more, and its own
[family permissions](family-sharing.md#permissions) still apply on top: whichever is stricter wins.

!!! note "Notifications stay in TidyShop"

    HKI 7 does not raise notifications for family lists. That would mean holding a connection open
    while the app is closed, which is a cost the dashboard should not impose — TidyShop's own app
    already does it, per device, with its own quiet hours and per-list muting.
