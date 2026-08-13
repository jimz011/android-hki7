# Connect to Home Assistant

## Find your server

HKI 7 checks your local network first and lists what it finds. Tap a discovered server to use it.

If nothing appears:

- Make sure the phone is on the same Wi-Fi network as Home Assistant, not on mobile data or a
  guest VLAN.
- Choose **Enter an address manually** and type the URL yourself, for example
  `http://homeassistant.local:8123` or `http://192.168.1.20:8123`.

Include the scheme (`http://` or `https://`) and the port. Home Assistant's default port is `8123`.

## Sign in

HKI 7 opens Home Assistant's **own** login page. You type your username and password into Home
Assistant, not into HKI 7, and Home Assistant hands the app an authorization code that it exchanges
for a token. The app never sees your password.

The token is stored locally on the device and is only ever sent back to the server that issued it.

??? question "Sign-in failed — what now?"

    - **"Can't reach the server"** — the address is wrong, the server is down, or a certificate
      is not trusted. Tap **Try again**, or **Choose a different server** to go back.
    - **"No authorization code"** — the login page completed but did not return a code. This is
      usually a redirect being blocked; try again in a different browser, or set the browser you
      use for links to a mainstream one.
    - Multi-factor authentication works normally — it is Home Assistant's own login screen.

    More in [Troubleshooting](../troubleshooting.md).

## Name this device

Pick something recognisable — `Kitchen Tablet`, `Jimmy's Phone`. This name is what the device is
registered as with Home Assistant, which means it decides:

- the name of the `notify.mobile_app_<device>` service your automations will call,
- the names of the diagnostic sensors HKI 7 can create for this device,
- how the device appears in **Settings › Family Sharing › Devices**.

The registration itself goes through Home Assistant's standard mobile-app registration endpoint,
the same one the official companion app uses.

## Local and remote access

A phone moves between your home Wi-Fi and the outside world, and the address that works in one
place often does not work in the other. HKI 7 handles both, and neither is required.

Open **Settings › Connection** to set them up.

### Remote access

: **External URL or Nabu Casa URL** — for example `https://example.ui.nabu.casa` or your own
  reverse-proxied domain. Leave it empty for a local-only setup.

### Local network

: **Internal URL** — for example `http://homeassistant.local:8123`, plus **Home Wi-Fi names**, a
  comma-separated list of SSIDs. There is an **Add current network** button so you do not have to
  type it. On those networks the app uses the internal URL; everywhere else it uses the external
  one.

!!! tip "Why bother with an internal URL"

    Going out to the internet and back in — hairpinning through your router — is slower and, on
    some routers, does not work at all. An internal URL keeps home traffic on the LAN, which
    matters most for camera streams.

### Checking the connection

The **Active connection** block at the top of the Connection screen shows which route is in use
and the current status. **Refresh connection** forces a reconnect.

At the bottom of the same screen is the **Connection log** — a live diagnostic of what the
connection has actually been doing: attempts, reconnects, sync, and subscriptions. It holds the
last 300 lines, starts empty every time the app launches, and has buttons to copy or clear it. It
is the first thing to look at when something connection-shaped goes wrong, and the right thing to
attach to a bug report.

!!! note "The log is not a history"

    It is deliberately not persisted across launches: the lines can name entities and Home
    Assistant addresses, and a stored log would need a retention policy and a privacy story it
    does not have.

## Next

Once you are connected, the app asks what your dashboard should start from.

[:octicons-arrow-right-24: Your first dashboard](first-dashboard.md)
