# Contributing to Distortion

Bug reports, fixes, and translations are welcome.

## Before opening an issue

Check the [user guide](docs/USER-GUIDE.md) and [open issues](https://github.com/droid2299/Distortion-Android/issues) for the same problem. Include your Distortion version, Android version, device model, and the shortest steps that reproduce it. For playback issues, identify the source and describe whether the issue also occurs when playing through Spotify directly. Review any diagnostic export before sharing it. [Reporting a problem](docs/USER-GUIDE.md#report-a-problem)

## Build and submit a change

1. Follow the [developer guide](docs/DEVELOPMENT.md#set-up-the-checkout) to install the toolchain and build a debug APK.
2. Keep the pull request focused. Write a short commit subject describing what changes for the user. Add or update a unit test for a bug fix or rule change.
3. Run the [checks for your change](docs/DEVELOPMENT.md#verify-a-change). In the pull request, explain the behavior, list the checks that ran, and note any device or live-service scenarios you could not verify.
4. For user-facing changes, update the relevant guide or settings copy. Keep default strings in `res/values/` and translations in the matching `res/values-*/` directory.

## Compatibility and licensing

- **Extensions.** Preserve existing extension contracts. Additions should leave current packages usable. [Extension API](docs/extensions/API.md)
- **Spotify.** Preserve Spotify's DRM, licensing, attestation, and rate limits. Contributions must respect these protections and must not add Spotify audio downloading or decryption.
- **License.** Contributions are distributed under [GPL-3.0](LICENSE.md). Preserve copyright, license notices, and upstream attribution when reusing the code. [Third-party notices](THIRD_PARTY_NOTICES.md)

Maintainers can follow the [release guide](docs/RELEASING.md) for signing, verification, and publication.
