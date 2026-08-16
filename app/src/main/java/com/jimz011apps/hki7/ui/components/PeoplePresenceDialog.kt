package com.jimz011apps.hki7.ui.components

import com.jimz011apps.hki7.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.jimz011apps.hki7.data.HAArea
import com.jimz011apps.hki7.data.HAEntity
import com.jimz011apps.hki7.ui.components.ModernAlertDialog as AlertDialog
import com.jimz011apps.hki7.ui.theme.LocalHKIAppColors

/** One tracked person: who they are, where they are, and their Home Assistant picture. */
data class PersonPresence(
    val name: String,
    val roomName: String?,
    val avatarUrl: String? = null
)

/**
 * Who the room-presence sensors currently place where.
 *
 * Opened from a people counter, which used to be the only counter that could not be tapped — a
 * room saying "2" with no way to find out who, and no way to see the household at a glance short
 * of opening every room in turn.
 *
 * Two shapes from the same data: a room's counter lists the people in that room, and the
 * whole-home counter lists everyone with the room they are in.
 */
@Composable
fun PeoplePresenceDialog(
    people: List<PersonPresence>,
    /** False for a single room's counter, which is already titled by its room. */
    showRooms: Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalHKIAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Groups, contentDescription = null) },
        title = { Text(stringResource(R.string.cr_people_here_title)) },
        text = {
            if (people.isEmpty()) {
                Text(
                    stringResource(R.string.cr_people_here_empty),
                    color = colors.onMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    people.forEach { person ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = itemCornerShape(),
                            color = colors.subtleSurface
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                ) {
                                    // Home Assistant's own picture for the person where there is
                                    // one; the initial-style icon is only the fallback.
                                    if (person.avatarUrl != null) {
                                        AsyncImage(
                                            model = person.avatarUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        person.name,
                                        color = colors.onSurface,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (showRooms) {
                                        Text(
                                            person.roomName ?: stringResource(R.string.cr_people_here_unknown_room),
                                            color = colors.onMuted,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_close_bbfa773)) } }
    )
}

/** Absolute URL for a person's Home Assistant picture, which is served relative to the instance. */
private fun avatarUrl(entity: HAEntity?, baseUrl: String): String? =
    entity?.entityPicture?.let {
        if (it.startsWith("http") || it.startsWith("content:") || it.startsWith("file:")) it
        else baseUrl.trimEnd('/') + it
    }

private fun displayName(entity: HAEntity?, entityId: String): String =
    entity?.friendlyName?.takeIf { it.isNotBlank() } ?: entityId.substringAfter('.').replace('_', ' ')

/** Turns roster sensor ids into display rows, resolving each one's room from its reported state. */
fun personPresenceRows(
    entityIds: List<String>,
    entitiesById: Map<String, HAEntity>,
    baseUrl: String,
    roomNameOf: (String) -> String?
): List<PersonPresence> = entityIds.map { id ->
    val entity = entitiesById[id]
    PersonPresence(
        name = displayName(entity, id),
        roomName = entity?.state?.let(roomNameOf),
        avatarUrl = avatarUrl(entity, baseUrl)
    )
}

/** Every tracked person across the home, for the whole-home counter, grouped by room name. */
fun allPersonPresenceRows(
    peopleIdsByArea: Map<String, List<String>>,
    areas: List<HAArea>,
    entitiesById: Map<String, HAEntity>,
    baseUrl: String
): List<PersonPresence> {
    val areaNames = areas.associate { it.area_id to it.name }
    return peopleIdsByArea.entries
        .sortedBy { areaNames[it.key]?.lowercase() ?: "" }
        .flatMap { (areaId, ids) ->
            ids.map { id ->
                val entity = entitiesById[id]
                PersonPresence(
                    name = displayName(entity, id),
                    roomName = areaNames[areaId],
                    avatarUrl = avatarUrl(entity, baseUrl)
                )
            }
        }
}
