# Multiple homes

HKI 7 can be connected to more than one Home Assistant at once — a house and a holiday home, a
main server and a test instance, your place and a parent's.

Manage them under **Settings › Connection › Home Assistant instances**.

## What each home keeps of its own

Each instance is genuinely separate, not a bookmark. Every home keeps its own:

- Login and tokens
- Dashboard
- Notification settings and connection
- Location and device-tracker settings
- Internal/external URLs and Wi-Fi network list

## Adding and switching

**Add Home Assistant instance** runs the same discovery-and-login flow as the first setup,
including naming the device for that server.

To switch between homes, **swipe left from the upper-right edge of any page header**. The active
instance is also shown and selectable in the Connection settings.

[:octicons-arrow-right-24: Gestures](../getting-started/gestures.md)

Instances can be **renamed** and **removed**. Removing one deletes its stored credentials and its
dashboard from the device; it does not touch anything on the server.

## Notifications across homes

Each home has its own notification switch, managed under **Settings › Connection**.

As soon as **more than one** home is set to receive notifications, **Background notifications**
becomes required — a single persistent foreground connection is what makes several servers
deliverable at the same time. The Notifications screen says so when it applies.

[:octicons-arrow-right-24: Notifications](notifications.md)

## Location across homes

Zones come from whichever instances you have location enabled for, and geofences are registered
for all of them together. Each zone's geofence is keyed by both instance and entity, so two homes
with a zone called `home` do not collide.

## Family sharing across homes

Family sharing is per-server: the
[HKI 7 Cloud](https://github.com/jimz011/HKI7-Cloud-Component) component stores the household's
state on the Home Assistant it is installed on. Two homes with the component installed each have their own admins,
their own shared dashboards, and their own parental controls.
