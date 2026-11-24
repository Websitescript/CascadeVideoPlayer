package com.falah.cascade

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.BatteryManager
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.media3.session.SessionCommand
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

private val GlassDark = Color.Black.copy(alpha = 0.45f)
private val Edge = Color.White.copy(alpha = 0.14f)

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
    var lastDialog by remember { mutableStateOf("speed") }
    var interaction by remember { mutableIntStateOf(0) }
    var sleepMin by remember { mutableIntStateOf(0) }
    var seekSec by remember { mutableIntStateOf(prefs.seekSec) }
    var fb by remember { mutableStateOf<Triple<Boolean, Int, Int>?>(null) }
    var lastFb by remember { mutableStateOf<Triple<Boolean, Int, Int>?>(null) }
    var swipeTarget by remember { mutableStateOf<Long?>(null) }
    var gauge by remember { mutableStateOf<Triple<Boolean, Float, Int>?>(null) } // brightness?, fill 0..1, percent
    var lastGauge by remember { mutableStateOf(Triple(true, 0f, 0)) }
    var holdSpeed by remember { mutableFloatStateOf(prefs.holdSpeed) }
    var boost by remember { mutableStateOf(false) }
    var prevSpeed by remember { mutableFloatStateOf(1f) }
    val textureRef = remember { mutableStateOf<TextureView?>(null) }

    val am = remember { ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVol = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    val sysPct0 = remember { am.getStreamVolume(AudioManager.STREAM_MUSIC) * 100f / maxVol }
    var volPct by remember { mutableFloatStateOf(if (sysPct0 >= 99.5f) 100f + prefs.boost else sysPct0) } // 0..200
    var sentBoostMb by remember { mutableIntStateOf(-1) }
    var brightness by remember {
        val b = act.window.attributes.screenBrightness
        mutableFloatStateOf(if (b < 0f) 0.5f else b)
    }

    fun applyBoost(pct: Float) {
        val extra = (pct - 100f).coerceIn(0f, 100f)
        val mb = if (extra <= 0f) 0 else (2000.0 * kotlin.math.log10(1.0 + extra / 100.0)).roundToInt()
        if (mb != sentBoostMb) {
            sentBoostMb = mb
            prefs.boost = extra.roundToInt()
            c.sendCustomCommand(SessionCommand("cascade.boost", Bundle.EMPTY), Bundle().apply { putInt("mb", mb) })
        }
    }
    LaunchedEffect(Unit) { applyBoost(volPct) }

    // ---- clock + battery ----
    var clock by remember { mutableStateOf("") }
    var battery by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val f = SimpleDateFormat("h:mm a", Locale.getDefault())
        while (true) { clock = f.format(Date()).lowercase(); delay(10_000) }
    }
    DisposableEffect(Unit) {
        fun read(i: Intent?) {
            if (i == null) return
            val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val sc = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && sc > 0) battery = l * 100 / sc
            val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        }
        val rec = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, i: Intent?) { read(i) }
        }
        read(ctx.registerReceiver(rec, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        onDispose { ctx.unregisterReceiver(rec) }
    }
    val sysInfo = listOfNotNull(
        if (battery >= 0) (if (charging) "⚡" else "") + "$battery%" else null,
        clock.ifEmpty { null },
    ).joinToString("  ")

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
        val it0 = intent
        val streams: List<Uri> = when {
            it0?.data != null -> listOf(it0!!.data!!)
            it0?.action == Intent.ACTION_SEND_MULTIPLE -> it0!!.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
            it0?.action == Intent.ACTION_SEND -> listOfNotNull(it0!!.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            else -> emptyList()
        }
        val list: List<Media>
        var start = 0
        if (streams.isNotEmpty()) {
            list = streams.map { u ->
                val type = ctx.contentResolver.getType(u) ?: ""
                Media(0, u, displayName(ctx, u), 0, 0, type.startsWith("audio") || isAudioExt(u.toString()), 0)
            }
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
    LaunchedEffect(fb) { val cur = fb; if (cur != null) { lastFb = cur; delay(800); fb = null } }
    LaunchedEffect(gauge) { val g = gauge; if (g != null) { lastGauge = g; delay(900); gauge = null } }
    LaunchedEffect(dialog) { if (dialog.isNotEmpty()) lastDialog = dialog }
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
    BackHandler(enabled = dialog.isNotEmpty()) { dialog = "" }

    val isAudioItem = remember(title, itemCount) { c.currentMediaItem?.mediaMetadata?.extras?.getBoolean("audio") == true }
    fun toggleVideo() {
        if (isAudioItem) { Toast.makeText(ctx, "This is an audio file", Toast.LENGTH_SHORT).show(); return }
        userAudio = !userAudio
        act.audioMode.value = userAudio
        Toast.makeText(ctx, if (userAudio) "Audio only" else "Video on", Toast.LENGTH_SHORT).show()
    }
    val showUi = (showControls || audioMode) && !locked
    val shownDialog = if (dialog.isNotEmpty()) dialog else lastDialog
    val subLine = buildString {
        if (itemCount > 1) append("${c.currentMediaItemIndex + 1} of $itemCount")
        if (!audioMode && videoSize.height > 0) { if (isNotEmpty()) append("  ·  "); append("${videoSize.height}p") }
        if (sysInfo.isNotEmpty()) { if (isNotEmpty()) append("  ·  "); append(sysInfo) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // ---------------- video / audio art ----------------
        if (audioMode) {
            AudioArt(title, isPlaying)
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
            update = { it.setCues(cues); it.setBottomPaddingFraction(if (showUi) 0.22f else 0.06f) },
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
                                if (!locked && c.isPlaying) { prevSpeed = c.playbackParameters.speed; boost = true; c.setPlaybackSpeed(holdSpeed) }
                            },
                            onDoubleTap = { o ->
                                if (!locked) {
                                    val fx = o.x / size.width
                                    if (fx < 0.33f) doubleTap(false) else if (fx > 0.67f) doubleTap(true) else toggleVideo()
                                }
                            },
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
                                    gauge = Triple(true, brightness, (brightness * 100).roundToInt())
                                } else {
                                    volPct = (volPct + delta * 160f).coerceIn(0f, 200f)
                                    val sys = (volPct.coerceAtMost(100f) / 100f * maxVol).roundToInt()
                                    am.setStreamVolume(AudioManager.STREAM_MUSIC, sys, 0)
                                    applyBoost(volPct)
                                    gauge = Triple(false, volPct / 200f, volPct.roundToInt())
                                }
                            },
                        )
                    },
            )

            // ---------------- scrim ----------------
            AnimatedVisibility(visible = showUi, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.85f))),
                    ),
                )
            }

            // ---------------- double-tap ripples ----------------
            val curFb = fb ?: lastFb
            SeekRipple(fb?.first == false, false, curFb?.second ?: 0, curFb?.third ?: 0)
            SeekRipple(fb?.first == true, true, curFb?.second ?: 0, curFb?.third ?: 0)

            // ---------------- swipe-seek readout ----------------
            swipeTarget?.let { t ->
                val delta = (t - pos) / 1000
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.clip(RoundedCornerShape(20.dp)).background(GlassDark).border(1.dp, Edge, RoundedCornerShape(20.dp)).padding(horizontal = 22.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(fmt(t), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        Text("${if (delta >= 0) "+" else "-"}${abs(delta)}s", color = Brand, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ---------------- brightness / volume gauges ----------------
            val g = gauge
            val gs = g ?: lastGauge
            AnimatedVisibility(
                visible = g != null && g.first,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 30.dp),
                enter = fadeIn() + slideInHorizontally { -it }, exit = fadeOut(),
            ) { GaugeBar(Icons.Filled.BrightnessMedium, gs.second, gs.third) }
            AnimatedVisibility(
                visible = g != null && !g.first,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 30.dp),
                enter = fadeIn() + slideInHorizontally { it }, exit = fadeOut(),
            ) { GaugeBar(Icons.Filled.VolumeUp, gs.second, gs.third) }

            // ---------------- 2x boost pill ----------------
            AnimatedVisibility(
                visible = boost,
                modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 14.dp),
                enter = fadeIn() + scaleIn(initialScale = 0.8f), exit = fadeOut() + scaleOut(targetScale = 0.8f),
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(GlassDark).border(1.dp, Edge, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.KeyboardDoubleArrowRight, null, tint = Brand, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${speedLabel(holdSpeed)} speed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // ---------------- unlock button ----------------
            AnimatedVisibility(
                visible = locked && showControls,
                modifier = Modifier.align(Alignment.CenterStart).windowInsetsPadding(WindowInsets.safeDrawing).padding(start = 24.dp),
                enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut(),
            ) {
                GlassButton({ locked = false; showControls = true; interaction++ }, size = 58.dp, fill = GlassDark, border = Edge) {
                    Icon(Icons.Filled.LockOpen, "Unlock", tint = Color.White)
                }
            }

            // ---------------- battery + clock (when controls are hidden) ----------------
            AnimatedVisibility(
                visible = !showUi && sysInfo.isNotEmpty(),
                modifier = Modifier.align(Alignment.TopEnd).windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 8.dp, end = 16.dp),
                enter = fadeIn(), exit = fadeOut(),
            ) {
                Text(
                    sysInfo, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }

            // ---------------- top bar ----------------
            AnimatedVisibility(
                visible = showUi,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = slideInVertically(tween(300)) { -it / 2 } + fadeIn(tween(300)),
                exit = slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(220)),
            ) {
                Row(
                    Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GlassButton({ act.finish() }, size = 46.dp, fill = GlassDark, border = Edge) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (subLine.isNotEmpty()) Text(subLine, color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Row(
                        Modifier.clip(RoundedCornerShape(26.dp)).background(GlassDark).border(1.dp, Edge, RoundedCornerShape(26.dp)).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!audioMode) PillIcon(Icons.Filled.PhotoCamera, "Screenshot") { screenshot() }
                        if (!isAudioItem) PillIcon(
                            if (audioMode) Icons.Filled.Videocam else Icons.Filled.Headphones, "Play as audio",
                            if (audioMode) Brand else Color.White,
                        ) { userAudio = !userAudio; act.audioMode.value = userAudio; interaction++ }
                        if (!audioMode) PillIcon(Icons.Filled.PictureInPictureAlt, "Picture in picture") { act.enterPip(videoSize) }
                        if (!audioMode) PillIcon(Icons.Filled.ScreenRotation, "Rotate") { portraitLock = !isPortrait }
                    }
                }
            }

            // ---------------- bottom panel ----------------
            AnimatedVisibility(
                visible = showUi,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(320)) { it / 2 } + fadeIn(tween(320)),
                exit = slideOutVertically(tween(220)) { it / 2 } + fadeOut(tween(220)),
            ) {
                Column(
                    Modifier.windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)).background(GlassDark).border(1.dp, Edge, RoundedCornerShape(28.dp))
                        .padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt(pos), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(54.dp))
                        SeekBar(pos, dur, Modifier.weight(1f)) { c.seekTo(it); interaction++ }
                        Text("-" + fmt(dur - pos), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.width(62.dp))
                    }
                    val transport: @Composable () -> Unit = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (itemCount > 1) GlassButton({ c.seekToPreviousMediaItem(); interaction++ }, size = 40.dp, fill = GlassDark, border = Edge) {
                                Icon(Icons.Filled.SkipPrevious, "Previous", tint = Color.White)
                            }
                            SkipBtn(false, seekSec, 50.dp) { seekBy(-seekSec * 1000L); interaction++ }
                            Box(
                                Modifier.pressClick(0.9f) { togglePlay(); interaction++ }
                                    .shadow(14.dp, CircleShape, ambientColor = Brand, spotColor = Brand)
                                    .size(62.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(BrandLight, Color(0xFFEE7E3E)))),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (buffering) CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(62.dp))
                                AnimatedContent(
                                    targetState = isPlaying,
                                    transitionSpec = { (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith (scaleOut(targetScale = 0.5f) + fadeOut()) },
                                    label = "playpause",
                                ) { p ->
                                    Icon(if (p) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play / pause", tint = Color.White, modifier = Modifier.size(34.dp))
                                }
                            }
                            SkipBtn(true, seekSec, 50.dp) { seekBy(seekSec * 1000L); interaction++ }
                            if (itemCount > 1) GlassButton({ c.seekToNextMediaItem(); interaction++ }, size = 40.dp, fill = GlassDark, border = Edge) {
                                Icon(Icons.Filled.SkipNext, "Next", tint = Color.White)
                            }
                        }
                    }
                    val options: @Composable () -> Unit = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val fast = abs(speed - 1f) > 0.01f
                            Row(
                                Modifier.pressClick(0.94f) { dialog = "speed" }.clip(RoundedCornerShape(50))
                                    .background(if (fast) Brand.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Speed, null, tint = if (fast) Brand else Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(speedLabel(speed), color = if (fast) Brand else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            PillIcon(Icons.Filled.Subtitles, "Subtitles") { dialog = "sub" }
                            PillIcon(Icons.Filled.Audiotrack, "Audio track") { dialog = "audio" }
                            if (!audioMode) PillIcon(Icons.Filled.AspectRatio, "Aspect ratio") {
                                resizeMode = (resizeMode + 1) % 3
                                Toast.makeText(ctx, listOf("Fit", "Stretch", "Zoom")[resizeMode], Toast.LENGTH_SHORT).show()
                            }
                            PillIcon(Icons.Filled.MoreVert, "More") { dialog = "more" }
                        }
                    }
                    if (isPortrait) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { transport() }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PillIcon(Icons.Filled.Lock, "Lock") { locked = true; showControls = true; interaction++ }
                            Spacer(Modifier.weight(1f))
                            options()
                        }
                    } else {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            PillIcon(Icons.Filled.Lock, "Lock") { locked = true; showControls = true; interaction++ }
                            Spacer(Modifier.weight(1f))
                            transport()
                            Spacer(Modifier.weight(1f))
                            options()
                        }
                    }
                }
            }

            // ---------------- side panel ----------------
            AnimatedVisibility(visible = dialog.isNotEmpty(), enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { dialog = "" },
                )
            }
            AnimatedVisibility(
                visible = dialog.isNotEmpty(),
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(200)),
            ) {
                SidePanel(panelTitle(shownDialog), { dialog = "" }) {
                    when (shownDialog) {
                        "speed" -> {
                            Text(speedLabel(speed), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Brand)
                            Slider(
                                value = speed,
                                onValueChange = { c.setPlaybackSpeed(((it * 20).roundToInt() / 20f).coerceIn(0.25f, 4f)) },
                                valueRange = 0.25f..4f,
                                colors = SliderDefaults.colors(thumbColor = Brand, activeTrackColor = Brand, inactiveTrackColor = Color.White.copy(alpha = 0.2f)),
                            )
                            listOf(listOf(0.5f, 0.75f, 1f, 1.25f), listOf(1.5f, 2f, 3f, 4f)).forEach { r ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    r.forEach { s -> OptionChip(speedLabel(s), abs(speed - s) < 0.01f, { c.setPlaybackSpeed(s) }) }
                                }
                            }
                        }
                        "audio" -> TrackList(
                            tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }, null, null,
                            onPick = { g ->
                                c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
                                    .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                            },
                        )
                        "sub" -> {
                            TrackList(
                                tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT },
                                tracks.groups.none { it.type == C.TRACK_TYPE_TEXT && it.isSelected },
                                {
                                    c.trackSelectionParameters = c.trackSelectionParameters.buildUpon().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true).build()
                                },
                                onPick = { g ->
                                    c.trackSelectionParameters = c.trackSelectionParameters.buildUpon()
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .setOverrideForType(TrackSelectionOverride(g.mediaTrackGroup, 0)).build()
                                },
                            )
                            ActionRow("Load subtitle file…", Icons.Filled.FolderOpen) { dialog = ""; subPicker.launch(arrayOf("*/*")) }
                        }
                        else -> {
                            Label("Repeat")
                            ChipRow(listOf("Off", "One", "All"), repeat) { c.repeatMode = it }
                            Label("Sleep timer")
                            ChipRow(listOf("Off", "15 min", "30 min", "45 min", "60 min"), listOf(0, 15, 30, 45, 60).indexOf(sleepMin)) { sleepMin = listOf(0, 15, 30, 45, 60)[it] }
                            Label("Hold-to-speed (press and hold the screen)")
                            ChipRow(listOf("1.5x", "2x", "3x", "4x"), listOf(1.5f, 2f, 3f, 4f).indexOf(holdSpeed)) {
                                holdSpeed = listOf(1.5f, 2f, 3f, 4f)[it]; prefs.holdSpeed = holdSpeed
                            }
                            Label("Double-tap / skip step")
                            ChipRow(listOf("5 s", "10 s", "15 s", "30 s"), listOf(5, 10, 15, 30).indexOf(seekSec)) {
                                seekSec = listOf(5, 10, 15, 30)[it]; prefs.seekSec = seekSec
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------
// Pieces
// ------------------------------------------------------------------------------------------

private fun panelTitle(s: String) = when (s) {
    "speed" -> "Playback speed"
    "audio" -> "Audio track"
    "sub" -> "Subtitles"
    else -> "More"
}

@Composable
private fun PillIcon(icon: ImageVector, desc: String, tint: Color = Color.White, onClick: () -> Unit) {
    Box(Modifier.pressClick(0.82f, onClick).size(44.dp), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SkipBtn(forward: Boolean, sec: Int, size: androidx.compose.ui.unit.Dp = 58.dp, onClick: () -> Unit) {
    var spins by remember { mutableIntStateOf(0) }
    val rot by animateFloatAsState(spins * 360f * (if (forward) 1f else -1f), tween(450), label = "spin")
    GlassButton({ spins++; onClick() }, size = size, fill = GlassDark, border = Edge) {
        Icon(
            Icons.Filled.Replay, null, tint = Color.White,
            modifier = Modifier.size(size * 0.58f).graphicsLayer { scaleX = if (forward) -1f else 1f; rotationZ = rot },
        )
        Text("$sec", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoxScope.SeekRipple(visible: Boolean, forward: Boolean, acc: Int, tick: Int) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(if (forward) Alignment.CenterEnd else Alignment.CenterStart),
        enter = fadeIn(tween(120)), exit = fadeOut(tween(300)),
    ) {
        val sc = remember { Animatable(0.85f) }
        LaunchedEffect(tick) { sc.snapTo(0.85f); sc.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 300f)) }
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.38f)
                .clip(
                    if (forward) RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                    else RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50),
                )
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(Modifier.graphicsLayer { scaleX = sc.value; scaleY = sc.value }, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (forward) Icons.Filled.KeyboardDoubleArrowRight else Icons.Filled.KeyboardDoubleArrowLeft,
                    null, tint = Color.White, modifier = Modifier.size(40.dp),
                )
                Text(if (forward) "+$acc s" else "-$acc s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun GaugeBar(icon: ImageVector, level: Float, percent: Int) {
    val a by animateFloatAsState(level.coerceIn(0f, 1f), tween(120), label = "gauge")
    val hot = percent > 100
    Box(
        Modifier.width(54.dp).height(190.dp).clip(RoundedCornerShape(27.dp)).background(GlassDark).border(1.dp, Edge, RoundedCornerShape(27.dp)),
    ) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(a)
                .background(Brush.verticalGradient(if (hot) listOf(Color(0xFFFF8A5B), Color(0xFFE5483B)) else listOf(BrandLight, Brand))),
        )
        Text("$percent%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        Icon(icon, null, tint = Color.White, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).size(20.dp))
    }
}

@Composable
private fun SidePanel(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
    Column(
        Modifier.fillMaxHeight().width(340.dp).clip(shape).background(Color(0xF2141519)).border(1.dp, Color.White.copy(alpha = 0.10f), shape)
            .windowInsetsPadding(WindowInsets.safeDrawing).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            GlassButton(onClose, size = 38.dp, fill = Color.White.copy(alpha = 0.10f), border = Color.Transparent) {
                Icon(Icons.Filled.Close, "Close", tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(18.dp))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun Label(t: String) {
    Text(t, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun ChipRow(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { i, s -> OptionChip(s, selected == i, { onPick(i) }) }
    }
}

@Composable
private fun TrackList(
    groups: List<Tracks.Group>,
    offSelected: Boolean?,
    onOff: (() -> Unit)?,
    onPick: (Tracks.Group) -> Unit,
) {
    if (onOff != null) TrackRow("Off", offSelected == true, onOff)
    groups.forEachIndexed { i, g ->
        val f = g.getTrackFormat(0)
        val lang = f.language?.takeIf { it != "und" }?.let { Locale(it).displayLanguage }
        val codec = f.sampleMimeType?.substringAfter('/')?.uppercase()
        val ch = if (f.channelCount > 0) "${f.channelCount}ch" else null
        val label = listOfNotNull(f.label ?: lang ?: "Track ${i + 1}", codec, ch).joinToString(" · ")
        TrackRow(label, g.isSelected) { onPick(g) }
    }
    if (groups.isEmpty() && onOff == null) Text("No tracks available", color = Color.White.copy(alpha = 0.6f))
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressClick(0.98f, onClick).clip(RoundedCornerShape(14.dp))
            .background(if (selected) Brand.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = Brand, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ActionRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().pressClick(0.98f, onClick).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.06f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Brand, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun AudioArt(title: String, playing: Boolean) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3A2314), Color(0xFF0D0D0F)))),
        contentAlignment = BiasAlignment(0f, -0.4f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(340.dp).background(Brush.radialGradient(listOf(Brand.copy(alpha = 0.32f), Color.Transparent))))
                LogoMark(190.dp, animated = playing)
            }
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun speedLabel(s: Float): String = "%.2f".format(Locale.US, s).trimEnd('0').trimEnd('.') + "x"

/** Orange seek bar: track grows and a time chip appears while dragging. */
@Composable
private fun SeekBar(pos: Long, dur: Long, modifier: Modifier, onSeek: (Long) -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    var w by remember { mutableIntStateOf(1) }
    val dragging = drag != null
    val frac = drag ?: if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val th by animateFloatAsState(if (dragging) 8f else 4f, spring(), label = "th")
    val tr by animateFloatAsState(if (dragging) 11f else 6f, spring(dampingRatio = 0.5f), label = "tr")
    val chipA by animateFloatAsState(if (dragging) 1f else 0f, tween(150), label = "chip")
    val shown = drag?.let { (it * dur).toLong() } ?: pos
    Box(
        modifier.height(40.dp).onSizeChanged { w = it.width.coerceAtLeast(1) }
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
            val t = th.dp.toPx()
            val x = size.width * frac
            drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(0f, cy - t / 2), Size(size.width, t), CornerRadius(t / 2))
            drawRoundRect(Brand, Offset(0f, cy - t / 2), Size(x, t), CornerRadius(t / 2))
            drawCircle(Brand.copy(alpha = 0.28f), radius = tr.dp.toPx() * 1.8f, center = Offset(x, cy))
            drawCircle(Color.White, radius = tr.dp.toPx(), center = Offset(x, cy))
            drawCircle(Brand, radius = tr.dp.toPx() * 0.45f, center = Offset(x, cy))
        }
        Text(
            fmt(shown), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset((frac * w - 26.dp.toPx()).coerceIn(0f, (w - 52.dp.toPx()).coerceAtLeast(0f)).roundToInt(), -20.dp.roundToPx()) }
                .graphicsLayer { alpha = chipA }
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
