package com.falah.cascade

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Rational
import android.view.TextureView
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.SubtitleView
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

class PlayerActivity : ComponentActivity() {
    private var future: ListenableFuture<MediaController>? = null
    val controller = mutableStateOf<MediaController?>(null)
    val launchIntent = mutableStateOf<Intent?>(null)
    val inPip = mutableStateOf(false)
    val audioMode = mutableStateOf(false)
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= 28) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        launchIntent.value = intent

        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val f = MediaController.Builder(this, token).buildAsync()
        future = f
        f.addListener({
            try { controller.value = f.get() } catch (_: Exception) { finish() }
        }, ContextCompat.getMainExecutor(this))

        setContent {
            CascadeTheme(2) {
                val c = controller.value
                if (c == null) Box(Modifier.fillMaxSize().background(Color.Black))
                else PlayerScreen(this@PlayerActivity, c, prefs)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        launchIntent.value = intent
    }

    override fun onResume() { super.onResume(); hideBars() }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideBars() }

    private fun hideBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onUserLeaveHint() {
        val c = controller.value
        if (c != null && c.isPlaying && !audioMode.value) enterPip(c.videoSize)
    }

    fun enterPip(v: VideoSize) {
        if (Build.VERSION.SDK_INT < 26) return
        val w = if (v.width > 0) v.width else 16
        val h = if (v.height > 0) v.height else 9
        val r = (w.toFloat() / h).coerceIn(0.5f, 2.3f)
        try {
            enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(Rational((r * 1000).toInt(), 1000)).build())
        } catch (_: Exception) {
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip.value = isInPictureInPictureMode
    }

    fun saveProgress() {
        val c = controller.value ?: return
        val id = c.currentMediaItem?.mediaId ?: return
        if (id.isNotEmpty()) prefs.save(id, c.currentPosition, maxOf(c.duration, 0L))
    }

    override fun onStop() {
        super.onStop()
        saveProgress()
        if (!audioMode.value) controller.value?.pause()
    }

    override fun onDestroy() {
        if (isFinishing && !audioMode.value) {
            controller.value?.run { stop(); clearMediaItems() }
        }
        future?.let { MediaController.releaseFuture(it) }
        super.onDestroy()
    }
}

// ------------------------------------------------------------------------------------------
// Player UI
// ------------------------------------------------------------------------------------------

