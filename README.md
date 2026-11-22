# Cascade Video Player

Local-files video & audio player for Android (Kotlin, Jetpack Compose, Media3 ExoPlayer + FFmpeg decoders).

## Build the APK (GitHub Actions)
1. Create a new GitHub repo and upload everything in this folder (keep `.github/workflows/build.yml`).
2. Open **Actions → Build APK → Run workflow** (it also runs on every push).
3. Download the `CascadeVideoPlayer-apk` artifact → `app-debug.apk` → install.

The workflow installs Gradle itself, so no `gradlew` / wrapper file is needed.

## Features
- Home screen: search, sort, Videos / Audio tabs, poster grid, "Continue watching", light / dark / system theme
- Plays most formats (MP4, MKV, WebM, MOV, TS, FLV, MP3, AAC, FLAC, OGG, OPUS, WAV...) incl. AC3/DTS/Opus audio via FFmpeg
- Open from other apps ("Open with"), open any file, play network URLs
- Resume where you left off (saved per video)
- Double-tap left/right to skip (5/10/15/30 s), swipe to seek, left swipe = brightness, right swipe = volume
- Long-press = 2x speed, speed slider 0.25x–4x
- Screenshot button (saved to Pictures/Cascade)
- Play as audio (screen off, keeps playing with a notification)
- Notification / lock-screen controls (ongoing media notification = Realme Live Alert)
- Audio tracks, embedded + external subtitles (.srt .vtt .ass), fit / stretch / zoom, rotate, lock, PiP,
  next / previous, repeat modes, sleep timer
