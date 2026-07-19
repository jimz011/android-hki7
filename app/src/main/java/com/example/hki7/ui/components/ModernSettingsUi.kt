package com.example.hki7.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hki7.ui.theme.LocalHKIAppColors

/** Onboarding-inspired heading used by full settings surfaces. */
@Composable
fun ModernSettingsHeader(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Tune,
    canGoBack: Boolean = false,
    onBack: () -> Unit = {},
    onClose: (() -> Unit)? = null
) {
    val colors = LocalHKIAppColors.current
    val accent = readableDialogAccent(MaterialTheme.colorScheme.primary, colors.elevated)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (canGoBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(46.dp)
                    .background(colors.subtleSurface, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.onSurface)
            }
        } else {
            Surface(
                modifier = Modifier.size(50.dp),
                shape = RoundedCornerShape(17.dp),
                color = accent.copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }
        }

        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        onClose?.let { close ->
            IconButton(
                onClick = close,
                modifier = Modifier
                    .size(46.dp)
                    .background(colors.subtleSurface, CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurface)
            }
        }
    }
}

/** Compact version for Material AlertDialog title slots. */
@Composable
fun ModernSettingsDialogTitle(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.Default.Tune
) {
    val colors = LocalHKIAppColors.current
    val accent = readableDialogAccent(MaterialTheme.colorScheme.primary, colors.elevated)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(15.dp),
            color = accent.copy(alpha = 0.16f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(23.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onMuted)
        }
    }
}

@Composable
fun SettingsSubcategory(
    title: String,
    description: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalHKIAppColors.current
    val accent = readableDialogAccent(MaterialTheme.colorScheme.primary, colors.elevated)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        description?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onMuted)
        }
    }
}

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalHKIAppColors.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = colors.subtleSurface,
        contentColor = colors.onSurface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
fun ModernSettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalHKIAppColors.current
    val accent = readableDialogAccent(MaterialTheme.colorScheme.primary, colors.elevated)
    Surface(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = colors.subtleSurface,
        contentColor = colors.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(14.dp),
                color = accent.copy(alpha = 0.13f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = colors.onMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = colors.onMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsTabRow(
    tabs: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (key, label) ->
            SettingsChoiceChip(
                selected = selected == key,
                onClick = { onSelect(key) },
                label = { Text(label) }
            )
        }
    }
}

/** Shared borderless selection chip used by settings tabs, categories, and compact choices. */
@Composable
fun SettingsChoiceChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = LocalHKIAppColors.current
    val accent = readableDialogAccent(MaterialTheme.colorScheme.primary, colors.elevated)
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(14.dp),
        border = null,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.subtleSurface,
            labelColor = colors.onMuted,
            selectedContainerColor = accent.copy(alpha = 0.24f),
            selectedLabelColor = colors.onSurface,
            disabledContainerColor = colors.subtleSurface.copy(alpha = 0.55f),
            disabledLabelColor = colors.onMuted.copy(alpha = 0.55f)
        )
    )
}

/** Full-width, onboarding-inspired frame for settings that need more room than an AlertDialog. */
@Composable
fun ModernSettingsDialogFrame(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    icon: ImageVector = Icons.Default.Tune,
    onBack: (() -> Unit)? = null,
    footer: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    val colors = LocalHKIAppColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false)
    ) {
        BackHandler { onBack?.invoke() ?: onDismiss() }
        DialogContrastTheme(colors) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .widthIn(max = 620.dp)
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(32.dp),
                color = colors.elevated,
                contentColor = colors.onSurface,
                shadowElevation = 18.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    colors.elevated,
                                    colors.elevated
                                )
                            )
                        )
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    ModernSettingsHeader(
                        title = title,
                        subtitle = subtitle,
                        icon = icon,
                        canGoBack = onBack != null,
                        onBack = { onBack?.invoke() },
                        onClose = onDismiss
                    )
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) { content() }
                    HorizontalDivider(color = colors.onMuted.copy(alpha = 0.22f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = footer
                    )
                }
            }
        }
    }
}

/** Modern replacement for Material AlertDialog, shared by confirmations, pickers, and editors. */
@Composable
fun ModernAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    stableHeight: Boolean = false
) {
    val colors = LocalHKIAppColors.current
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        DialogContrastTheme(colors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = modifier
                        .fillMaxWidth(0.94f)
                        .widthIn(max = 560.dp)
                        .then(
                            if (stableHeight) Modifier.fillMaxHeight(0.88f).heightIn(max = 720.dp)
                            else Modifier
                        ),
                    shape = RoundedCornerShape(30.dp),
                    color = colors.elevated,
                    contentColor = colors.onSurface,
                    shadowElevation = 18.dp
                ) {
                    Column(
                        modifier = (if (stableHeight) Modifier.fillMaxSize() else Modifier.heightIn(max = 720.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.11f),
                                        colors.elevated,
                                        colors.elevated
                                    )
                                )
                            )
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                    if (icon != null || title != null) {
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
                                    if (icon != null) {
                                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                                            icon()
                                        }
                                    } else {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            title?.let { titleContent ->
                                Box(Modifier.weight(1f)) {
                                    CompositionLocalProvider(
                                        LocalContentColor provides colors.onSurface,
                                        LocalTextStyle provides MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                    ) { titleContent() }
                                }
                            }
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.size(44.dp).background(colors.subtleSurface, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = colors.onSurface)
                            }
                        }
                    }

                    text?.let { textContent ->
                        Box(modifier = Modifier.fillMaxWidth().weight(1f, fill = stableHeight)) {
                            CompositionLocalProvider(
                                LocalContentColor provides colors.onSurface,
                                LocalTextStyle provides MaterialTheme.typography.bodyMedium
                            ) { textContent() }
                        }
                    }

                    HorizontalDivider(color = colors.onMuted.copy(alpha = 0.22f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dismissButton?.invoke()
                        Spacer(Modifier.width(8.dp))
                        confirmButton()
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogContrastTheme(
    colors: com.example.hki7.ui.theme.HKIAppColors,
    content: @Composable () -> Unit
) {
    val base = MaterialTheme.colorScheme
    val darkSurface = colors.elevated.luminance() < 0.5f
    val accent = readableDialogAccent(base.primary, colors.elevated)
    val onAccent = if (accent.luminance() < 0.45f) Color.White else Color(0xFF111111)
    val selectedContainer = accent.copy(alpha = if (darkSurface) 0.28f else 0.14f)
    val dialogScheme = base.copy(
        primary = accent,
        onPrimary = onAccent,
        background = colors.elevated,
        onBackground = colors.onSurface,
        surface = colors.elevated,
        onSurface = colors.onSurface,
        surfaceVariant = colors.subtleSurface,
        onSurfaceVariant = colors.onMuted,
        primaryContainer = selectedContainer,
        onPrimaryContainer = colors.onSurface,
        secondaryContainer = selectedContainer,
        onSecondaryContainer = colors.onSurface,
        outline = colors.onMuted.copy(alpha = 0.72f),
        outlineVariant = colors.onMuted.copy(alpha = 0.34f)
    )
    MaterialTheme(
        colorScheme = dialogScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

private fun readableDialogAccent(accent: Color, surface: Color): Color = when {
    surface.luminance() < 0.5f && accent.luminance() < 0.30f -> lerp(accent, Color.White, 0.38f)
    surface.luminance() >= 0.5f && accent.luminance() > 0.68f -> lerp(accent, Color.Black, 0.30f)
    else -> accent
}
