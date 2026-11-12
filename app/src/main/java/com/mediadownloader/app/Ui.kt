package com.mediadownloader.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// ---- Colores del tema claro ----
val AccentBlue = Color(0xFF007AFF)      // Acento principal (azul iOS)
val InkColor = Color(0xFF1C1C28)        // Texto e íconos oscuros
val Orange = AccentBlue                 // Nombre viejo: otros archivos aún lo usan, ahora es azul
val OrangeSoft = Color(0xFF5AA9FF)
val Gold = Color(0xFFFFB627)
val Bg = Color(0xFFEAF1FF)
val Card1 = Color(0x99FFFFFF)
val Card2 = Color(0x66FFFFFF)
val GlassBorder = Color(0x4D94A3C7)
val Muted = Color(0xFF5B6070)
val Good = Color(0xFF22A559)
val DangerRed = Color(0xFFD93025)
val TrackColor = Color(0x3394A3C7)


/** Alto de la barra inferior flotante: las listas lo usan como espacio extra al final. */
val LocalNavBarPad = compositionLocalOf { 0.dp }

/** Efecto cristal claro: blanco translúcido con borde sutil. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp)): Modifier = this
    .clip(shape)
    .background(Brush.verticalGradient(listOf(Color(0xB3FFFFFF), Color(0x66FFFFFF))))
    .border(1.dp, Brush.verticalGradient(listOf(Color(0xE6FFFFFF), Color(0x4D94A3C7))), shape)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AccentBlue, onPrimary = Color.White,
            primaryContainer = Color(0xFFD6E8FF), onPrimaryContainer = Color(0xFF004A99),
            secondary = Color(0xFF5856D6), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE3E2FA), onSecondaryContainer = Color(0xFF2E2D80),
            background = Color.Transparent, onBackground = InkColor,
            surface = Color.Transparent, onSurface = InkColor,
            surfaceVariant = Card2, onSurfaceVariant = Muted,
            surfaceContainer = Card1, surfaceContainerHigh = Color(0xF2FFFFFF), surfaceContainerHighest = Color(0xF7FFFFFF),
            surfaceContainerLow = Color(0xFAF4F7FF), surfaceContainerLowest = Color.Transparent,
            outline = Color(0xFF8A94A8), outlineVariant = Color(0x3394A3C7),
            error = DangerRed,
        ),
    ) {
        // Fondo con degradado pastel detrás de todas las pantallas (así el cristal se nota)
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFFD6E6FF), Color(0xFFE6DCFF), Color(0xFFD3F3EE)))
                )
            )
            CompositionLocalProvider(
                LocalContentColor provides InkColor,
                content = content,
            )
        }
    }
}

/** Thumbnail with play/duration overlay — remote image, local video frame, or a music icon. */
@Composable
fun Thumb(model: Any?, audio: Boolean, durationSec: Long, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(Card2)) {
        if (model != null) {
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (model == null && audio) {
            Icon(Icons.Default.MusicNote, null, tint = AccentBlue, modifier = Modifier.align(Alignment.Center).size(28.dp))
        }
        if (durationSec > 0 || !audio) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(5.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(11.dp))
                    if (durationSec > 0) Text(formatDuration(durationSec), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

fun formatDuration(s: Long): String {
    val h = s / 3600; val m = (s % 3600) / 60; val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun humanSize(b: Long): String = when {
    b <= 0 -> "—"
    b >= 1L shl 30 -> "%.2f GB".format(b / (1L shl 30).toDouble())
    b >= 1L shl 20 -> "%.2f MB".format(b / (1L shl 20).toDouble())
    else -> "%.0f KB".format(b / 1024.0)
}

fun compactNumber(n: Long): String = when {
    n >= 1_000_000_000 -> "%.1fB".format(n / 1e9)
    n >= 1_000_000 -> "%.1fM".format(n / 1e6)
    n >= 1_000 -> "%.1fK".format(n / 1e3)
    else -> n.toString()
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()

/** Play inside the app's own player. */
fun play(ctx: Context, uri: Uri, title: String) {
    ctx.startActivity(Intent(ctx, PlayerActivity::class.java).setData(uri).putExtra("title", title))
}

fun openExternal(ctx: Context, uri: Uri, mime: String) {
    val i = Intent(Intent.ACTION_VIEW).setDataAndType(uri, mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try { ctx.startActivity(i) } catch (e: ActivityNotFoundException) { toast(ctx, "No app found to open this") }
}

fun shareFile(ctx: Context, uri: Uri, mime: String, name: String) {
    val i = Intent(Intent.ACTION_SEND).setType(mime)
        .putExtra(Intent.EXTRA_STREAM, uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, name))
}
