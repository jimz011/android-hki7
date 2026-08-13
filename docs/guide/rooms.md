# Rooms

HKI 7 maps Home Assistant's **areas** to rooms and its **floors** to the groups they sit in. The
Rooms page is a card per room; tapping one opens that room's own screen.

!!! important "Rooms come from Home Assistant areas"

    This is the one piece of Home Assistant setup HKI 7 genuinely depends on. If your devices are
    not assigned to areas, there are no rooms to show — auto-generation has nothing to group by,
    the per-room counters have nothing to count, and the role slots below have nothing to
    discover.

    Set them up in Home Assistant under **Settings → Areas, labels & zones**, assign devices to
    them under **Settings → Devices & Services → Devices**, then re-run the dashboard import or
    add the rooms by hand.

## The Rooms page

Room cards are grouped by floor, each floor a section with its own heading. A floor can be
**full** or **half** width, which is how you get two small rooms side by side and the living room
across the whole row.

Each card shows the room's wallpaper or colour, its name and icon, and a **status summary** — what
is currently on or open in there. It also shows motion, presence and people counters when the room
has entities that report them.

## Room detail

A room screen is a dashboard in its own right: it takes the same buttons, stacks, badges and
widgets as any other page, and edits the same way.

Above that, HKI 7 gives every room four **role slots** that get their own prominent controls:

| Slot | Typical entities |
|---|---|
| **Lock** | `lock.*` — the room's door |
| **Climate** | `climate.*` — its thermostat or AC |
| **Camera** | `camera.*` — a live stream |
| **Blinds** | `cover.*` — shades and curtains |

Each slot takes **several** entities, not just one, and each has its own icon override. They are
auto-discovered from the area's devices on import, and stay auto-discovered until you change them
— after which HKI 7 leaves your choice alone.

## Room configuration

Open a room's settings to set:

**Identity**

: Name, icon, wallpaper image and header colour, and which floor it belongs to.

**Role entities**

: The lock / climate / camera / blinds slots described above, plus their icons.

**Media players**

: Which players belong to this room. Auto-discovered, and left alone once you customise them.

**Temperature and humidity**

: One or more sensors each. Multiple sources are **averaged**, so a room with three thermometers
  reports one sensible number rather than whichever the importer happened to pick.

**Status summary**

: Which entities feed the "what is on in here" line on the room card and header, keyed by role.

**Badge bar**

: The room's own badges, configured independently of the Home page's.

## People in a room

If your household runs a room-presence sensor — [ESPresense](https://espresense.com/) or Home
Assistant's `mqtt_room` — HKI 7 can show who is currently in each room, and optionally follow a
person from room to room as they move.

Nothing in the app talks to MQTT. Those integrations publish the room name as an ordinary sensor
state, and HKI 7 reads that sensor like any other.

Setting this up is a family-sharing feature, because the sensor roster is configured once for the
household rather than on every phone:

[:octicons-arrow-right-24: Room following](family-sharing.md#room-following)

## Hiding a room

An admin can hide whole rooms from specific family members, so a child's phone never shows the
master bedroom.

!!! warning "Hiding is not a permission"

    This hides the room from HKI 7's interface. Home Assistant has no per-entity read permission
    for non-admin users, so anyone in the household can still reach those entities through Home
    Assistant directly. See [Family sharing](family-sharing.md#parental-controls).
