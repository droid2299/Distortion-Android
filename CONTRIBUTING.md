# Contributing to Distortion

Bug reports, fixes, and translations are welcome.

## Before opening an issue

Check the [user guide](docs/USER-GUIDE.md) and [open issues](https://github.com/droid2299/Distortion-Android/issues) for the same problem. Include your Distortion version, Android version, device model, and the shortest steps that reproduce it. For playback issues, identify the source and describe whether the issue also occurs when playing through Spotify directly. Review any diagnostic export before sharing it. [Reporting a problem](docs/USER-GUIDE.md#report-a-problem)

## Build and submit a change

Every feature and fix has its own short-lived branch, such as `codex/account-free-onboarding` or `feature/adaptive-set-energy`. Start from the latest `main`. Do not push directly to `main`; open a pull request using the repository template. Keep unrelated features in separate pull requests.

Before merging, update the branch from `main` and let CI test the resulting merge commit. Require the `build` check, resolve review conversations, and record device validation. CI checks unit tests (including AutoMix) and builds both debug and optimized APKs. Live Spotify tests skip without a configured browser, so passing CI does not establish live-service compatibility. Test relevant playback sources on a device before merging. Review and merge with squash; publish releases only from tested commits on `main`.

The repository owner is currently the only required maintainer. Pull requests are mandatory, but GitHub does not require a second approving account: authors cannot approve their own PR. Review the diff and device evidence before merging; add a required independent approval when a second maintainer is available.

After merging, confirm the `main` workflow passes. If a regression occurs, revert the feature with a new PR and the same checks. Prefer additive, backward-compatible storage changes so reverting does not destroy user data. Automated checks reduce regressions; they cannot guarantee bug-free behavior.

1. Follow the [developer guide](docs/DEVELOPMENT.md#set-up-the-checkout) to install the toolchain and build a debug APK.
2. Keep the pull request focused. Write a short commit subject describing what changes for the user. Add or update a unit test for a bug fix or rule change.
3. Run the [checks for your change](docs/DEVELOPMENT.md#verify-a-change). In the pull request, explain the behavior, list the checks that ran, and note any device or live-service scenarios you could not verify.
4. For user-facing changes, update the relevant guide or settings copy. Keep default strings in `res/values/` and translations in the matching `res/values-*/` directory.

## Compatibility and licensing

- **Extensions.** Preserve existing extension contracts. Additions should leave current packages usable. [Extension API](docs/extensions/API.md)
- **Spotify.** Preserve Spotify's DRM, licensing, attestation, and rate limits. Contributions must respect these protections and must not add Spotify audio downloading or decryption.
- **License.** Contributions are distributed under [GPL-3.0](LICENSE.md). Preserve copyright, license notices, and upstream attribution when reusing the code. [Third-party notices](THIRD_PARTY_NOTICES.md)

Maintainers can follow the [release guide](docs/RELEASING.md) for signing, verification, and publication.
