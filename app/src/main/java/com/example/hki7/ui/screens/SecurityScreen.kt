package com.example.hki7.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hki7.ui.MainViewModel
import com.example.hki7.ui.components.HKIPage
import com.example.hki7.ui.theme.LocalHKIAppColors
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
fun SecurityScreen(viewModel: MainViewModel) {
    val entities by viewModel.entities.collectAsState()
    val currentUrl by viewModel.currentUrl.collectAsState()
    val appColors = LocalHKIAppColors.current
    val cameras = entities.filter { it.entity_id.startsWith("camera.") }

    HKIPage(
        viewModel = viewModel,
        title = "Security",
        subtitle = "Cameras",
        pageKey = "security",
        pageSettingsTitle = "Security Settings"
    ) { padding ->
        if (cameras.isEmpty()) {
            EmptyEditHint(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cameras) { camera ->
                    val streamUrl = camera.attributes?.get("entity_picture")?.jsonPrimitive?.contentOrNull?.let {
                        if (it.startsWith("http")) it else "$currentUrl$it"
                    }
                    Surface(shape = RoundedCornerShape(24.dp), color = appColors.elevated) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(camera.friendlyName ?: camera.entity_id, color = appColors.onSurface, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).aspectRatio(16 / 9f),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.Black
                            ) {
                                if (streamUrl != null) {
                                    AsyncImage(streamUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        Icon(Icons.Default.Security, null, tint = appColors.onMuted)
                                        Text("No preview", color = appColors.onMuted, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyEditHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Swipe the header down to start edit mode, then add widgets.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}
