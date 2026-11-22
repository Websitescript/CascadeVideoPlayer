package com.falah.cascade

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.abs
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
    val dark = isDarkTheme()
    val bg = MaterialTheme.colorScheme.background
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
    var urlDialog by remember { mutableStateOf(false) }

    val videos = remember(all, query, sortKey) { sorted(all.filter { !it.isAudio && it.title.contains(query, true) }, sortKey) }
    val audios = remember(all, query, sortKey) { sorted(all.filter { it.isAudio && it.title.contains(query, true) }, sortKey) }
    val recent = remember(all, refresh) { prefs.recent(all) }

    val loader = remember {
        ImageLoader.Builder(ctx).components { add(VideoFrameDecoder.Factory()) }.crossfade(true).build()
    }

    fun launchPlayer(i: Intent) {
        val opts = ActivityOptions.makeCustomAnimation(ctx, android.R.anim.fade_in, android.R.anim.fade_out)
        ctx.startActivity(i, opts.toBundle())
    }
    fun open(list: List<Media>, i: Int) {
        Queue.items = list; Queue.index = i
        launchPlayer(Intent(ctx, PlayerActivity::class.java))
    }
    fun openUri(u: Uri) {
        launchPlayer(Intent(ctx, PlayerActivity::class.java).setData(u))
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) openUri(u) }

    // ---- hero carousel ----
    val heroItems = remember(recent, videos, query) {
        if (query.isNotEmpty()) emptyList() else (recent.map { it.first } + videos).distinctBy { it.uri }.take(6)
    }
    val progressOf = remember(recent) { recent.associate { it.first.uri to it.second } }
    val heroNow = rememberUpdatedState(heroItems)
    val pager = rememberPagerState { heroNow.value.size }
    val page = pager.currentPage
    val backdrop = if (tab == 0) heroItems.getOrNull(page) else null

    val full: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }

    Box(Modifier.fillMaxSize().background(bg)) {
        // ---------- blurred, cross-fading backdrop ----------
        Crossfade(targetState = backdrop?.uri, animationSpec = tween(800), label = "backdrop") { u ->
            if (u != null) {
                AsyncImage(
                    model = ImageRequest.Builder(ctx).data(u).videoFrameMillis(3000).size(96, 96).build(),
                    imageLoader = loader, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .blur(48.dp, BlurredEdgeTreatment.Unbounded)
                        .graphicsLayer { alpha = if (dark) 0.6f else 0.45f },
                )
            }
        }
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Brand.copy(alpha = 0.25f), Color.Transparent), center = Offset(80f, 0f), radius = 1000f)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to bg.copy(alpha = 0.15f), 0.5f to bg.copy(alpha = 0.8f), 1f to bg)))

        // ---------- content ----------
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        ) {
            item(span = full) {
                Header(
                    all.count { !it.isAudio }, all.count { it.isAudio }, sortKey, { sortKey = it }, mode, onMode,
                    { picker.launch(arrayOf("video/*", "audio/*")) }, { urlDialog = true },
                )
            }
            item(span = full) {
                TextField(
                    value = query, onValueChange = { query = it },
                    placeholder = { Text("Search your library") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = glassFill(), unfocusedContainerColor = glassFill(),
                    ),
                    modifier = Modifier.fillMaxWidth().border(1.dp, glassBorder(), RoundedCornerShape(26.dp)),
                )
            }
            item(span = full) {
                Segmented(tab, listOf("Videos", "Audio")) { tab = it }
            }

            when {
                !granted -> item(span = full) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        LogoMark(84.dp, animated = true)
                        Spacer(Modifier.height(20.dp))
                        Text("Allow access to your videos and audio so Cascade can list them.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { permLauncher.launch(perms) }) { Text("Allow access") }
                    }
                }

                tab == 0 -> {
                    if (heroItems.isNotEmpty()) {
                        item(span = full) {
                            Column {
                                HorizontalPager(
                                    state = pager,
                                    contentPadding = PaddingValues(end = 44.dp),
                                    pageSpacing = 12.dp,
                                    modifier = Modifier.fillMaxWidth(),
                                ) { p ->
                                    val m = heroItems.getOrNull(p) ?: return@HorizontalPager
                                    val off = (pager.currentPage - p) + pager.currentPageOffsetFraction
                                    val a = abs(off).coerceAtMost(1f)
                                    HeroCard(
                                        m, loader, progressOf[m.uri],
                                        Modifier.graphicsLayer {
                                            val s = 1f - a * 0.07f
                                            scaleX = s; scaleY = s; alpha = 1f - a * 0.4f
                                        },
                                    ) { open(heroItems, p) }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    repeat(heroItems.size) { i ->
                                        val w by animateDpAsState(if (i == page) 22.dp else 6.dp, spring(dampingRatio = 0.7f), label = "dot")
                                        Box(
                                            Modifier.padding(horizontal = 3.dp).height(6.dp).width(w).clip(CircleShape)
                                                .background(if (i == page) Brand else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                    if (videos.isEmpty()) item(span = full) { Empty("No videos found") }
                    else {
                        item(span = full) { SectionTitle("All videos", "${videos.size}") }
                        items(videos.size, key = { videos[it].uri.toString() }) { i ->
                            Appear(i) { VideoCard(videos[i], loader, Modifier.aspectRatio(0.8f)) { open(videos, i) } }
                        }
                    }
                }

                else -> {
                    if (audios.isEmpty()) item(span = full) { Empty("No audio found") }
                    else {
                        item(span = full) { SectionTitle("All audio", "${audios.size}") }
                        items(audios.size, key = { audios[it].uri.toString() }, span = { GridItemSpan(maxLineSpan) }) { i ->
                            Appear(i) { AudioRow(audios[i]) { open(audios, i) } }
                        }
                    }
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
private fun Header(
    videoCount: Int, audioCount: Int,
    sortKey: Int, onSort: (Int) -> Unit,
    mode: Int, onMode: (Int) -> Unit,
    onOpenFile: () -> Unit, onOpenUrl: () -> Unit,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    var plus by remember { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }
    val pop = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f)) }

    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        LogoMark(42.dp, Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value })
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Cascade", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ink)
            Text("$videoCount videos · $audioCount audio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        Box {
            GlassButton({ plus = true }, size = 42.dp) { Icon(Icons.Filled.Add, "Open", tint = ink) }
            DropdownMenu(expanded = plus, onDismissRequest = { plus = false }) {
                DropdownMenuItem(text = { Text("Open file") }, leadingIcon = { Icon(Icons.Filled.FolderOpen, null) }, onClick = { plus = false; onOpenFile() })
                DropdownMenuItem(text = { Text("Play network URL") }, leadingIcon = { Icon(Icons.Filled.Link, null) }, onClick = { plus = false; onOpenUrl() })
            }
        }
        Spacer(Modifier.width(8.dp))
        Box {
            GlassButton({ sortMenu = true }, size = 42.dp) { Icon(Icons.Filled.Sort, "Sort", tint = ink) }
            DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                listOf("Newest", "Name", "Duration", "Size").forEachIndexed { i, s ->
                    DropdownMenuItem(
                        text = { Text(s) },
                        onClick = { onSort(i); sortMenu = false },
                        trailingIcon = if (sortKey == i) ({ Icon(Icons.Filled.Check, null) }) else null,
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        GlassButton({ onMode((mode + 1) % 3) }, size = 42.dp) {
            Crossfade(targetState = mode, animationSpec = tween(250), label = "theme") { m ->
                Icon(
                    when (m) { 1 -> Icons.Filled.LightMode; 2 -> Icons.Filled.DarkMode; else -> Icons.Filled.BrightnessAuto },
                    "Theme", tint = ink,
                )
            }
        }
    }
}

@Composable
private fun Segmented(tab: Int, labels: List<String>, onTab: (Int) -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().height(50.dp).clip(shape).background(glassFill()).border(1.dp, glassBorder(), shape)) {
        val w = maxWidth / labels.size
        val x by animateDpAsState(w * tab, spring(dampingRatio = 0.8f, stiffness = 380f), label = "seg")
        Box(
            Modifier.offset(x = x).width(w).fillMaxHeight().padding(4.dp).clip(RoundedCornerShape(22.dp))
                .background(Brush.horizontalGradient(listOf(BrandLight, Brand))),
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, s ->
                val c by animateColorAsState(if (tab == i) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, tween(250), label = "segc")
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onTab(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(s, color = c, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String, count: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.width(8.dp))
        Text(count, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Brand.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

@Composable
private fun Empty(t: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 56.dp), contentAlignment = Alignment.Center) {
        Text(t, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeroCard(m: Media, loader: ImageLoader, progress: Float?, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier.fillMaxWidth().aspectRatio(1.6f).pressClick(0.97f, onClick).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(m.uri).videoFrameMillis(3000).crossfade(true).build(),
            imageLoader = loader, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))))
        Text(
            if (progress != null) "Continue watching" else "Latest", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.40f)).padding(horizontal = 10.dp, vertical = 5.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(start = 18.dp, end = 76.dp, bottom = 20.dp)) {
            Text(m.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                if (progress != null) "${fmt((m.durationMs * (1f - progress)).toLong())} left" else "${fmt(m.durationMs)} · ${humanSize(m.sizeBytes)}",
                color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp,
            )
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp).size(48.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(BrandLight, Color(0xFFEE7E3E)))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
        if (progress != null) {
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.25f)))
            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(Brand))
        }
    }
}

@Composable
private fun VideoCard(m: Media, loader: ImageLoader, modifier: Modifier, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.pressClick(0.96f, onClick).clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, Color.White.copy(alpha = 0.10f), shape),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(ctx).data(m.uri).videoFrameMillis(3000).crossfade(true).build(),
            imageLoader = loader, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.88f))))
        Text(
            fmt(m.durationMs), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 7.dp, vertical = 3.dp),
        )
        Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(m.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(humanSize(m.sizeBytes), color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun AudioRow(m: Media, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().pressClick(0.97f, onClick).clip(shape).background(glassFill()).border(1.dp, glassBorder(), shape).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(BrandLight, Color(0xFFEE7E3E)))),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.MusicNote, null, tint = Color.White) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(m.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            Text("${fmt(m.durationMs)} · ${humanSize(m.sizeBytes)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.size(36.dp).clip(CircleShape).background(Brand.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayArrow, null, tint = Brand)
        }
    }
}
