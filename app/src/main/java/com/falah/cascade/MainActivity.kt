package com.falah.cascade

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = Prefs(this)
        setContent {
            var mode by remember { mutableIntStateOf(prefs.theme) }
            CascadeTheme(mode) {
                HomeScreen(prefs, mode) {
                    mode = it
                    prefs.theme = it
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(prefs: Prefs, mode: Int, onMode: (Int) -> Unit) {
    val ctx = LocalContext.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val view = LocalView.current
    SideEffect {
        val w = (view.context as Activity).window
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(w, view).isAppearanceLightNavigationBars = !dark
    }

    // ---- permissions ----
    val perms = remember {
        if (Build.VERSION.SDK_INT >= 33)
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    fun has(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    fun hasAccess() =
        if (Build.VERSION.SDK_INT >= 33) has(Manifest.permission.READ_MEDIA_VIDEO) || has(Manifest.permission.READ_MEDIA_AUDIO)
        else has(Manifest.permission.READ_EXTERNAL_STORAGE)

    var granted by remember { mutableStateOf(hasAccess()) }
    var refresh by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        granted = hasAccess(); refresh++
    }
    LaunchedEffect(Unit) { if (!granted) permLauncher.launch(perms) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val o = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { granted = hasAccess(); refresh++ }
        }
        lifecycle.addObserver(o)
        onDispose { lifecycle.removeObserver(o) }
    }

    // ---- data ----
    var all by remember { mutableStateOf<List<Media>>(emptyList()) }
    LaunchedEffect(refresh, granted) {
        if (granted) all = withContext(Dispatchers.IO) { Library.load(ctx) }
    }

    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var sortKey by remember { mutableIntStateOf(0) }
    var sortMenu by remember { mutableStateOf(false) }
    var urlDialog by remember { mutableStateOf(false) }

    val videos = remember(all, query, sortKey) { sorted(all.filter { !it.isAudio && it.title.contains(query, true) }, sortKey) }
    val audios = remember(all, query, sortKey) { sorted(all.filter { it.isAudio && it.title.contains(query, true) }, sortKey) }
    val recent = remember(all, refresh) { prefs.recent(all) }

    val loader = remember {
        ImageLoader.Builder(ctx).components { add(VideoFrameDecoder.Factory()) }.crossfade(true).build()
    }

    fun open(list: List<Media>, i: Int) {
        Queue.items = list; Queue.index = i
        ctx.startActivity(Intent(ctx, PlayerActivity::class.java))
    }
    fun openUri(u: Uri) {
        ctx.startActivity(Intent(ctx, PlayerActivity::class.java).setData(u))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) openUri(u) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        // top bar
        Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            LogoMark(36.dp)
            Spacer(Modifier.width(10.dp))
            Text("Cascade", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { picker.launch(arrayOf("video/*", "audio/*")) }) { Icon(Icons.Filled.FolderOpen, "Open file", tint = MaterialTheme.colorScheme.onBackground) }
            IconButton(onClick = { urlDialog = true }) { Icon(Icons.Filled.Link, "Open URL", tint = MaterialTheme.colorScheme.onBackground) }
            Box {
                IconButton(onClick = { sortMenu = true }) { Icon(Icons.Filled.Sort, "Sort", tint = MaterialTheme.colorScheme.onBackground) }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    listOf("Newest", "Name", "Duration", "Size").forEachIndexed { i, s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = { sortKey = i; sortMenu = false },
                            trailingIcon = if (sortKey == i) ({ Icon(Icons.Filled.Check, null) }) else null,
                        )
                    }
                }
            }
            IconButton(onClick = { onMode((mode + 1) % 3) }) {
                Icon(
                    when (mode) { 1 -> Icons.Filled.LightMode; 2 -> Icons.Filled.DarkMode; else -> Icons.Filled.BrightnessAuto },
                    "Theme", tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        // search
        TextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(26.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        )

        // tabs
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Videos (${all.count { !it.isAudio }})", "Audio (${all.count { it.isAudio }})").forEachIndexed { i, s ->
                FilterChip(
                    selected = tab == i, onClick = { tab = i }, label = { Text(s) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Brand, selectedLabelColor = Color.White),
                )
            }
        }

        // content
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !granted -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LogoMark(72.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("Allow access to your videos and audio so Cascade can list them.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { permLauncher.launch(perms) }) { Text("Allow access") }
                }
                tab == 0 -> if (videos.isEmpty()) Empty("No videos found") else LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    if (recent.isNotEmpty() && query.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                SectionTitle("Continue watching")
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(recent, key = { it.first.uri.toString() }) { (m, p) ->
                                        VideoCard(m, loader, p, Modifier.width(230.dp).aspectRatio(16f / 9f)) { open(listOf(m), 0) }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("All videos") }
                    items(videos.size, key = { videos[it].uri.toString() }) { i ->
                        VideoCard(videos[i], loader, null, Modifier.aspectRatio(0.78f)) { open(videos, i) }
                    }
                }
                else -> if (audios.isEmpty()) Empty("No audio found") else LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    items(audios.size, key = { audios[it].uri.toString() }) { i -> AudioRow(audios[i]) { open(audios, i) } }
                }
            }
        }
    }

    if (urlDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { urlDialog = false },
            title = { Text("Play network stream") },
            text = { TextField(value = url, onValueChange = { url = it }, placeholder = { Text("https://…") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { if (url.isNotBlank()) openUri(Uri.parse(url.trim())); urlDialog = false }) { Text("Play") }
            },
            dismissButton = { TextButton(onClick = { urlDialog = false }) { Text("Cancel") } },
        )
    }
}

private fun sorted(l: List<Media>, key: Int): List<Media> = when (key) {
    1 -> l.sortedBy { it.title.lowercase() }
    2 -> l.sortedByDescending { it.durationMs }
    3 -> l.sortedByDescending { it.sizeBytes }
    else -> l.sortedByDescending { it.dateAdded }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
}

@Composable
private fun Empty(t: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun VideoCard(m: Media, loader: ImageLoader, progress: Float?, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).clickable(onClick = onClick)) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(m.uri).videoFrameMillis(3000).crossfade(true).build(),
            imageLoader = loader, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.85f))))
        Column(Modifier.align(Alignment.BottomStart).padding(start = 10.dp, end = 10.dp, bottom = if (progress != null) 14.dp else 10.dp)) {
            Text("${fmt(m.durationMs)} · ${humanSize(m.sizeBytes)}", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
            Text(m.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (progress != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.3f)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(Brand))
        }
    }
}

@Composable
private fun AudioRow(m: Media, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Color(0xFFF69A5C), Color(0xFFEE7E3E)))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.MusicNote, null, tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(m.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
            Text("${fmt(m.durationMs)} · ${humanSize(m.sizeBytes)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
