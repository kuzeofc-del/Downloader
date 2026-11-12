package com.mediadownloader.app

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject

data class Shortcut(val name: String, val url: String, val custom: Boolean = false) {
    val domain get() = Cookies.hostOf(url)?.let { Cookies.baseDomain(it) } ?: url
}

private val DEFAULT_SHORTCUTS = listOf(
    Shortcut("YouTube", "https://m.youtube.com"),
    Shortcut("Facebook", "https://m.facebook.com/watch"),
    Shortcut("Instagram", "https://www.instagram.com"),
    Shortcut("TikTok", "https://www.tiktok.com"),
    Shortcut("X-Twitter", "https://x.com"),
    Shortcut("Dailymotion", "https://www.dailymotion.com"),
    Shortcut("Vimeo", "https://vimeo.com/watch"),
)

private val DialogBg = Color(0xF5FFFFFF)
private val GlassText = Color(0xFF5B6070)
private val TileBg = Color(0xB3FFFFFF)

object Shortcuts {
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("shortcuts", Context.MODE_PRIVATE)
    fun custom(ctx: Context): List<Shortcut> = try {
        val a = JSONArray(prefs(ctx).getString("list", "[]"))
        (0 until a.length()).map { a.getJSONObject(it).let { o -> Shortcut(o.getString("n"), o.getString("u"), true) } }
    } catch (_: Exception) { emptyList() }

    fun save(ctx: Context, list: List<Shortcut>) {
        val a = JSONArray()
        list.forEach { a.put(JSONObject().put("n", it.name).put("u", it.url)) }
        prefs(ctx).edit().putString("list", a.toString()).apply()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen() {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val clipUrl by UiBus.clipUrl.collectAsState()
    val engine by Downloads.engine.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var custom by remember { mutableStateOf(Shortcuts.custom(ctx)) }
    var showAdd by remember { mutableStateOf(false) }
    var removeTarget by remember { mutableStateOf<Shortcut?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        // Sin título: solo el engranaje de ajustes, a la derecha
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { UiBus.overlay.value = "settings" }) { Icon(Icons.Default.Settings, "Settings", tint = InkColor) }
        }

        // ---- search / link box (glass)
        Row(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(30.dp)).padding(start = 20.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).padding(vertical = 18.dp)) {
                if (query.isEmpty()) Text("Search video or paste a link…", color = GlassText, fontSize = 16.sp)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = InkColor, fontSize = 16.sp),
                    cursorBrush = SolidColor(AccentBlue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { UiBus.go(query); query = "" }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isEmpty()) {
                IconButton(onClick = { query = clipboard.getText()?.text.orEmpty() }) {
                    Icon(Icons.Default.ContentPaste, "Paste", tint = GlassText)
                }
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(AccentBlue).clickable { UiBus.go(query); query = "" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.Search, "Go", tint = Color.White) }
        }

        // ---- copied link detected
        clipUrl?.let { url ->
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Link, null, tint = AccentBlue)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Copied link detected", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(url, color = GlassText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                TextButton(onClick = { UiBus.clipUrl.value = null }) { Text("Dismiss", color = GlassText) }
                Button(
                    onClick = { UiBus.clipUrl.value = null; UiBus.sheet.value = SheetRequest(url) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White),
                ) { Icon(Icons.Default.Download, null, Modifier.size(18.dp)) }
            }
        }

        // ---- site shortcuts (glass)
        Spacer(Modifier.height(18.dp))
        Column(Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)).padding(vertical = 18.dp, horizontal = 6.dp)) {
            FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = 4, horizontalArrangement = Arrangement.SpaceEvenly) {
                (DEFAULT_SHORTCUTS + custom).forEach { s ->
                    ShortcutTile(
                        s,
                        onClick = { UiBus.openBrowser(s.url) },
                        onLongClick = { if (s.custom) removeTarget = s },
                    )
                }
                Column(
                    Modifier.width(80.dp).padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).clickable { showAdd = true },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(TileBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, "Add", tint = InkColor)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Add", fontSize = 12.sp, color = InkColor)
                }
            }
        }

        Text(
            "$engine  •  Save: Download/$SAVE_FOLDER",
            color = GlassText, fontSize = 11.sp, modifier = Modifier.padding(vertical = 14.dp),
        )
        Spacer(Modifier.height(LocalNavBarPad.current))
    }

    if (showAdd) AddShortcutDialog(onDismiss = { showAdd = false }) { s ->
        custom = custom + s
        Shortcuts.save(ctx, custom)
        showAdd = false
    }
    removeTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("Remove \"${s.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    custom = custom.filterNot { it == s }; Shortcuts.save(ctx, custom); removeTarget = null
                }) { Text("Remove", color = AccentBlue) }
            },
            dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel", color = GlassText) } },
            containerColor = DialogBg,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTile(s: Shortcut, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.width(80.dp).padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var loaded by remember { mutableStateOf(false) }
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(TileBg), contentAlignment = Alignment.Center) {
            if (!loaded) Text(s.name.take(1).uppercase(), color = InkColor, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            // The site's own favicon, like any browser's speed-dial
            AsyncImage(
                model = "https://www.google.com/s2/favicons?domain=${s.domain}&sz=128",
                contentDescription = s.name,
                onSuccess = { loaded = true },
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(s.name, fontSize = 12.sp, color = InkColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AddShortcutDialog(onDismiss: () -> Unit, onAdd: (Shortcut) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(Browser.url.value.ifBlank { "https://" }) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogBg,
        title = { Text("Add a site") },
        text = {
            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(url, { url = it }, label = { Text("Link") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val u = url.trim().let { if (it.startsWith("http")) it else "https://$it" }
                val n = name.trim().ifBlank { Cookies.hostOf(u) ?: "Site" }
                if (Cookies.hostOf(u) != null) onAdd(Shortcut(n, u, true))
            }) { Text("Add", color = AccentBlue) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = GlassText) } },
    )
}

@Suppress("unused")
private fun Modifier.outlined() = this.border(1.dp, Card2, RoundedCornerShape(12.dp))
