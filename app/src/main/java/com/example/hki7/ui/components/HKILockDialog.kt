package com.example.hki7.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoorFront
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors

internal val LockGreen  = Color(0xFF4CAF50)
internal val LockOrange = Color(0xFFFF8C00)
internal val LockRed    = Color(0xFFE53935)

@Composable
fun HKILockDialog(
    entity: HAEntity,
    entities: List<HAEntity> = listOf(entity),
    doorEntities: Map<String, HAEntity?> = emptyMap(),
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
    titleOverrides: Map<String, String?> = emptyMap(),
    iconNames: Map<String, String?> = emptyMap(),
    spinIcons: Map<String, Boolean> = emptyMap()
) {
    var page by remember(entities.map { it.entity_id }) { mutableIntStateOf(0) }
    var dragAmount by remember { mutableFloatStateOf(0f) }
    val currentEntity = entities.getOrElse(page) { entity }
    val doorEntity = doorEntities[currentEntity.entity_id]

    val isDoorOpen  = doorEntity?.state == "on"
    val accentColor = when {
        isDoorOpen                            -> LockRed
        currentEntity.state == "open"         -> LockRed
        currentEntity.state == "locked"       -> LockGreen
        else                                  -> LockOrange
    }
    val stateText = when {
        isDoorOpen || currentEntity.state == "open" -> "Open"
        currentEntity.state == "locked"             -> "Locked"
        else                                        -> "Unlocked"
    }
    val icon = if (currentEntity.state == "locked") Icons.Default.Lock else Icons.Default.LockOpen
    val pageStatus = if (entities.size > 1) "${page + 1}/${entities.size} - ${stateText.uppercase()}" else stateText.uppercase()

    val openDoorTab = listOf(
        Triple("Open Door", Icons.Default.DoorFront, { viewModel.openLock(currentEntity.entity_id) })
    )

    HKIDialog(
        entity = currentEntity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = icon,
        iconTint = accentColor,
        titleOverride = titleOverrides[currentEntity.entity_id],
        iconName = iconNames[currentEntity.entity_id]?.takeUnless { it.isBlank() }
            ?: defaultEntityIconSlug(currentEntity, lockDoorOpen = isDoorOpen),
        spinIcon = spinIcons[currentEntity.entity_id] == true,
        statusText = pageStatus,
        tabs = openDoorTab,
        currentTab = null
    ) {
        val appColors = LocalHKIAppColors.current
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(entities.size) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragAmount > 80f && page > 0) page--
                            if (dragAmount < -80f && page < entities.lastIndex) page++
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragAmount += amount
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stateText,
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.headlineLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Box(Modifier.height(VerticalControlHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        VerticalMasterSwitch(
                            isOn = currentEntity.state == "locked",
                            onToggle = { viewModel.toggleLock(currentEntity.entity_id) },
                            accentColor = accentColor,
                            doorOpen = isDoorOpen
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("LOCK", color = appColors.onMuted, style = MaterialTheme.typography.labelSmall)
                }
            }

            if (entities.size > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    entities.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == page) 8.dp else 6.dp)
                                .background(if (index == page) Color.White else Color.Gray, CircleShape)
                        )
                    }
                }
            }
        }
    }
}
