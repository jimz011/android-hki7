package com.example.hki7.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private data class QuickStartTip(
    val title: String,
    val instruction: String,
    val icon: ImageVector
)

private val gestureTips = listOf(
    QuickStartTip("Notifications", "Swipe right from the upper-left edge.", Icons.Default.Notifications),
    QuickStartTip("Quick actions", "Pull down on any page header for Search, Edit and Settings.", Icons.Default.KeyboardArrowDown),
    QuickStartTip("Media player", "When the handle appears, swipe up on the bottom bar to restore the player.", Icons.Default.KeyboardArrowUp)
)

private val editTip = QuickStartTip(
    "Make it yours",
    "In Edit mode, add and reorder rooms, floors and widgets. Tap an item to configure it.",
    Icons.Default.Edit
)

@Composable
fun QuickStartGuideDialog(onComplete: () -> Unit) {
    val colors = LocalHKIAppColors.current
    var activeGesture by remember { mutableIntStateOf(0) }
    var completing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2_800)
            activeGesture = (activeGesture + 1) % gestureTips.size
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.96f)
                    .widthIn(max = 560.dp),
                shape = RoundedCornerShape(32.dp),
                color = colors.surface,
                tonalElevation = 10.dp,
                shadowElevation = 20.dp
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                    colors.surface,
                                    colors.surface
                                )
                            )
                        )
                ) {
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 24.dp, top = 24.dp, end = 24.dp)
                    ) {
                    Surface(
                        modifier = Modifier.size(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.17f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(27.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your dashboard is ready",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Four simple gestures are all you need to get around HKI 7.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onMuted
                    )
                    Spacer(Modifier.height(22.dp))

                    AnimatedContent(
                        targetState = activeGesture,
                        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(220)) },
                        label = "quick_start_gesture"
                    ) { gesture ->
                        GesturePreview(gesture, gestureTips[gesture].title)
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        gestureTips.indices.forEach { index ->
                            Box(
                                Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(width = if (index == activeGesture) 22.dp else 7.dp, height = 7.dp)
                                    .background(
                                        if (index == activeGesture) MaterialTheme.colorScheme.primary
                                        else colors.onMuted.copy(alpha = 0.28f),
                                        CircleShape
                                    )
                            )
                        }
                    }

                    gestureTips.forEachIndexed { index, tip ->
                        QuickStartTipRow(tip, highlighted = index == activeGesture)
                        Spacer(Modifier.height(9.dp))
                    }
                    QuickStartTipRow(editTip, highlighted = false)
                    Spacer(Modifier.height(20.dp))
                    }
                    Button(
                        onClick = {
                            if (!completing) {
                                completing = true
                                onComplete()
                            }
                        },
                        enabled = !completing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 24.dp)
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(if (completing) "Opening dashboard…" else "Got it — let's explore", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickStartTipRow(tip: QuickStartTip, highlighted: Boolean) {
    val colors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        color = if (highlighted) accent.copy(alpha = 0.11f) else colors.subtleSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = RoundedCornerShape(13.dp),
                color = if (highlighted) accent.copy(alpha = 0.19f) else colors.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(tip.icon, contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    tip.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(tip.instruction, style = MaterialTheme.typography.bodySmall, color = colors.onMuted)
            }
        }
    }
}

