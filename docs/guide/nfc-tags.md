# NFC tags

HKI 7 reads and writes the same NFC tags as the official Home Assistant app. Tap one and Home
Assistant fires a `tag_scanned` event, which is what tag triggers in automations listen for. Tags
are set up under **Settings › NFC tags**.

You need a phone with NFC, and NFC switched on in Android's settings. The screen says so if
either is missing.

## Scanning

Hold the phone near a tag. There is nothing to open first: a tag tapped anywhere in HKI 7 is
reported, not only on the Scan tab, and a banner confirms it reached Home Assistant. The Scan tab
keeps the last twenty scans and writes.

With HKI 7 closed or in the background, the tap is reported without the app opening, as in the
official app; a message appears only if it could not reach Home Assistant. That needs a tag
written with [the switch below](#read-tags-while-hki-7-is-closed) on.

The scan is sent through this phone's own Home Assistant registration, the same way the official
app sends it. Home Assistant therefore records this phone as the device that scanned the tag, and
an automation can tell which phone did. That registration exists once location or notifications
are switched on in HKI 7. Without one, HKI 7 fires the event directly instead, which Home
Assistant only accepts from an administrator.

## Writing

On the **Write** tab, either let HKI 7 generate a new ID or enter one you already use, then tap
**Tap a tag to write** and hold a tag to the phone. Enter an existing ID to rewrite a tag without
touching the automations that use it. A label is optional and stays on this phone; it is never
sent to Home Assistant.

A tag holds a link of the form `https://www.home-assistant.io/tag/<id>`, which is the format the
official app writes, so either app reads tags written by the other.

### Read tags while HKI 7 is closed

This switch, on by default, decides what happens when a tag is tapped while HKI 7 is **closed**.

Home Assistant's website tells Android that `home-assistant.io` links belong to the official app,
so a plain tag always goes to that app, or to the browser when it is not installed. No other app
can claim those links. With the switch on, HKI 7 adds a second record to the tag (an Android
Application Record) that names HKI 7, and Android hands the tap to HKI 7 instead. HKI 7 reports
it without opening.

| | Switch on | Switch off |
| --- | --- | --- |
| HKI 7 closed | Reported, without HKI 7 opening | The official app or the browser opens |
| HKI 7 open | Reported | Reported |
| Android phone without HKI 7 | Sent to HKI 7 on the Play Store | Opens the official app |
| iPhone | Opens the Home Assistant app | Opens the Home Assistant app |

Turn it off if other people in the house use the official app and should be able to tap the same
tags. The setting only affects tags written from now on; to change an existing tag, write it again
with the same ID.

If a tag is too small to hold both records, HKI 7 writes the link alone, as the official app does.

!!! note "Tags written elsewhere"
    A tag written by the official app, or by HKI 7 with the switch off, is a plain tag. It still
    works while HKI 7 is open, but a tap while HKI 7 is closed goes to the official app. Write it
    again from HKI 7 to change that.
