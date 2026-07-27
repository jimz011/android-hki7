package com.jimz011apps.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jimz011apps.hki7.BuildConfig
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/**
 * Changelog for the current release, newest first. Only the entry matching the running
 * [BuildConfig.VERSION_NAME] is shown, so adding the next version's notes here is all that a
 * future release needs.
 */
// beta.4 through beta.6 carry the same backup/branding notes forward: beta.5 only reworked the app
// icon and beta.6 adds a single nav-bar fix, so each still shows the backup changes instead of a
// near-empty changelog for anyone updating straight from beta.3.
private val beta4Through6Notes = listOf(
    "The Backup and Restore screen now shows when each automatic backup last ran — both Google Drive and Home Assistant — so you can tell at a glance that your latest changes are safely saved.",
    "Added a \"Back up now\" button to each cloud backup, so you can create an immediate backup any time without waiting for the daily schedule.",
    "Fixed: the app icon looked oversized after the recent themed-icon (monochrome) update. The mark now sits with proper padding, matching other app icons on your home screen and in the themed-icon style.",
)

// beta.7 shows only its own notes — the beta.4–6 backup/icon notes have had plenty of exposure.
private val beta7Notes = listOf(
    "Cleaner automatic room import. When a device's main purpose is a light, thermostat, blind, fan, humidifier, lock, or vacuum, HKI no longer clutters the room with the device's built-in helper controls — its own child lock, the little status light on an air conditioner or blind, and its sleep or display switches are all skipped. Genuine extras are still kept, like a ceiling fan's separate light or a standalone smart door lock.",
    "Climate devices from different integrations are no longer merged together. A room that used to collapse every thermostat and AC into one combined tile now gives each integration its own control — your Tado thermostats stay together, and a Tuya AC gets its own tile — so you can operate them separately.",
    "Climate devices now show an icon that matches what they actually are: air conditioners (units that cool but don't heat) get an AC icon, humidifiers and dehumidifiers get a humidifier icon, and fans recognised as air purifiers get a purifier icon — instead of everything sharing the generic thermostat or fan icon.",
    "Air purifier and humidifier icons now pulse gently instead of spinning while the device is running. A spinning purifier or humidifier looked wrong, so they use the same calm pulse as other climate devices.",
    "Door locks now use door-shaped icons — a closed, locked door when locked and an open door when unlocked — instead of a plain padlock, so a room's lock state is clearer at a glance.",
    "Header badges that summarise several devices at once can now be reordered. Open the badge's settings to choose which entity leads — it decides the badge's icon, the state it shows, and the order of the pop-up list.",
    "Animated icon effects are now on by default for new installs, so a fresh setup feels alive out of the box. Existing setups keep whatever you already chose, and you can turn effects on or off any time under Settings › Appearance › Icons.",
    "Buttons and header badges can now show one of an entity's attributes instead of its state — for example a sensor's battery level or a media player's current source — and you can add a unit like °C, %, W, or kW to the shown value. Open the item's settings and pick State or one of the listed attributes. (The unused per-button Label field has been removed.)",
    "You can now copy a dashboard. In Settings › Dashboards, tap the copy icon on any dashboard to create a full duplicate — every room, widget, and page setting included — as a starting point for a new layout.",
    "New Settings › Family Sharing section brings parental controls, family dashboard sharing, and per-user permissions together under three tabs. Admins can set per person whether they can edit at all, whether they're limited to aesthetic changes only (icons, names, layout, wallpaper — no adding or removing widgets, buttons or rooms), and whether they see the global search and flows buttons. Everyone else sees it locked, and can choose to use a dashboard an admin shared with them — including during onboarding.",
    "Shared dashboards now update automatically. When the owner pushes a change, it's pulled in the next time you open the app — while keeping your own aesthetic tweaks (icons, names, layout, wallpaper) intact.",
    "Non-admin family members now get the automatic per-room Adaptive Lighting controls too — previously only admins saw them, because Home Assistant limits reading that configuration to admins. With the HKI 7 Cloud component installed, everyone gets them.",
    "Fixed: the dashboard header title and subtitle now stay readable on any header background — a light wallpaper or custom color no longer leaves the text near-white and hard to read. The color is chosen from the actual header background instead of the app's light/dark mode.",
    "Fixed: the \"Unable to connect\" screen — with its Refresh and Log in again buttons — now appears reliably when Home Assistant can't be reached. In some cases, such as signing in on a trusted network and then leaving Wi-Fi, the app used to keep showing a stale dashboard instead of offering to reconnect.",
)

