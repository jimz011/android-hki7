# Custom pages

**Home** and **Rooms** are built in and always present. Beyond those you can add **custom pages** —
as many as you like, each a full-width canvas with its own name, icon and contents, sitting in the
navigation bar next to everything else.

A custom page is the answer to "this does not belong on Home and it is not a room": a media wall, a
garden page, a workshop, a holiday page, a page of everything the cleaner needs and nothing else.

## Create a page

Custom pages are created and edited under **Settings › Dashboard › Navigation Bar**, from the
**Create custom page** button at the top.

A page has three properties:

**Name**

: What the navigation bar shows, and the title at the top of the page. Required.

**Subtitle**

: Optional line under the title in the page header.

**Icon**

: The navigation bar icon, picked from the bundled
  [Material Design Icons and Simple Icons](appearance.md#icons-and-animation) sets. Defaults to
  `view-dashboard` if you do not choose one.

The page is created empty and appears in the navigation bar straight away.

!!! tip "Edit the same page later"

    The same three fields are reachable from the page itself: pull down on the header, open the
    page's settings, and choose **Name, subtitle, and navigation icon**. Editing from either place
    changes the same page.

## Fill it

A custom page takes the same widget canvas as Home, and edits exactly the same way —
[pull down on the header](../getting-started/gestures.md#quick-actions) to reach edit mode, then
add widgets, button stacks and swiping stacks, drag to reorder, and tap any item to open its
settings.

Everything on [Dashboards](dashboards.md) applies here: the same widgets, the same actions, the
same [visibility rules](dashboards.md#visibility-rules), the same appearance controls.

Two things Home has that a custom page deliberately does not:

- **No badge bar.** Badges live on Home and on room pages.
- **No people row or notification status** in the header.

The header is otherwise the same, and follows the full/compact setting under
[Appearance](appearance.md).

## Place it in the navigation bar

The list under **Settings › Dashboard › Navigation Bar** holds every tab. Home and Rooms are pinned
at the top and cannot be moved or hidden. Everything below them — the Climate, Security, Energy and
Battery screens, and your custom pages — can be:

- **Reordered** with the up and down arrows. The order is also the horizontal
  [swipe](../getting-started/gestures.md) order.
- **Hidden**, with the visibility toggle. A hidden page keeps its contents; it simply leaves the
  bar until you switch it back on.
- **Edited**, with the pencil, for custom pages.

!!! note "Hiding is how you retire a page"

    There is no delete for a custom page. Switching it off in the navigation bar takes it out of
    the bar and out of the swipe order, and its widgets are kept for when you want it back.

## Reaching a page

A custom page is reached by tapping its navigation bar entry, or by swiping horizontally from the
page beside it.

!!! warning "Not a navigation target"

    Buttons, badges and popups with a **Navigate** action can target Home, Rooms, the Climate,
    Security, Energy and Battery screens, Settings, or a specific room — but not a custom page. To
    put a custom page one tap away, keep it visible in the navigation bar.

## Sharing, permissions and backups

Custom pages are part of the dashboard, so they travel with it:

- **[Backups](backup.md)** include every custom page and its widgets.
- **[Shared dashboards](family-sharing.md)** carry custom pages to the people you share with. When
  a shared dashboard arrives, a page you already have keeps your local version, so your edits are
  not overwritten.
- **[Parental controls](family-sharing.md#parental-controls)** list each custom page by name alongside
  the built-in views, so an admin can hide a specific page from a specific family member.