@Composable
private fun GesturePreview(gesture: Int, label: String) {
    val colors = LocalHKIAppColors.current
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "guide_motion")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "guide_progress"
    )
    val motionAlpha = sin(progress * PI).toFloat().coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth().height(320.dp),
        shape = RoundedCornerShape(24.dp),
        color = colors.background.copy(alpha = 0.72f)
    ) {
        Box(Modifier.fillMaxSize()) {
            Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                val w = size.width
                val h = size.height
                val phoneH = h * 0.94f
                val phoneW = min(w * 0.52f, phoneH * 0.52f)
                val phoneLeft = (w - phoneW) / 2f
                val phoneTop = (h - phoneH) / 2f
                val phoneRadius = phoneW * 0.16f
                val bezel = 5.dp.toPx()

                // Portrait device shell and screen.
                drawRoundRect(
                    Color.Black.copy(alpha = 0.92f),
                    topLeft = Offset(phoneLeft, phoneTop),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(phoneRadius)
                )
                drawRoundRect(
                    colors.onMuted.copy(alpha = 0.28f),
                    topLeft = Offset(phoneLeft, phoneTop),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(phoneRadius),
                    style = Stroke(1.dp.toPx())
                )
                val screenLeft = phoneLeft + bezel
                val screenTop = phoneTop + bezel
                val screenW = phoneW - bezel * 2f
                val screenH = phoneH - bezel * 2f
                val screenRadius = phoneRadius - bezel
                drawRoundRect(
                    colors.surface,
                    topLeft = Offset(screenLeft, screenTop),
                    size = Size(screenW, screenH),
                    cornerRadius = CornerRadius(screenRadius)
                )

                // Status area and earpiece make the silhouette read as a phone immediately.
                val statusH = screenH * 0.075f
                drawRoundRect(
                    colors.onMuted.copy(alpha = 0.38f),
                    Offset(screenLeft + screenW * 0.39f, screenTop + statusH * 0.32f),
                    Size(screenW * 0.22f, 3.dp.toPx()),
                    CornerRadius(2.dp.toPx())
                )

                val headerTop = screenTop + statusH
                val headerH = screenH * 0.23f
                drawRect(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0.24f), accent.copy(alpha = 0.08f)),
                        startY = headerTop,
                        endY = headerTop + headerH
                    ),
                    topLeft = Offset(screenLeft, headerTop),
                    size = Size(screenW, headerH)
                )
                // Header controls.
                val headerIconY = headerTop + headerH * 0.35f
                drawCircle(accent.copy(alpha = 0.72f), 4.dp.toPx(), Offset(screenLeft + screenW * 0.13f, headerIconY))
                drawRoundRect(
                    colors.onSurface.copy(alpha = 0.52f),
                    Offset(screenLeft + screenW * 0.23f, headerIconY - 2.dp.toPx()),
                    Size(screenW * 0.34f, 4.dp.toPx()),
                    CornerRadius(2.dp.toPx())
                )
                drawCircle(colors.onSurface.copy(alpha = 0.34f), 5.dp.toPx(), Offset(screenLeft + screenW * 0.84f, headerIconY))

                // A recognisable vertical dashboard: summary card followed by a two-column grid.
                val cardColor = colors.onMuted.copy(alpha = 0.12f)
                val contentTop = headerTop + headerH + screenH * 0.055f
                val gap = screenW * 0.055f
                val contentInset = screenW * 0.09f
                val contentW = screenW - contentInset * 2f
                drawRoundRect(
                    cardColor,
                    Offset(screenLeft + contentInset, contentTop),
                    Size(contentW, screenH * 0.15f),
                    CornerRadius(8.dp.toPx())
                )
                val smallW = (contentW - gap) / 2f
                val smallTop = contentTop + screenH * 0.15f + gap
                drawRoundRect(cardColor, Offset(screenLeft + contentInset, smallTop), Size(smallW, screenH * 0.17f), CornerRadius(8.dp.toPx()))
                drawRoundRect(cardColor, Offset(screenLeft + contentInset + smallW + gap, smallTop), Size(smallW, screenH * 0.17f), CornerRadius(8.dp.toPx()))
                drawRoundRect(
                    cardColor.copy(alpha = cardColor.alpha * 0.72f),
                    Offset(screenLeft + contentInset, smallTop + screenH * 0.17f + gap),
                    Size(contentW, screenH * 0.09f),
                    CornerRadius(7.dp.toPx())
                )

                // Bottom navigation and its media handle.
                val navH = screenH * 0.13f
                val navTop = screenTop + screenH - navH - screenH * 0.035f
                drawRoundRect(
                    colors.elevated,
                    Offset(screenLeft + screenW * 0.07f, navTop),
                    Size(screenW * 0.86f, navH),
                    CornerRadius(navH * 0.38f)
                )
                val handleCenter = Offset(screenLeft + screenW / 2f, navTop - screenH * 0.027f)
                drawRoundRect(
                    accent.copy(alpha = 0.85f),
                    Offset(handleCenter.x - screenW * 0.11f, handleCenter.y - 2.dp.toPx()),
                    Size(screenW * 0.22f, 4.dp.toPx()),
                    CornerRadius(2.dp.toPx())
                )
                repeat(4) { index ->
                    drawCircle(
                        colors.onMuted.copy(alpha = if (index == 0) 0.62f else 0.32f),
                        radius = 3.dp.toPx(),
                        center = Offset(screenLeft + screenW * (0.23f + index * 0.18f), navTop + navH / 2f)
                    )
                }

                val eased = FastOutSlowInEasing.transform(progress)
                val (start, end) = when (gesture) {
                    0 -> Offset(phoneLeft - 4.dp.toPx(), headerIconY) to Offset(screenLeft + screenW * 0.55f, headerIconY)
                    1 -> Offset(screenLeft + screenW / 2f, headerTop + headerH * 0.18f) to Offset(screenLeft + screenW / 2f, contentTop + screenH * 0.19f)
                    else -> handleCenter to Offset(handleCenter.x, smallTop + screenH * 0.10f)
                }

                // Highlight the part of the UI affected by the current gesture.
                when (gesture) {
                    0 -> drawCircle(accent.copy(alpha = 0.18f * motionAlpha), 16.dp.toPx(), Offset(screenLeft + screenW * 0.13f, headerIconY))
                    1 -> drawRoundRect(
                        accent.copy(alpha = 0.10f * motionAlpha),
                        Offset(screenLeft, headerTop),
                        Size(screenW, headerH),
                        CornerRadius(8.dp.toPx())
                    )
                    else -> drawCircle(accent.copy(alpha = 0.20f * motionAlpha), 18.dp.toPx(), handleCenter)
                }

                // A faint full arrow keeps the direction understandable even between finger loops.
                drawLine(
                    accent.copy(alpha = 0.32f),
                    start,
                    end,
                    3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val dx = end.x - start.x
                val dy = end.y - start.y
                val length = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val ux = dx / length
                val uy = dy / length
                val arrowBack = 11.dp.toPx()
                val arrowSide = 6.dp.toPx()
                val arrowBase = Offset(end.x - ux * arrowBack, end.y - uy * arrowBack)
                drawLine(
                    accent.copy(alpha = 0.52f),
                    end,
                    Offset(arrowBase.x - uy * arrowSide, arrowBase.y + ux * arrowSide),
                    3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    accent.copy(alpha = 0.52f),
                    end,
                    Offset(arrowBase.x + uy * arrowSide, arrowBase.y - ux * arrowSide),
                    3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val finger = Offset(
                    x = start.x + (end.x - start.x) * eased,
                    y = start.y + (end.y - start.y) * eased
                )
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(finger.x, finger.y)
                }
                drawPath(
                    path,
                    accent.copy(alpha = 0.28f * motionAlpha),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(Color.White.copy(alpha = 0.96f * motionAlpha), 11.dp.toPx(), finger)
                drawCircle(accent.copy(alpha = motionAlpha), 11.dp.toPx(), finger, style = Stroke(3.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.20f * motionAlpha), 18.dp.toPx(), finger)
            }

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(26.dp),
                color = colors.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = colors.onSurface,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
