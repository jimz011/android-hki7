package com.example.hki7.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import com.example.hki7.ui.components.ModernAlertDialog as AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.hki7.data.HKIMarkdownWidget
import com.example.hki7.ui.components.EditRemoveBadge
import com.example.hki7.ui.components.EditSettingsButton
import com.example.hki7.ui.components.WidgetWidthSelector
import com.example.hki7.ui.components.WidgetBackground
import com.example.hki7.ui.components.WidgetBackgroundSelector
import com.example.hki7.ui.components.fadingEdges
import com.example.hki7.ui.components.surfaceGradient
import com.example.hki7.ui.components.itemCornerShape
import com.example.hki7.ui.theme.LocalHKIAppColors

/** Free-form markdown card. Content is authored in the widget settings. */
@Composable
fun MarkdownWidgetItem(
    widget: HKIMarkdownWidget,
    isEditMode: Boolean,
    onDelete: () -> Unit,
    onSettings: () -> Unit,
    currentUrl: String = ""
) {
    if (widget.isHidden && !isEditMode) return
    val appColors = LocalHKIAppColors.current
    Box {
        Surface(
            modifier = Modifier.fillMaxWidth()
                .then(if (widget.isSquare) Modifier.aspectRatio(1f) else Modifier)
                .then(
                    if (widget.backgroundUrl.isNullOrBlank())
                        Modifier.background(surfaceGradient(appColors.elevated), RoundedCornerShape(widget.cornerRadius.dp))
                    else Modifier
                ),
            shape = RoundedCornerShape(widget.cornerRadius.dp),
            color = Color.Transparent
        ) {
          Box {
            WidgetBackground(widget.backgroundUrl, currentUrl)
            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .then(if (widget.isSquare) Modifier.verticalScroll(scroll) else Modifier),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (widget.content.isBlank()) {
                    Text(
                        "Empty markdown widget — open its settings in edit mode to write content.",
                        style = MaterialTheme.typography.bodySmall, color = appColors.onMuted
                    )
                } else {
                    MarkdownContent(widget.content)
                }
            }
          }
        }
        if (isEditMode) {
            EditRemoveBadge(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd))
            EditSettingsButton(onClick = onSettings, modifier = Modifier.align(Alignment.Center))
        }
    }
}

/**
 * Minimal markdown renderer: #–###### headings, - / * / numbered lists, > quotes, --- rules,
 * ``` code blocks, and inline **bold**, *italic*, `code`, ~~strike~~ and [text](url) links.
 */
@Composable
fun MarkdownContent(markdown: String) {
    val appColors = LocalHKIAppColors.current
    val lines = markdown.replace("\r\n", "\n").lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.startsWith("```") -> {
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.appendLine(lines[i])
                    i++
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = itemCornerShape(),
                    color = appColors.subtleSurface
                ) {
                    Text(
                        code.toString().trimEnd('\n'),
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = appColors.onSurface,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            trimmed == "---" || trimmed == "***" || trimmed == "___" ->
                HorizontalDivider(color = appColors.onMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 4.dp))
            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 6)
                val text = trimmed.dropWhile { it == '#' }.trim()
                val style = when (level) {
                    1 -> MaterialTheme.typography.headlineSmall
                    2 -> MaterialTheme.typography.titleLarge
                    3 -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleSmall
                }
                Text(markdownInline(text), style = style.copy(fontWeight = FontWeight.Bold), color = appColors.onSurface)
            }
            trimmed.startsWith("> ") || trimmed == ">" -> {
                Row {
                    Box(
                        Modifier.width(3.dp).heightIn(min = 18.dp)
                            .background(appColors.onMuted.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        markdownInline(trimmed.removePrefix(">").trim()),
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = appColors.onMuted
                    )
                }
            }
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                Row {
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = appColors.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text(markdownInline(trimmed.substring(2)), style = MaterialTheme.typography.bodyMedium, color = appColors.onSurface)
                }
            }
            trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                val number = trimmed.substringBefore('.')
                Row {
                    Text("$number.", style = MaterialTheme.typography.bodyMedium, color = appColors.onSurface)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        markdownInline(trimmed.substringAfter('.').trim()),
                        style = MaterialTheme.typography.bodyMedium, color = appColors.onSurface
                    )
                }
            }
            trimmed.isEmpty() -> Spacer(Modifier.height(2.dp))
            else -> Text(markdownInline(trimmed), style = MaterialTheme.typography.bodyMedium, color = appColors.onSurface)
        }
        i++
    }
}

