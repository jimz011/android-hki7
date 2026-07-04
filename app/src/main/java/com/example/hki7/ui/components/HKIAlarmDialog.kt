package com.example.hki7.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HAEntity
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay

fun alarmStateColor(state: String): Color = when (state.lowercase()) {
    "disarmed" -> Color(0xFFE53935)
    "armed_home", "armed_away", "armed_night", "armed_vacation", "armed_custom_bypass" -> Color(0xFF4CAF50)
    "pending", "arming", "disarming" -> Color(0xFFFF8C00)
    "triggered" -> Color(0xFFE53935)
    else -> Color(0xFF9E9E9E)
}

fun alarmStateLabel(state: String): String = when (state.lowercase()) {
    "disarmed" -> "Disarmed"
    "armed_home" -> "Armed Home"
    "armed_away" -> "Armed Away"
    "armed_night" -> "Armed Night"
    "armed_vacation" -> "Armed Vacation"
    "armed_custom_bypass" -> "Armed (Custom)"
    "pending" -> "Pending"
    "arming" -> "Arming…"
    "disarming" -> "Disarming…"
    "triggered" -> "TRIGGERED"
    else -> state.replaceFirstChar(Char::uppercase)
}

@Composable
fun HKIAlarmDialog(
    entity: HAEntity,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    titleOverride: String? = null,
    iconName: String? = null,
    spinIcon: Boolean = false
) {
    val accent = alarmStateColor(entity.state)
    HKIDialog(
        entity = entity,
        onDismiss = onDismiss,
        viewModel = viewModel,
        icon = Icons.Default.Security,
        iconTint = accent,
        titleOverride = titleOverride,
        iconName = iconName,
        spinIcon = spinIcon,
        statusText = alarmStateLabel(entity.state)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            AlarmKeypadContent(entity, viewModel)
        }
    }
}