@Composable
private fun PlayerScreen(act: PlayerActivity, c: MediaController, prefs: Prefs) {
    val ctx: Context = act
    val intent by act.launchIntent
    val inPip by act.inPip
    val audioMode by act.audioMode

    var isPlaying by remember { mutableStateOf(c.isPlaying) }
    var buffering by remember { mutableStateOf(c.playbackState == Player.STATE_BUFFERING) }
    var pos by remember { mutableLongStateOf(c.currentPosition) }
    var dur by remember { mutableLongStateOf(maxOf(c.duration, 0L)) }
    var speed by remember { mutableFloatStateOf(c.playbackParameters.speed) }
    var title by remember { mutableStateOf(c.mediaMetadata.title?.toString() ?: "") }
    var videoSize by remember { mutableStateOf(c.videoSize) }
    var cues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var tracks by remember { mutableStateOf(c.currentTracks) }
    var itemCount by remember { mutableIntStateOf(c.mediaItemCount) }
    var repeat by remember { mutableIntStateOf(c.repeatMode) }

    var resizeMode by remember { mutableIntStateOf(0) } // 0 fit, 1 stretch, 2 zoom
    var showControls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var userAudio by remember { mutableStateOf(false) }
    var portraitLock by remember { mutableStateOf<Boolean?>(null) }
    var dialog by remember { mutableStateOf("") }
    var interaction by remember { mutableIntStateOf(0) }
    var sleepMin by remember { mutableIntStateOf(0) }
    var seekSec by remember { mutableIntStateOf(prefs.seekSec) }
    var fb by remember { mutableStateOf<Triple<Boolean, Int, Int>?>(null) }
    var swipeTarget by remember { mutableStateOf<Long?>(null) }
    var gauge by remember { mutableStateOf<Pair<Boolean, Float>?>(null) }
    var boost by remember { mutableStateOf(false) }
    var prevSpeed by remember { mutableFloatStateOf(1f) }
    val textureRef = remember { mutableStateOf<TextureView?>(null) }

    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var vol by remember { mutableFloatStateOf(am.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var brightness by remember {
        val b = act.window.attributes.screenBrightness
        mutableFloatStateOf(if (b < 0f) 0.5f else b)
    }

    // ---- player listener ----
    DisposableEffect(c) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying_: Boolean) { isPlaying = isPlaying_ }
            override fun onPlaybackStateChanged(state: Int) { buffering = state == Player.STATE_BUFFERING }
            override fun onMediaMetadataChanged(m: MediaMetadata) { title = m.title?.toString() ?: title }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                itemCount = c.mediaItemCount
                title = item?.mediaMetadata?.title?.toString() ?: ""
                val a = item?.mediaMetadata?.extras?.getBoolean("audio") ?: false
                act.audioMode.value = a || userAudio
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { itemCount = c.mediaItemCount }
            override fun onVideoSizeChanged(v: VideoSize) { videoSize = v }
            override fun onCues(cueGroup: CueGroup) { cues = cueGroup.cues }
            override fun onTracksChanged(t: Tracks) { tracks = t }
            override fun onPlaybackParametersChanged(p: PlaybackParameters) { speed = p.speed }
            override fun onRepeatModeChanged(r: Int) { repeat = r }
        }
        c.addListener(l)
        onDispose { c.removeListener(l) }
    }

    // ---- progress polling + save every ~3s ----
    LaunchedEffect(c) {
        var tick = 0
        while (true) {
            pos = c.currentPosition
            dur = maxOf(c.duration, 0L)
            if (++tick % 10 == 0 && c.isPlaying) {
                c.currentMediaItem?.let { if (it.mediaId.isNotEmpty()) prefs.save(it.mediaId, pos, dur) }
            }
            delay(300)
        }
    }

    // ---- load what we were asked to play ----
    LaunchedEffect(intent) {
        val data = intent?.data
        val list: List<Media>
        var start = 0
        if (data != null) {
            val type = ctx.contentResolver.getType(data) ?: ""
            list = listOf(Media(0, data, displayName(ctx, data), 0, 0, type.startsWith("audio") || isAudioExt(data.toString()), 0))
        } else if (Queue.items.isNotEmpty()) {
            list = Queue.items
            start = Queue.index.coerceIn(0, Queue.items.lastIndex)
        } else return@LaunchedEffect
        Queue.items = emptyList()

        val from = (start - 20).coerceAtLeast(0)
        val slice = list.subList(from, minOf(list.size, start + 80))
        val first = slice[start - from]
        val firstId = first.uri.toString()
        userAudio = false
        act.audioMode.value = first.isAudio

        if (c.mediaItemCount > 0 && c.currentMediaItem?.mediaId == firstId) return@LaunchedEffect // already playing it

        val items = slice.map { m ->
            MediaItem.Builder()
                .setUri(m.uri)
                .setMediaId(m.uri.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(m.title)
                        .setExtras(Bundle().apply { putBoolean("audio", m.isAudio) }).build(),
                ).build()
        }
        val saved = prefs.pos(firstId)
        val d = prefs.dur(firstId)
        val resume = if (saved > 5000 && (d <= 0 || saved < d * 0.97)) saved else 0L
        c.setMediaItems(items, start - from, resume)
        c.prepare()
        c.play()
        if (resume > 0) Toast.makeText(ctx, "Resumed from ${fmt(resume)}", Toast.LENGTH_SHORT).show()
    }

    // ---- play as audio: turn the video track off/on ----
    LaunchedEffect(audioMode) {
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, audioMode).build()
    }

    // ---- orientation ----
    LaunchedEffect(videoSize, audioMode, portraitLock) {
        val vertical = audioMode || (videoSize.width > 0 && videoSize.height > videoSize.width)
        val wantPortrait = portraitLock ?: vertical
        act.requestedOrientation =
            if (wantPortrait) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    val isPortrait = portraitLock ?: (audioMode || (videoSize.width > 0 && videoSize.height > videoSize.width))

    // ---- auto-hide controls / feedback / sleep ----
    LaunchedEffect(showControls, isPlaying, interaction, dialog) {
        if (showControls && isPlaying && dialog.isEmpty()) { delay(3500); showControls = false }
    }
    LaunchedEffect(fb) { if (fb != null) { delay(800); fb = null } }
    LaunchedEffect(gauge) { if (gauge != null) { delay(900); gauge = null } }
    LaunchedEffect(sleepMin) { if (sleepMin > 0) { delay(sleepMin * 60_000L); c.pause(); sleepMin = 0 } }

    // ---- helpers ----
    fun seekBy(ms: Long) {
        val d = c.duration
        var t = c.currentPosition + ms
        t = if (d > 0) t.coerceIn(0L, d) else t.coerceAtLeast(0L)
        c.seekTo(t)
    }
    fun doubleTap(forward: Boolean) {
        seekBy(if (forward) seekSec * 1000L else -seekSec * 1000L)
        val prev = fb
        val acc = if (prev != null && prev.first == forward) prev.second + seekSec else seekSec
        fb = Triple(forward, acc, (prev?.third ?: 0) + 1)
    }
    fun togglePlay() {
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }
    fun screenshot() {
        val tv = textureRef.value
        val bmp = if (tv != null && videoSize.width > 0 && videoSize.height > 0) tv.getBitmap(videoSize.width, videoSize.height) else tv?.bitmap
        val ok = bmp != null && saveShot(ctx, bmp)
        Toast.makeText(ctx, if (ok) "Screenshot saved to Pictures/Cascade" else "Couldn't take screenshot", Toast.LENGTH_SHORT).show()
    }
    fun addSubtitle(uri: Uri) {
        val cur = c.currentMediaItem ?: return
        val name = displayName(ctx, uri).lowercase() + (uri.lastPathSegment?.lowercase() ?: "")
        val mime = when {
            name.contains(".vtt") -> MimeTypes.TEXT_VTT
            name.contains(".ass") || name.contains(".ssa") -> MimeTypes.TEXT_SSA
            name.contains(".ttml") || name.contains(".xml") -> MimeTypes.APPLICATION_TTML
            else -> MimeTypes.APPLICATION_SUBRIP
        }
        val sub = MediaItem.SubtitleConfiguration.Builder(uri).setMimeType(mime).setLanguage("und")
            .setLabel("External subtitle").setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
        val p = c.currentPosition
        val i = c.currentMediaItemIndex
        val item = MediaItem.Builder().setUri(Uri.parse(cur.mediaId)).setMediaId(cur.mediaId)
            .setMediaMetadata(cur.mediaMetadata).setSubtitleConfigurations(listOf(sub)).build()
        c.replaceMediaItem(i, item)
        c.seekTo(i, p)
        c.trackSelectionParameters = c.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false).build()
        Toast.makeText(ctx, "Subtitle loaded", Toast.LENGTH_SHORT).show()
    }
    val subPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) addSubtitle(u) }

    BackHandler(enabled = locked) { showControls = true }

    val isAudioItem = remember(title, itemCount) { c.currentMediaItem?.mediaMetadata?.extras?.getBoolean("audio") == true }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ---------------- video / audio art ----------------
        if (audioMode) {
            AudioArt(title)
        } else {
            BoxWithConstraints(Modifier.fillMaxSize().clipToBounds(), contentAlignment = Alignment.Center) {
                val cw = maxWidth
                val ch = maxHeight
                val ar = if (videoSize.height > 0 && videoSize.width > 0) videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height else 16f / 9f
                var w = cw
                var h = ch
                when (resizeMode) {
                    0 -> if (cw / ch > ar) { h = ch; w = ch * ar } else { w = cw; h = cw / ar }
                    2 -> if (cw / ch > ar) { w = cw; h = cw / ar } else { h = ch; w = ch * ar }
                }
                AndroidView(
                    factory = { x -> TextureView(x).also { tv -> c.setVideoTextureView(tv); textureRef.value = tv } },
                    onRelease = { tv -> c.clearVideoTextureView(tv); if (textureRef.value === tv) textureRef.value = null },
                    modifier = Modifier.requiredSize(w, h),
                )
            }
        }

        // ---------------- subtitles ----------------
        AndroidView(
            factory = { x ->
                SubtitleView(x).apply {
                    setBottomPaddingFraction(0.1f)
                    setApplyEmbeddedStyles(true)
                    setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.1f)
                    setStyle(
                        CaptionStyleCompat(
                            android.graphics.Color.WHITE, android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
                            CaptionStyleCompat.EDGE_TYPE_OUTLINE, android.graphics.Color.BLACK, null,
                        ),
                    )
                }
            },
            update = { it.setCues(cues) },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPip) {
            // ---------------- gestures ----------------
            Box(
                Modifier.fillMaxSize()
                    .pointerInput(locked) {
                        detectTapGestures(
                            onPress = { tryAwaitRelease(); if (boost) { boost = false; c.setPlaybackSpeed(prevSpeed) } },
                            onLongPress = {
                                if (!locked && c.isPlaying) { prevSpeed = c.playbackParameters.speed; boost = true; c.setPlaybackSpeed(2f) }
                            },
                            onDoubleTap = { o -> if (!locked && !audioMode) doubleTap(o.x > size.width / 2) },
                            onTap = { showControls = !showControls; interaction++ },
                        )
                    }
                    .pointerInput(locked, audioMode) {
                        if (!locked && !audioMode) detectHorizontalDragGestures(
                            onDragStart = { swipeTarget = c.currentPosition },
                            onDragEnd = { swipeTarget?.let { c.seekTo(it) }; swipeTarget = null },
                            onDragCancel = { swipeTarget = null },
                            onHorizontalDrag = { change, dx ->
                                change.consume()
                                if (dur > 0) swipeTarget = ((swipeTarget ?: c.currentPosition) + dx / size.width * 90_000f).toLong().coerceIn(0L, dur)
                            },
                        )
                    }
                    .pointerInput(locked, audioMode) {
                        var left = true
                        if (!locked && !audioMode) detectVerticalDragGestures(
                            onDragStart = { o -> left = o.x < size.width / 2 },
                            onDragEnd = { gauge = null },
                            onDragCancel = { gauge = null },
                            onVerticalDrag = { change, dy ->
                                change.consume()
                                val delta = -dy / size.height * 1.3f
                                if (left) {
                                    brightness = (brightness + delta).coerceIn(0.02f, 1f)
                                    val lp = act.window.attributes
                                    lp.screenBrightness = brightness
                                    act.window.attributes = lp
                                    gauge = true to brightness
                                } else {
                                    vol = (vol + delta * maxVol).coerceIn(0f, maxVol.toFloat())
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, vol.roundToInt(), 0)
                                    gauge = false to vol / maxVol
                                }
                            },
                        )
                    },
            )

            // ---------------- feedback overlays ----------------
            fb?.let { (fwd, acc, _) ->
                Box(Modifier.fillMaxSize().padding(horizontal = 64.dp), contentAlignment = if (fwd) Alignment.CenterEnd else Alignment.CenterStart) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Replay, null, tint = Color.White, modifier = Modifier.size(40.dp).scale(if (fwd) -1f else 1f, 1f))
                        Text(if (fwd) "+${acc}s" else "-${acc}s", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            swipeTarget?.let { t ->
                val delta = (t - pos) / 1000
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "${fmt(t)}  (${if (delta >= 0) "+" else "-"}${abs(delta)}s)", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            gauge?.let { (isBright, level) ->
                Box(Modifier.fillMaxSize().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) {
                    Row(
                        Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (isBright) Icons.Filled.BrightnessMedium else Icons.Filled.VolumeUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Box(Modifier.width(110.dp).height(4.dp).background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))) {
                            Box(Modifier.fillMaxWidth(level.coerceIn(0f, 1f)).height(4.dp).background(Brand, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
            if (boost) {
                Box(Modifier.fillMaxSize().padding(top = 28.dp), contentAlignment = Alignment.TopCenter) {
                    Text("2x  ▶▶", color = Color.White, fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 6.dp))
                }
            }

            // ---------------- lock ----------------
            if (locked && showControls) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp), contentAlignment = Alignment.CenterStart) {
                    CircleBtn(56.dp, { locked = false; showControls = true }) { Icon(Icons.Filled.LockOpen, "Unlock", tint = Color.White) }
                }
            }

            // ---------------- controls ----------------
            AnimatedVisibility(visible = (showControls || audioMode) && !locked, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.78f))))
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    // top bar
                    Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { act.finish() }) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
                        Spacer(Modifier.weight(1f))
                        if (!audioMode) IconButton(onClick = { screenshot() }) { Icon(Icons.Filled.PhotoCamera, "Screenshot", tint = Color.White) }
                        if (!isAudioItem) IconButton(onClick = { userAudio = !userAudio; act.audioMode.value = userAudio; interaction++ }) {
                            Icon(if (audioMode) Icons.Filled.Videocam else Icons.Filled.Headphones, "Play as audio", tint = if (audioMode) Brand else Color.White)
                        }
                        if (!audioMode) IconButton(onClick = { act.enterPip(videoSize) }) { Icon(Icons.Filled.PictureInPictureAlt, "Picture in picture", tint = Color.White) }
                        if (!audioMode) IconButton(onClick = { portraitLock = !isPortrait }) { Icon(Icons.Filled.ScreenRotation, "Rotate", tint = Color.White) }
                        IconButton(onClick = { locked = true; showControls = true; interaction++ }) { Icon(Icons.Filled.Lock, "Lock", tint = Color.White) }
                    }

                    // centre transport
                    Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (itemCount > 1) IconButton(onClick = { c.seekToPreviousMediaItem() }) { Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White, modifier = Modifier.size(32.dp)) }
                        SkipBtn(false, seekSec) { seekBy(-seekSec * 1000L); interaction++ }
                        Box(
                            Modifier.size(76.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)).clickable { togglePlay(); interaction++ },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (buffering) CircularProgressIndicator(color = Brand, strokeWidth = 3.dp, modifier = Modifier.size(76.dp))
                            Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play / pause", tint = Color.White, modifier = Modifier.size(42.dp))
                        }
                        SkipBtn(true, seekSec) { seekBy(seekSec * 1000L); interaction++ }
                        if (itemCount > 1) IconButton(onClick = { c.seekToNextMediaItem() }) { Icon(Icons.Filled.SkipNext, "Next", tint = Color.White, modifier = Modifier.size(32.dp)) }
                    }

                    // bottom
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
                        Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(14.dp))
                        SeekBar(pos, dur) { c.seekTo(it); interaction++ }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(fmt(pos), color = Color.White, fontSize = 12.sp)
                            Spacer(Modifier.weight(1f))
                            Text(
                                speedLabel(speed), color = if (abs(speed - 1f) > 0.01f) Brand else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { dialog = "speed" }.padding(horizontal = 10.dp, vertical = 8.dp),
                            )
                            IconButton(onClick = { dialog = "sub" }) { Icon(Icons.Filled.Subtitles, "Subtitles", tint = Color.White) }
                            IconButton(onClick = { dialog = "audio" }) { Icon(Icons.Filled.Audiotrack, "Audio track", tint = Color.White) }
                            if (!audioMode) IconButton(onClick = {
                                resizeMode = (resizeMode + 1) % 3
                                Toast.makeText(ctx, listOf("Fit", "Stretch", "Zoom")[resizeMode], Toast.LENGTH_SHORT).show()
                            }) { Icon(Icons.Filled.AspectRatio, "Aspect ratio", tint = Color.White) }
                            IconButton(onClick = { dialog = "more" }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
                            Spacer(Modifier.width(8.dp))
                            Text("-" + fmt(dur - pos), color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // ---------------- dialogs ----------------
    val close = { dialog = "" }
    when (dialog) {
        "speed" -> AlertDialog(
            onDismissRequest = close,
            confirmButton = { TextButton(onClick = close) { Text("Done") } },
            title = { Text("Playback speed") },
            text = {
                Column {
                    Text(speedLabel(speed), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Brand)
                    Slider(value = speed, onValueChange = { c.setPlaybackSpeed(((it * 20).roundToInt() / 20f).coerceIn(0.25f, 4f)) }, valueRange = 0.25f..4f)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f).forEach { s ->
                            FilterChip(selected = abs(speed - s) < 0.01f, onClick = { c.setPlaybackSpeed(s) }, label = { Text(speedLabel(s)) }, colors = brandChip())
                        }
                    }
                }
            },
        )
        "audio" -> TrackDialog(
            "Audio track", tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }, null, null,
            onPick = { g ->
                c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                dialog = ""
            }, onDismiss = close,
        )
        "sub" -> TrackDialog(
            "Subtitles", tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT },
            tracks.groups.none { it.type == C.TRACK_TYPE_TEXT && it.isSelected },
            {
                c.trackSelectionParameters = c.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                dialog = ""
            },
            onPick = { g ->
                c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                dialog = ""
            }, onDismiss = close,
            extra = { TextButton(onClick = { dialog = ""; subPicker.launch(arrayOf("*/*")) }) { Text("Load subtitle file…") } },
        )
        "more" -> AlertDialog(
            onDismissRequest = close,
            confirmButton = { TextButton(onClick = close) { Text("Done") } },
            title = { Text("More") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Label("Repeat")
                    ChipRow(listOf("Off", "One", "All"), repeat) { c.repeatMode = it }
                    Label("Sleep timer")
                    ChipRow(listOf("Off", "15 min", "30 min", "45 min", "60 min"), listOf(0, 15, 30, 45, 60).indexOf(sleepMin)) { sleepMin = listOf(0, 15, 30, 45, 60)[it] }
                    Label("Double-tap / skip step")
                    ChipRow(listOf("5 s", "10 s", "15 s", "30 s"), listOf(5, 10, 15, 30).indexOf(seekSec)) {
                        seekSec = listOf(5, 10, 15, 30)[it]; prefs.seekSec = seekSec
                    }
                }
            },
        )
    }
}