private val changelog: Map<String, List<String>> = mapOf(
    "1.0.0-beta.7" to beta7Notes,
    "1.0.0-beta.6" to listOf(
        "Fixed: in edit mode, the Add Widget and Add Room / Floor bars no longer overlap the system navigation buttons when you use three-button navigation instead of gesture navigation.",
    ) + beta4Through6Notes,
    "1.0.0-beta.5" to beta4Through6Notes,
    "1.0.0-beta.4" to beta4Through6Notes,
    "1.0.0-beta.3" to listOf(
        "Added three new icon packs alongside Material Design Icons: Simple Icons (brand & service logos like Spotify and Philips Hue), Tabler, and Phosphor. Switch packs right in the icon picker when choosing an icon for any button.",
        "New: animated icons. Entity icons can gently glow, spin, or pulse while the device is active — lights glow, fans and vacuums spin, playing media and active climate pulse. Only active devices animate; turn it on and tune the effects under Settings › Appearance › Icons.",
        "New: family dashboard sharing. An admin can share a dashboard with specific family members (or everyone), and they import it into their own app — no more passing backup files between phones.*",
        "New: parental controls. Admins can hide certain views and rooms from specific people, keeping a dashboard simple for kids or guests (UX-level hiding, not a Home Assistant security restriction).*",
        "New: automatic local cloud backup. Back up your dashboard and appearance settings to your own Home Assistant every day, and restore them any time.*",
        "Media player source icons are now colored, and tapping the app logo opens the installed app — for example, if music is playing from Spotify, tapping the Spotify logo in the media player bar opens the Spotify app.",
        "New widget: iFrame — embed any web page on your dashboard.",
        "Fixed: Energy views now show negative values when you're exporting power.",
        "Fixed: onboarding could auto-complete when tabbing out of the app before finishing.",
        "Fixed: the thermostat dial's mode-selection button was too large and overlapped nearby items.",
        "Many other smaller bug fixes.",
        "* (HKI 7 Cloud Component integration required)",
    ),
    "1.0.0-beta.2" to listOf(
        "Fixed an issue with smaller displays when either the height was too small or the width too narrow (or both), the content would look bad. It now falls back to a single column design on smaller screens. This has been changed across all elements.",
        "Dialog headers now show 2 rows on narrower screens so that the title no longer cuts off.",
        "Fixed an issue where light sliders that have adaptive lighting feature would overlap other elements.",
        "Added visual to nav bars if there is more content on the navbar than the screen can display",
        "Fixed an issue where camera's wouldn't respect screen orientation lock.",
        "Fixed an issue where we could not zoom in on camera dialogs or full-screen camera's."
    )
)

/** True when there are release notes to show for the running build. */
fun hasChangelogForCurrentVersion(): Boolean = changelog[BuildConfig.VERSION_NAME]?.isNotEmpty() == true

/**
 * "What's new" dialog shown once after the app is updated. Dismissal is the caller's cue to record
 * the version code so it never appears again for this release.
 */
@Composable
fun WhatsNewDialog(onDismiss: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    val entries = changelog[BuildConfig.VERSION_NAME].orEmpty()
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.96f),
                shape = RoundedCornerShape(30.dp),
                color = appColors.elevated,
                contentColor = appColors.onSurface,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                    appColors.elevated,
                                    appColors.elevated
                                )
                            )
                        )
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "What's new",
                                style = MaterialTheme.typography.headlineSmall,
                                color = appColors.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                "HKI 7 v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = appColors.onMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .heightIn(max = 420.dp)
                            .fadingEdges(scrollState)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        entries.forEach { entry ->
                            // Entries starting with "*" are footnotes: no bullet, muted style.
                            if (entry.startsWith("*")) {
                                Text(
                                    entry,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appColors.onMuted
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        Modifier
                                            .padding(top = 7.dp)
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                    Text(
                                        entry,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = appColors.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(0.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Got it")
                        Spacer(Modifier.width(6.dp))
                    }
                }
            }
        }
    }
}