@Composable
private fun AlarmKeypadContent(entity: HAEntity, viewModel: MainViewModel) {
    val appColors = LocalHKIAppColors.current
    val state = entity.state.lowercase()
    val accent = alarmStateColor(state)
    var codeBuffer by remember(entity.entity_id) { mutableStateOf("") }
    var errorMessage by remember(entity.entity_id) { mutableStateOf<String?>(null) }
    var successMessage by remember(entity.entity_id) { mutableStateOf<String?>(null) }
    var selectedAction by remember(entity.entity_id, entity.state) { mutableStateOf<Pair<String, String>?>(null) }
    var pendingRemainingSeconds by remember(entity.entity_id) { mutableIntStateOf(0) }
    var pendingCancelCode by remember(entity.entity_id) { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }
    val shakeOffset = remember { Animatable(0f) }
    val pendingSeconds by viewModel.alarmPendingSeconds.collectAsState()
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        listOf(0f, -14f, 14f, -10f, 10f, -6f, 6f, 0f).forEach { target ->
            shakeOffset.animateTo(target, animationSpec = tween(45))
        }
    }
    LaunchedEffect(pendingRemainingSeconds) {
        if (pendingRemainingSeconds <= 0) return@LaunchedEffect
        delay(1000)
        pendingRemainingSeconds = (pendingRemainingSeconds - 1).coerceAtLeast(0)
    }

    val needsCode = entity.alarmCodeFormat != null
    val isPending = state in setOf("pending", "arming")
    val isBusy = state in setOf("pending", "arming", "disarming", "triggered")

    fun submit(service: String) {
        val submittedCode = codeBuffer.takeIf { needsCode }
        viewModel.setAlarmState(entity.entity_id, service, submittedCode) { success ->
            codeBuffer = ""
            if (success) {
                errorMessage = null
                successMessage = when (service) {
                    "alarm_disarm" -> "Alarm disarmed"
                    else -> "${selectedAction?.second ?: "Alarm command"} accepted"
                }
                if (service == "alarm_disarm") {
                    pendingRemainingSeconds = 0
                    pendingCancelCode = null
                } else if (service.startsWith("alarm_arm_")) {
                    pendingRemainingSeconds = pendingSeconds
                    pendingCancelCode = submittedCode
                }
                selectedAction = null
            } else {
                successMessage = null
                errorMessage = "Command failed. Check the code and try again."
                shakeTrigger++
            }
        }
    }

    fun cancelPending() {
        viewModel.setAlarmState(entity.entity_id, "alarm_disarm", pendingCancelCode) { success ->
            if (success) {
                pendingRemainingSeconds = 0
                pendingCancelCode = null
                selectedAction = null
                errorMessage = null
                successMessage = "Arming cancelled"
            } else {
                successMessage = null
                errorMessage = "Cancel failed. Check the code and try again."
                shakeTrigger++
            }
        }
    }

    val armModes = remember(entity.supportedFeatures) {
        buildList {
            val f = entity.supportedFeatures
            if (f and 1 != 0) add("alarm_arm_home" to "Arm Home")
            if (f and 2 != 0) add("alarm_arm_away" to "Arm Away")
            if (f and 4 != 0) add("alarm_arm_night" to "Arm Night")
            if (f and 16 != 0) add("alarm_arm_custom_bypass" to "Arm Custom")
            if (f and 32 != 0) add("alarm_arm_vacation" to "Arm Vacation")
        }
    }
    val actions = remember(state, armModes) {
        buildList {
            if (state != "disarmed") add("alarm_disarm" to "Disarm")
            if (state == "disarmed") addAll(armModes)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
    ) {
        Text(alarmStateLabel(state), color = accent, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.height(20.dp), contentAlignment = Alignment.Center) {
            when {
                errorMessage != null -> Text(errorMessage!!, color = Color(0xFFEF5350), style = MaterialTheme.typography.bodyMedium)
                successMessage != null -> Text(successMessage!!, color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                isBusy -> Text("Please wait…", color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(20.dp))

        if (isPending || pendingRemainingSeconds > 0) {
            AlarmPendingPanel(
                remainingSeconds = pendingRemainingSeconds.takeIf { it > 0 },
                onCancel = { cancelPending() }
            )
            return@Column
        }

        if (selectedAction == null) {
            AlarmModeList(
                actions = actions,
                pendingSeconds = pendingSeconds,
                onPendingSecondsSelected = { viewModel.setAlarmPendingSeconds(it) },
                onActionClick = { action ->
                    selectedAction = action
                    codeBuffer = ""
                    errorMessage = null
                    successMessage = null
                }
            )
            return@Column
        }

        Text(selectedAction!!.second, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(14.dp))

        if (needsCode) {
            Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (codeBuffer.isEmpty()) {
                    Text("Enter code", color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(codeBuffer.length) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(accent))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            AlarmKeypad(
                modifier = Modifier.offset(x = shakeOffset.value.dp),
                onDigit = { d -> if (codeBuffer.length < 8) codeBuffer += d },
                onClear = { codeBuffer = "" },
                onBackspace = { codeBuffer = codeBuffer.dropLast(1) }
            )
            Spacer(Modifier.height(24.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { selectedAction = null; codeBuffer = ""; errorMessage = null }, modifier = Modifier.weight(1f)) {
                Text("Back")
            }
            Button(
                onClick = { submit(selectedAction!!.first) },
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (needsCode) "Confirm" else selectedAction!!.second)
            }
        }
    }
}

@Composable
private fun AlarmModeList(
    actions: List<Pair<String, String>>,
    pendingSeconds: Int,
    onPendingSecondsSelected: (Int) -> Unit,
    onActionClick: (Pair<String, String>) -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Modes", color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
        Text("Select a mode to continue", color = appColors.onMuted, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))

        Text("Pending timer", color = appColors.onSurface, style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(5, 10, 15, 20, 30, 60).forEach { seconds ->
                FilterChip(
                    selected = pendingSeconds == seconds,
                    onClick = { onPendingSecondsSelected(seconds) },
                    label = { Text("${seconds}s") }
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        if (actions.isEmpty()) {
            Text("No supported alarm modes reported by this entity", color = appColors.onMuted, style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        actions.forEach { action ->
            val actionColor = alarmStateColor(if (action.first == "alarm_disarm") "disarmed" else "armed_home")
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionClick(action) },
                shape = RoundedCornerShape(18.dp),
                color = appColors.surface,
                border = BorderStroke(1.dp, appColors.onMuted.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = actionColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = action.second,
                        color = appColors.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = appColors.onMuted)
                }
            }
        }
    }
}

@Composable
private fun AlarmPendingPanel(
    remainingSeconds: Int?,
    onCancel: () -> Unit
) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = appColors.surface,
        border = BorderStroke(1.dp, alarmStateColor("pending").copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = alarmStateColor("pending"),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text("Alarm pending", color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
            Text(
                remainingSeconds?.let { "Arming in $it seconds" } ?: "Waiting for Home Assistant",
                color = appColors.onMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel arming")
            }
        }
    }
}

@Composable
private fun AlarmKeypad(
    modifier: Modifier = Modifier,
    onDigit: (Char) -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('C', '0', '⌫')
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { key ->
                    AlarmKeypadButton(key) {
                        when (key) {
                            'C' -> onClear()
                            '⌫' -> onBackspace()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlarmKeypadButton(label: Char, onClick: () -> Unit) {
    val appColors = LocalHKIAppColors.current
    Surface(
        modifier = Modifier.size(64.dp).clip(CircleShape).clickable { onClick() },
        shape = CircleShape,
        color = appColors.subtleSurface
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (label) {
                'C' -> Text("Clear", style = MaterialTheme.typography.labelSmall, color = appColors.onMuted)
                '⌫' -> Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = appColors.onMuted)
                else -> Text(label.toString(), style = MaterialTheme.typography.headlineSmall, color = appColors.onSurface)
            }
        }
    }
}
