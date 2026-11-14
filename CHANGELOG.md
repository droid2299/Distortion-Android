# Distortion changelog

## 1.0.1

- Added a Genius switch in Lyrics settings to control song meanings, annotations, and fallback lyrics.
- Improved AutoMix's settling animation and stopped its visual updates when Distortion goes into the background.
- Kept the full player's controls in place in portrait, including when the volume slider is hidden.
- Dropping a track above the queue moves it to the front.
- Canvas releases its video decoder in the background and prefers a lower resolution in Battery Saver.
- Fixed an Android title-matching error that could prevent music video lookup.
- Reduced background Spotify sign-in checks. On an unmetered connection, Distortion can refresh an outdated Spotify query hash and retry the read once.

## 1.0.0

- Material You colors follow the Android system palette in the app and home-screen widget.
- Home organizes Your Mix, Continue Listening, and Recently Played. The bottom bar gives Home, New, Radio, Library, and Search their own tabs.
- Now Playing includes a wavy seek bar, synchronized lyrics, queue controls, and a Bluetooth output indicator.
- Swipe left or right on the artwork to change tracks. Swipe up on the mini player to expand it, or swipe down on the full player to collapse it with a continuous artwork and controls transition.
- Compatible extensions can supply additional playback and lyrics sources. The app shows the active playback source, reported audio quality, and extension installation errors.
- Fixed a Spotify bridge startup race and an issue that could leave an older radio favorites list on disk.

If you have Distortion 2.4.1, download the v1.0.0 APK and install it over your existing app. The public package and signing key are the same. The older app's update check compares version names, so it will not offer the lower-numbered release automatically.
