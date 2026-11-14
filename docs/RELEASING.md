# Releasing Distortion

## Prepare the release

1. Choose the next version and add its notes under a `## X.Y.Z` heading in [CHANGELOG.md](../CHANGELOG.md). This heading is the source for in-app and GitHub release notes.
2. Choose an Android version code greater than the previous public APK's. Compare the version names independently, since the in-app updater uses version names to decide whether an update is available.
3. Update the local version fallback in [the app Gradle file](../base/app/build.gradle.kts). Run the [test suite](DEVELOPMENT.md#verify-a-change) and push the release commit to `main`. Record the commit SHA that will receive the tag.

## Build and sign locally

Distortion's current releases use the local release key. Use the same stable signing key as the previous public APK to allow Android to update existing installs. Supply it through the signing environment variables below or the untracked `base/keystore/keystore.properties` configuration. Keep signing material out of Git.

| Environment variable | Value |
| --- | --- |
| `MYSPOTI_KEYSTORE` | Path to the release keystore |
| `MYSPOTI_KEYSTORE_PASSWORD` | Keystore password |
| `MYSPOTI_KEY_ALIAS` | Signing key alias |
| `MYSPOTI_KEY_PASSWORD` | Signing key password |

These variable names are part of the existing build configuration. Build from `base/` with values for the version name and code chosen above. On Windows, use the Gradle wrapper batch file and quote each `-P` argument.

```sh
./gradlew :app:assembleRelease :app:collectApks \
  "-PappVersionName=$release_version" \
  "-PappVersionCode=$release_code" \
  -PupdateRepo=droid2299/Distortion-Android
```

The four named installable APKs are in `base/app/build/release-apks/`. Build without `-Pabis` to produce the complete set. The TV APK uses the universal binary, which selects the TV interface at runtime.

## Verify and publish

1. Verify every APK with `apksigner verify --print-certs`. Compare the stable signing certificate digest to the previous published APK. Use `aapt dump badging` to check that every stable APK has the expected version name, version code, and package ID `app.distortion.music`.
2. On a test device, install the new stable APK over the previous stable APK. Check startup, account state, playback, and the update screen. Repeat on TV if the release changes the TV experience.
3. Publish the matching `vX.Y.Z` tag and GitHub release against the recorded source commit. Attach the ARMv8, ARMv7, universal, and TV APKs. Use the matching changelog section as the release description and mark a stable release as latest.
4. Visit the public release page and verify the tag, notes, and four downloads. For a stable release, confirm it appears at [the latest release URL](https://github.com/droid2299/Distortion-Android/releases/latest).

## GitHub Actions

Branch pushes, pull requests, and version tags run [Android CI](../.github/workflows/android.yml). A tag push runs verification. The signing and publication job runs when a maintainer explicitly dispatches the workflow on a version tag and the repository has the required signing secrets.

```sh
gh workflow run android.yml --ref vX.Y.Z
```

Use one publishing path per release. For a locally signed release, the tag-triggered run verifies the source while the uploaded APKs remain the locally signed artifacts.
