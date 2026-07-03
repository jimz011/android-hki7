package com.example.hki7.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.hki7.data.HomeAssistantClient
import com.example.hki7.data.PreferencesManager
import kotlinx.coroutines.launch
import java.net.URLEncoder

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(prefs: PreferencesManager, onLoginSuccess: () -> Unit) {
    var urlInput by remember { mutableStateOf("https://hassio.schings.xyz") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (!isAuthenticating) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to HKI 7", style = MaterialTheme.typography.displaySmall)
            Text("Connect your Home Assistant server", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(Modifier.height(48.dp))
            
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://hassio.schings.xyz") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                isError = errorMessage != null
            )
            
            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(Modifier.height(24.dp))
            
            Button(
                onClick = { 
                    if (urlInput.isNotBlank()) {
                        errorMessage = null
                        isAuthenticating = true 
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Login with Home Assistant")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }
    } else {
        val authUrl = "${urlInput.removeSuffix("/")}/auth/authorize?client_id=${URLEncoder.encode("https://home-assistant.io/android", "UTF-8")}&redirect_uri=${URLEncoder.encode("homeassistant://auth-callback", "UTF-8")}"
        
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: ""
                                if (url.startsWith("homeassistant://auth-callback")) {
                                    val code = request?.url?.getQueryParameter("code")
                                    if (code != null) {
                                        scope.launch {
                                            try {
                                                val response = HomeAssistantClient.getAccessToken(urlInput, code)
                                                prefs.saveConnectionDetails(urlInput, response.access_token, response.refresh_token, response.expires_in)
                                                onLoginSuccess()
                                            } catch (e: Exception) {
                                                errorMessage = "Token exchange failed: ${e.message}"
                                                isAuthenticating = false
                                            }
                                        }
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl(authUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            IconButton(
                onClick = { isAuthenticating = false },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
