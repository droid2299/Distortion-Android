# Distortion

Distortion is an Android music player based on [Myspoti](https://github.com/kota-zo/myspoti). It uses Spotify for catalog and playback and supports compatible extensions for additional audio sources.

- Material You colors follow Android's system palette.
- The full player has a wavy seek bar, synchronized lyrics, and a queue.
- Home brings together Your Mix, Continue Listening, and Recently Played.
- Extensions show playback source, reported quality, and reachability when available.

## Build from source

The Gradle project is in `base/`. Use JDK 21 and the Android SDK with compile SDK 37.

```sh
cd base
./gradlew :app:assembleDebug
```

On Windows, run `gradlew.bat :app:assembleDebug` from `base`. The debug APK is written under `base/app/build/outputs/apk/debug/`.

```sh
./gradlew :app:testDebugUnitTest :extensions:test
```

See the [API documentation](docs/api), [extension documentation](docs/extensions), and [third-party notices](THIRD_PARTY_NOTICES.md).

## Credits

### BitChord

Myspoti owes a lot to [BitChord](https://github.com/kushagrasinghx/BitChord) by kushagrasinghx and its contributors (GPL-3.0-or-later), in code and in ideas:
- **The Immersive look** is ported from it: the player and its mesh backdrop, the swept word-by-word lyrics, the queue panel, the glass tab bar, the top bars, the sheets, the motion and the Home, Search, Library, artist, album, playlist, profile and podcast pages.
- **The equalizer** of the songs the app plays itself: its filters and the way a change glides.
- **Ideas** that shaped the app, DJ-style transitions between songs first of all: Myspoti's AutoMix is its own code, but the idea and the way the mix shows in the player come from BitChord's Automix.

Every file taken from BitChord keeps its GPL-3.0-or-later header and names the BitChord file it comes from; the full list is in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md#bitchord--gpl-30-or-later). Thank you, BitChord.

### Others

- UI derived from [Spotui](https://github.com/Spotui/Spotui) / [Metrolist](https://github.com/MetrolistGroup/Metrolist) (GPL-3.0); Spotui also gave the DASH-to-FLAC remux.
- [NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor) (GPL-3.0): music videos.
- [Reorderable](https://github.com/Calvin-LL/Reorderable) (Apache-2.0): dragging the rows of the queue.
- Lyrics: [LRCLIB](https://lrclib.net), LyricsPlus, lrc.red, Kugou and NetEase; song meanings: [Genius](https://genius.com).
- [Material Icons](https://github.com/google/material-design-icons) (Apache 2.0) and the [Manrope](https://github.com/googlefonts/manrope) font (SIL OFL 1.1, see `base/licenses/`).

Full notices: [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).

## License

Distortion and its upstream Myspoti code are distributed under the [GNU General Public License v3.0](LICENSE). Copyright and license notices for upstream and third-party code are included with the source.