/** Inline markdown spans: **bold**, *italic*, _italic_, `code`, ~~strike~~, [text](url). */
@Composable
private fun markdownInline(text: String): AnnotatedString {
    val linkColor = MaterialTheme.colorScheme.primary
    return remember(text, linkColor) { parseInlineMarkdown(text, linkColor) }
}

private fun parseInlineMarkdown(text: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        var index = 0
        val length = text.length
        fun startsWith(token: String) = text.startsWith(token, index)
        while (index < length) {
            when {
                startsWith("**") -> {
                    val end = text.indexOf("**", index + 2)
                    if (end == -1) { append(text[index]); index++ } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(parseInlineMarkdown(text.substring(index + 2, end), linkColor))
                        }
                        index = end + 2
                    }
                }
                startsWith("~~") -> {
                    val end = text.indexOf("~~", index + 2)
                    if (end == -1) { append(text[index]); index++ } else {
                        withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                            append(parseInlineMarkdown(text.substring(index + 2, end), linkColor))
                        }
                        index = end + 2
                    }
                }
                startsWith("`") -> {
                    val end = text.indexOf('`', index + 1)
                    if (end == -1) { append(text[index]); index++ } else {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = linkColor.copy(alpha = 0.12f))) {
                            append(text.substring(index + 1, end))
                        }
                        index = end + 1
                    }
                }
                startsWith("*") || startsWith("_") -> {
                    val marker = text[index]
                    val end = text.indexOf(marker, index + 1)
                    if (end == -1) { append(text[index]); index++ } else {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(parseInlineMarkdown(text.substring(index + 1, end), linkColor))
                        }
                        index = end + 1
                    }
                }
                startsWith("[") -> {
                    val closeBracket = text.indexOf(']', index + 1)
                    val openParen = if (closeBracket != -1 && closeBracket + 1 < length && text[closeBracket + 1] == '(') closeBracket + 1 else -1
                    val closeParen = if (openParen != -1) text.indexOf(')', openParen + 1) else -1
                    if (closeBracket == -1 || openParen == -1 || closeParen == -1) { append(text[index]); index++ } else {
                        val label = text.substring(index + 1, closeBracket)
                        val url = text.substring(openParen + 1, closeParen)
                        withLink(
                            LinkAnnotation.Url(
                                url,
                                TextLinkStyles(style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                            )
                        ) { append(label) }
                        index = closeParen + 1
                    }
                }
                else -> { append(text[index]); index++ }
            }
        }
    }

@Composable
fun MarkdownWidgetSettingsDialog(
    widget: HKIMarkdownWidget,
    onDismiss: () -> Unit,
    onSave: (HKIMarkdownWidget) -> Unit
) {
    var content by remember(widget) { mutableStateOf(widget.content) }
    var width by remember(widget) { mutableStateOf(widget.width) }
    var square by remember(widget) { mutableStateOf(widget.isSquare) }
    var radius by remember(widget) { mutableIntStateOf(widget.cornerRadius) }
    var backgroundUrl by remember(widget) { mutableStateOf(widget.backgroundUrl) }
    var settingsPage by remember(widget) { mutableStateOf("content") }
    AlertDialog(
        stableHeight = true,
        onDismissRequest = onDismiss,
        title = { com.example.hki7.ui.components.ModernSettingsDialogTitle("Markdown", "Content and card appearance") },
        text = {
            val scroll = rememberScrollState()
            Column(
                Modifier.heightIn(max = 480.dp).fadingEdges(scroll).verticalScroll(scroll),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                com.example.hki7.ui.components.SettingsTabRow(
                    tabs = listOf("content" to "Content", "appearance" to "Appearance"),
                    selected = settingsPage,
                    onSelect = { settingsPage = it }
                )
                if (settingsPage == "content") {
                com.example.hki7.ui.components.SettingsSubcategory("Content", "Write the information shown on this card")
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Markdown") },
                    placeholder = { Text("# Heading\nSome **bold** text, a list:\n- item one\n- item two") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 160.dp),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace)
                )
                Text(
                    "Supports # headings, lists, > quotes, ``` code, **bold**, *italic*, `code`, ~~strike~~ and [links](https://…).",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalHKIAppColors.current.onMuted
                )
                }
                if (settingsPage == "appearance") {
                com.example.hki7.ui.components.SettingsSubcategory("Appearance", "Size, shape, and background")
                WidgetWidthSelector(width = width, onWidthChange = { width = it })
                Text("Shape", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !square, onClick = { square = false }, label = { Text("Standard") })
                    FilterChip(selected = square, onClick = { square = true }, label = { Text("Square") })
                }
                WidgetBackgroundSelector(backgroundUrl) { backgroundUrl = it }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(widget.copy(content = content, width = width, isSquare = square, cornerRadius = radius, backgroundUrl = backgroundUrl))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
