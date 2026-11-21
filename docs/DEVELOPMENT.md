# Distortion developer guide

## Set up the checkout

Use JDK 21 and the Android SDK with Platform 37 and Build Tools 36.0.0. Android Studio can manage the SDK. Point Gradle to it with `ANDROID_HOME` or `sdk.dir` in `base/local.properties`. Local SDK paths should stay out of commits.

Build from the Gradle root at `base/`.

```sh
cd base
./gradlew :app:assembleDebug
```

On Windows in PowerShell, use `.\gradlew.bat` in place of `./gradlew`. APKs are written to `base/app/build/outputs/apk/debug/`. For a recent ARM64 device connected through ADB, run this from `base/`.

```sh
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

The default debug application ID is `app.distortion.music.beta.dev`. The public application ID is `app.distortion.music`. Android stores each package's accounts, downloaded files, and extensions separately.

## Architecture

| Module | Responsibility |
| --- | --- |
| [`:app`](../base/app) | Compose screens, playback routing, local Media3 playback, extensions integration, radio, and updates. |
| [`:api`](../base/api) | Shared Kotlin contracts for playback, catalog, library, queue, and related features. |
| [`:api-spotify`](../base/api-spotify/README.md) | Spotify API client and bridge running inside the Web Player. |
| [`:host-android`](../base/host-android) | Android WebView host, Spotify session lifecycle, media service, and diagnostics. |
| [`:extensions`](../base/extensions) and [`:extensions-cli`](../base/extensions-cli) | Extension runtime, package rules, and desktop tools for extension authors. |
| [`:automix`](../base/automix) | Beat analysis and transition planning for audio Distortion plays directly. |
| [`:host-cdp`](../base/host-cdp) | Desktop Chrome host and opt-in live integration tests. |

The app's playback and library interfaces are in [`data/engine`](../base/app/src/main/java/com/music/spotui/data/engine). Spotify audio runs inside the Web Player. Extension streams, downloaded tracks, and device audio use the app's local player. The routing layer selects a source and exposes playback state to the UI.

The Spotify client keeps a table of operation hashes. It can refresh the table from the public Web Player bundles after a server explicitly rejects a read's hash. Recovery is bounded, avoids downloading bundles on metered connections, and retries the read only when the hash changes. Web Player changes to login, licensing, or the service bridge may still require an app update.

## Verify a change

Run the same unit-test tasks used by [Android CI](../.github/workflows/android.yml) from `base/`.

```sh
./gradlew :api:test :automix:test :app:testDebugUnitTest :host-android:testDebugUnitTest :extensions:test :api-spotify:test :host-cdp:test
```

For a Spotify query or bridge change, run the focused GraphQL suite as needed.

Build both variants before a feature is merged. The optimized test build uses the development package and needs no production signing secret. It checks R8 and resource shrinking; it is not a stable release.

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease -PoverDev -Pabis=arm64-v8a
```

Follow the [feature branch and review policy](../CONTRIBUTING.md#build-and-submit-a-change). Test the optimized APK on a device for changed user flows, not just the debug APK. Signed updates and data migration also need a release-candidate check before publication.

```sh
./gradlew :api-spotify:test --tests 'app.myspoti.spotify.gql.*'
```

Host CDP tests that need Spotify skip unless configured. To run them, start desktop Chrome with a separate profile and the DevTools port bound to localhost. Sign into the Web Player with a test account in that profile.

```sh
google-chrome --user-data-dir=./spotify-test-profile --remote-debugging-port=9222
./gradlew :host-cdp:test -Pcdp=http://localhost:9222
```

For UI changes, record the device and Android version, the screen tested, and whether you verified light and dark mode. For playback changes, name the source and check startup, seek, next track, and background playback as applicable. Keep account tokens, cookies, and private diagnostic data out of issue reports and commits.

## Source references

- [Spotify client and bridge](../base/api-spotify/README.md)
- [Spotify capabilities and known limits](api/CAPABILITIES.md)
- [Extension API](extensions/API.md) and [source pipelines](extensions/SOURCE-PIPELINES.md)
- [AutoMix internals](automix/README.md)
- [Command-line tools](cli.md)
- [Release process](RELEASING.md)
