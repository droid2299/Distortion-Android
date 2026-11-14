# Extensions for Distortion

Extensions are optional packages for playback, downloads, lyrics, and other features. Distortion runs extension scripts in a sandbox and grants the actions you approve at installation. The extension author operates or selects the services used by that extension.

| Extension type | What it provides | Where to use it |
| --- | --- | --- |
| Audio source | Playback from a provider selected by the extension. Supported quality depends on the provider and recording. | **Settings → Sources**, and **Sound quality** in the player |
| Download source | Files for offline listening from the extension's provider. Distortion stores downloaded audio encrypted on the device. | Track, album, and playlist download actions |
| Lyrics | Additional timed, word-by-word, or plain lyrics from other catalogs. | Lyrics screen and its source menu |
| Other | Home or search sections, player actions, settings, and tools. | The surfaces listed on the extension's page |

[Extension API for authors](API.md)

## Install an extension

1. Open **Settings → Extensions**.
2. Choose the installation method the author provides. Distortion can browse available entries in the **Store**, add a repository using the author's `index.json` URL, install a local ZIP, or install from an HTTPS address.
3. On the confirmation sheet, read the actions, reasons, and domains. Review any critical permissions and tick the third-party acknowledgment if it appears. Tap **Install**.
4. Open the extension's settings if it needs its own sign-in or configuration. Enable it if required.

The source list in **Settings → Sources** controls which playback extension Distortion asks first. Enable the source you want and move it above Spotify if it should get the first attempt. If no enabled source supplies a match, a Spotify track can play through Spotify.

## Review permissions and updates

Install packages from authors you trust. An extension may gain access to playback, network destinations, or your Spotify account depending on the actions it requests. The install sheet lists individual actions. Approval applies to the requested set.

If an update asks for new permissions, Distortion waits for your approval. You can disable an extension while keeping its settings. Uninstalling an extension removes its stored settings and data.

## Troubleshoot an extension

| Symptom | What to check |
| --- | --- |
| The Install button stays disabled | Scroll through the confirmation sheet and complete the third-party acknowledgment if shown. Check for a minimum-version or package error. |
| A track plays through Spotify | Check the source order and the extension's status. Open **Why this source?** to see the attempt when available. |
| A source reports no match or is unreachable | Check the extension author's service status and try another track. Available recordings, service access, and quality depend on the provider. |
| Sign-in fails | Reopen sign-in from the extension's page and check the provider's instructions about accounts or subscriptions. |
| A download finds no compatible recording | Try another quality offered by the extension or a different download source. |

Report provider-specific problems to the extension author with the extension version and error text. For issues in Distortion's installation or playback flow, see [reporting a problem](../USER-GUIDE.md#report-a-problem).

## Android Auto and TV

If Distortion is missing from Android Auto's app list, check Android Auto's **Unknown sources** developer option for sideloaded media apps. [Android Auto developer settings](https://developer.android.com/training/cars/testing#allow-unknown-sources) Distortion's TV APK opens the TV interface, and the TV and phone keep their own installations and stored data.
