# Distortion changelog

## Unreleased

- Artist pages now include Spotify's **Discovered On** playlist selection, with separate retry controls and a related-artists fallback when the overview has none.
- Browse songs, albums, artists and public playlists in guest mode using an isolated anonymous session, with metadata pages and paged track lists.

- Added **Continue without an account** for local music and live radio, with the existing player and a **Connect Spotify** action. The guest choice survives restarts and does not replace an existing Spotify sign-in. Spotify-only library and catalog actions remain unavailable until sign-in succeeds.

## 1.0.3

- Adaptive Set builds a queue around a 15- to 60-minute target and an optional closing track. It adjusts upcoming tracks after skips or connection changes and shows a live estimate in the player queue. It uses downloads and device audio, with optional streaming from compatible audio extensions.
- Build a Mix combines recommendations from up to four artists or songs. Choose a familiarity level, preview the results, or save them as a Spotify playlist.
- Offline Favorites brings downloads and device audio together. Optional automatic downloads can save recent favorites through a compatible extension within a storage budget.
- Monthly Recap shows listening time and top tracks recorded on your device. Share a recap image or save eligible tracks as a private playlist.
- Standardized spacing across Material You screens and aligned the full player's transport controls and volume slider.

## 1.0.2

- Redesigned Home with a Quick Access grid, stacked Recently Played cards, and a mix of large artwork cards and compact recommendations.
- Brought the same Material You colors and shapes to New, Radio, Search, Library, settings, detail pages, and player surfaces.
- Low-memory devices now reduce startup preloading, motion, and artwork effects automatically. This mode also disables Canvas and limits music video resolution.
- Fixed Home retry handling so repeated Spotify errors no longer stop automatic recovery early.
- Kept search results separate across account sessions and cleared results immediately when the search field is emptied.
- Prevented a late radio response from replacing a queue that had already changed.
- Reduced background requests, duplicate disk writes, and layout work on Home.

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
