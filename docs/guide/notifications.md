# Notifications

Notifications come from **your own Home Assistant**, over the app's own connection to it. There is
no third-party push service and no developer-operated relay in the path.

## How delivery works

When you named the device during setup, HKI 7 registered with Home Assistant the same way the
official companion app does. That gives you a notify service:

```yaml
service: notify.mobile_app_<device_name>
data:
  title: Doorbell
  message: Someone is at the front door
```

HKI 7 listens for those calls on its live connection and turns them into Android notifications.

**While the app is open**, delivery rides the app's normal websocket. Nothing extra is needed.

**While the app is closed**, you need **Background notifications** (**Settings › Notifications**),
which keeps a persistent connection to Home Assistant. This uses more battery, which is why it is
a switch rather than a default.

!!! info "Multiple homes"

    Background notifications are **required** as soon as more than one home is set to receive
    notifications — a single foreground connection is what makes several servers deliverable at
    once. Each home's notification setting is managed under **Settings › Connection**.

### The connection notification

Android requires a visible notification for a persistent foreground connection. HKI 7's is on its
own **"Notification connection"** channel, so you can turn just that channel off in Android's
settings and keep everything else. The **Hide Connection Notification** button in
**Settings › Notifications** takes you straight there. The connection keeps working; only its
notification disappears.

## Supported payload fields

HKI 7 mirrors the official app's behaviour for the common fields:

| Field | Behaviour |
|---|---|
| `title` | Notification title |
| `message` | Notification text. The literal value `clear_notification` cancels the notification with the matching `tag` instead of showing anything |
| `data.tag` | Groups a notification so a later one with the same tag replaces it, and so `clear_notification` can cancel it |
| `data.channel` | Which Android notification channel to post on |
| `data.clickAction` | Where tapping the notification body goes |
| `data.sticky` | Keeps the notification from being dismissed by a tap |
| `data.actions` | Buttons on the notification — see below |

## Action buttons

`data.actions` puts buttons on a notification. Most entries fire a
`mobile_app_notification_action` event back to Home Assistant when tapped — **the same event the
official app fires**, so automations written for the official companion app work unchanged.

```yaml
service: notify.mobile_app_kitchen_tablet
data:
  title: Front door
  message: Someone is at the door
  data:
    actions:
      - action: UNLOCK_DOOR
        title: Unlock
      - action: URI
        title: Open camera
        uri: /lovelace/cameras
      - action: REPLY
        title: Reply
```

Two action names are reserved and behave differently:

`URI`

: Opens the `uri` directly instead of firing an event.

`REPLY`

: Shows an inline text input. Home Assistant's cross-platform spelling, `behavior: textInput`,
  does the same thing.

Any `action_data` you attach is echoed back inside the fired event, so an automation can tell
*which* door the tap was about.

!!! tip "Actions work from inside the app too"

    Tapping an action on an entry in the in-app notification history fires exactly the same
    `mobile_app_notification_action` event the notification shade would.

## The notification panel

**Swipe right from the upper-left edge** to open the notification panel. It has:

- **Unread** — arrived and not yet seen.
- **History** — read, but still around.
- **Archived** — kept deliberately.
- **Events** — a live timeline of what the house has been doing. See
  [Event timeline](events.md).

Per-entry actions are mark unread, archive and delete, plus mark-all-read.

### Retention

Non-archived entries are **purged 48 hours after arrival**. Archived entries are kept
indefinitely. The unread badge counts unread, non-archived entries only — and deliberately ignores
the Events tab entirely, because a feed of doors opening would leave the badge permanently lit and
drain it of any meaning.

## If notifications are not arriving

See [Troubleshooting › Notifications](../troubleshooting.md#notifications-never-arrive).
