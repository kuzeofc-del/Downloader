package com.mediadownloader.app

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        handleIntent(intent)
        askPermissions()
        setContent { AppTheme { AppRoot() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onDestroy() {
        Browser.detach()
        super.onDestroy()
    }

    /** IDM-style: when you come back to the app with a copied link, offer to download it. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) return
        val text = try {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                ?.getItemAt(0)?.coerceToText(this)?.toString()
        } catch (_: Exception) { null } ?: return
        val url = Downloads.extractUrls(text).firstOrNull() ?: return
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
        if (prefs.getString("last_clip", null) == url) return
        prefs.edit().putString("last_clip", url).apply()
        UiBus.clipUrl.value = url
    }

    private fun handleIntent(intent: Intent?) {
        val text = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        } ?: return
        val url = Downloads.extractUrls(text).firstOrNull() ?: return
        getSharedPreferences("app", Context.MODE_PRIVATE).edit().putString("last_clip", url).apply()
        UiBus.sheet.value = SheetRequest(url)
    }

    private fun askPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT <= 28) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (perms.isNotEmpty()) permissionLauncher.launch(perms.toTypedArray())
    }
}

@Composable
fun AppRoot() {
    val tab by UiBus.tab.collectAsState()
    val overlay by UiBus.overlay.collectAsState()
    val sheet by UiBus.sheet.collectAsState()
    val items by Downloads.items.collectAsState()
    val active = items.count { it.isActive }

    if (overlay == "settings") {
        BackHandler { UiBus.overlay.value = null }
        SettingsScreen(onBack = { UiBus.overlay.value = null })
    } else {
        BackHandler(enabled = tab != Tab.HOME && tab != Tab.BROWSER) { UiBus.tab.value = Tab.HOME }
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Píldora flotante de cristal
                val pill = RoundedCornerShape(50)
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .clip(pill).background(Color(0x59000000)).glass(pill)
                        .padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NavItem(Tab.HOME, tab, "Home") { Icon(Icons.Default.Home, null) }
                    NavItem(Tab.BROWSER, tab, "Browser") { Icon(Icons.Default.Public, null) }
                    NavItem(Tab.DOWNLOADS, tab, "Download") {
                        BadgedBox(badge = { if (active > 0) Badge(containerColor = Orange) { Text("$active") } }) {
                            Icon(Icons.Default.Download, null)
                        }
                    }
                    NavItem(Tab.FILES, tab, "Files") { Icon(Icons.Default.Folder, null) }
                }
            }
        ) { pad ->
            Box(Modifier.padding(pad).fillMaxSize()) {
                when (tab) {
                    Tab.HOME -> HomeScreen()
                    Tab.BROWSER -> BrowserScreen()
                    Tab.DOWNLOADS -> DownloadsScreen()
                    Tab.FILES -> FilesScreen()
                }
            }
        }
    }
    sheet?.let { req -> QualitySheet(req, onDismiss = { UiBus.sheet.value = null }) }
}

@Composable
private fun RowScope.NavItem(
    t: Tab, current: Tab, label: String, icon: @Composable () -> Unit,
) {
    val selected = t == current
    val tint = if (selected) Orange else Color(0xCCFFFFFF)
    Column(
        Modifier.weight(1f)
            .clip(RoundedCornerShape(50))
            .background(if (selected) Color(0x33FF6A13) else Color.Transparent)
            .clickable { UiBus.tab.value = t }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompositionLocalProvider(LocalContentColor provides tint) { icon() }
        Text(label, color = tint, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