// ------------------------------------------------------------------------------------------
// Small composables
// ------------------------------------------------------------------------------------------

@Composable
private fun brandChip() = FilterChipDefaults.filterChipColors(selectedContainerColor = Brand, selectedLabelColor = Color.White)

@Composable
private fun Label(t: String) {
    Text(t, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, s -> FilterChip(selected = selected == i, onClick = { onPick(i) }, label = { Text(s) }, colors = brandChip()) }
    }
}

@Composable
private fun CircleBtn(size: androidx.compose.ui.unit.Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun SkipBtn(forward: Boolean, sec: Int, onClick: () -> Unit) {
    CircleBtn(54.dp, onClick) {
        Icon(Icons.Filled.Replay, null, tint = Color.White, modifier = Modifier.size(34.dp).scale(if (forward) -1f else 1f, 1f))
        Text("$sec", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AudioArt(title: String) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A2314), Color(0xFF0D0D0F)))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            LogoMark(190.dp)
            Spacer(Modifier.height(24.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrackDialog(
    title: String,
    groups: List<Tracks.Group>,
    offSelected: Boolean?,
    onOff: (() -> Unit)?,
    onPick: (Tracks.Group) -> Unit,
    onDismiss: () -> Unit,
    extra: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (onOff != null) TrackRow("Off", offSelected == true, onOff)
                groups.forEachIndexed { i, g ->
                    val f = g.getTrackFormat(0)
                    val lang = f.language?.takeIf { it != "und" }?.let { Locale(it).displayLanguage }
                    val codec = f.sampleMimeType?.substringAfter('/')?.uppercase()
                    val ch = if (f.channelCount > 0) "${f.channelCount}ch" else null
                    val label = listOfNotNull(f.label ?: lang ?: "Track ${i + 1}", codec, ch).joinToString(" · ")
                    TrackRow(label, g.isSelected) { onPick(g) }
                }
                if (groups.isEmpty() && onOff == null) Text("No tracks available")
                extra?.invoke()
            }
        },
    )
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

private fun speedLabel(s: Float): String = "%.2f".format(Locale.US, s).trimEnd('0').trimEnd('.') + "x"

/** Orange seek bar with a time chip on the thumb, like the reference design. */
@Composable
private fun SeekBar(pos: Long, dur: Long, onSeek: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    var w by remember { mutableIntStateOf(1) }
    val frac = drag ?: if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val shown = if (drag != null) (drag!! * dur).toLong() else pos
    Box(
        Modifier.fillMaxWidth().height(34.dp).onSizeChanged { w = it.width.coerceAtLeast(1) }
            .pointerInput(dur) { detectTapGestures { o -> if (dur > 0) onSeek((o.x / size.width * dur).toLong()) } }
            .pointerInput(dur) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = (it.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { drag?.let { f -> if (dur > 0) onSeek((f * dur).toLong()) }; drag = null },
                    onDragCancel = { drag = null },
                    onHorizontalDrag = { ch, _ -> ch.consume(); drag = (ch.position.x / size.width).coerceIn(0f, 1f) },
                )
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cy = size.height / 2
            val th = 3.dp.toPx()
            drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th / 2))
            drawRoundRect(Brand, Offset(0f, cy - th / 2), Size(size.width * frac, th), CornerRadius(th / 2))
            val tw = 3.dp.toPx()
            drawRoundRect(Color.White, Offset(size.width * frac - tw / 2, cy - 8.dp.toPx()), Size(tw, 16.dp.toPx()), CornerRadius(tw / 2))
        }
        Text(
            fmt(shown), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset((frac * w - 24.dp.toPx()).coerceIn(0f, (w - 48.dp.toPx()).coerceAtLeast(0f)).roundToInt(), -22.dp.roundToPx()) }
                .background(Brand, RoundedCornerShape(6.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
    }
}

private fun saveShot(ctx: Context, bmp: Bitmap): Boolean = try {
    val name = "Cascade_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
    val v = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= 29) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Cascade")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val r = ctx.contentResolver
    val uri = r.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)!!
    r.openOutputStream(uri)!!.use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
    if (Build.VERSION.SDK_INT >= 29) {
        v.clear(); v.put(MediaStore.Images.Media.IS_PENDING, 0); r.update(uri, v, null, null)
    }
    true
} catch (e: Exception) {
    false
}
