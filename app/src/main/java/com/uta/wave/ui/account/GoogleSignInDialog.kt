package com.uta.wave.ui.account

import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.uta.wave.data.account.AccountManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSignInDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val accountManager = remember { AccountManager.getInstance(context) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFF2AEC7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sign in to YouTube Music",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = Color.White
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF1E1422)
                    )
                )
            },
            containerColor = Color(0xFF140E18)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.userAgentString =
                                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            webViewClient = object : WebViewClient() {
                                private var hasSaved = false

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val currentUrl = url ?: return
                                    val cookies = cookieManager.getCookie(currentUrl) ?: ""

                                    if (!hasSaved && (cookies.contains("SAPISID") || cookies.contains("__Secure-3PAPISID") || cookies.contains("SSID"))) {
                                        hasSaved = true
                                        view?.evaluateJavascript(
                                            """
                                            (function() {
                                                try {
                                                    var img = document.querySelector('img#img, yt-img-shadow#avatar img, button#avatar-btn img, img.yt-img-shadow');
                                                    var name = document.querySelector('yt-formatted-string#text, #channel-name, #account-name');
                                                    return JSON.stringify({
                                                        avatar: img ? img.src : null,
                                                        name: name ? name.innerText : null
                                                    });
                                                } catch(e) { return null; }
                                            })()
                                            """.trimIndent()
                                        ) { resultJson ->
                                            var avatar: String? = null
                                            var name = "Namikaze aftab"
                                            try {
                                                if (!resultJson.isNullOrBlank() && resultJson != "null") {
                                                    val clean = if (resultJson.startsWith("\"") && resultJson.endsWith("\"")) {
                                                        org.json.JSONTokener(resultJson).nextValue().toString()
                                                    } else resultJson
                                                    val obj = org.json.JSONObject(clean)
                                                    avatar = obj.optString("avatar").takeIf { !it.isNullOrBlank() && it != "null" }
                                                    val parsedName = obj.optString("name").takeIf { !it.isNullOrBlank() && it != "null" }
                                                    if (parsedName != null) name = parsedName
                                                }
                                            } catch (e: Exception) {
                                                // fallback
                                            }
                                            accountManager.saveSession(
                                                cookieHeader = cookies,
                                                userName = name,
                                                userEmail = "Connected via YouTube Music",
                                                avatarUrl = avatar
                                            )
                                            Toast.makeText(ctx, "Signed in as $name!", Toast.LENGTH_SHORT).show()
                                            onSuccess()
                                        }
                                    }
                                }
                            }

                            loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https%3A%2F%2Fmusic.youtube.com")
                            webViewInstance = this
                        }
                    }
                )
            }
        }
    }
}
