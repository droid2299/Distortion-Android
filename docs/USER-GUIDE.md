# Distortion user guide

## Artist discovery

Open an artist and scroll to **Discovered On** to explore Spotify's current discovery playlist selection. Tap a playlist to open it. Selections can change or contain unavailable playlists; Distortion shows unavailable entries and offers Retry when a request fails. Related artists use the overview results when available and load separately otherwise. A discovery failure does not stop the rest of the artist page or playback.

## Install and update

1. Download the APK for your device from [GitHub Releases](https://github.com/droid2299/Distortion-Android/releases/latest). Most recent phones use **ARMv8**. The **universal** APK supports both ARM architectures. Choose **TV** for Android TV or Google TV.
2. Open the downloaded APK. If Android opens **Install unknown apps**, allow the browser or file manager you used, then return to the installer.
3. Open Distortion. Sign in with your Spotify account, or choose **Continue without an account** to play local files and live radio. From the account-free Home, choose **Connect Spotify** when you want your Spotify library. Back from that sign-in returns to account-free listening.

Account-free mode remembers your choice across restarts. Local audio requires the audio permission and files stored on your device; radio requires a connection. Search browses songs, albums, artists and public playlists using a separate anonymous browser profile and a recent Android System WebView. Open a song and choose **Play with an audio extension**, or tap a track in an album, artist or playlist. Install and enable a compatible audio extension through **Library → Extensions**. If extensions cannot provide a track, a message offers Spotify sign-in; playback availability depends on the extension and its provider. Spotify library actions and personalized recommendations require connecting Spotify. Existing signed-in installations continue to open their normal Home.

When a guest playback attempt fails, the screen offers retry, extension management and Spotify sign-in. It can identify a missing enabled source or an unavailable network; other failures are reported without assuming which provider or match caused them.

After you start playing songs without an account, Home builds **Your Mix**, recent plays and artist mixes from up to 50 tracks played on this device. These are local listening-history mixes, not Spotify recommendations. Create a device playlist in **Library**, or open a song in Search and choose **Add to playlist**. You can also add local files. Device playlists and guest history stay in the app's storage and do not sync to Spotify or other devices; uninstalling the app or clearing its data removes them. Extension tracks need a compatible enabled source when played again.

In guest **Library**, use the plus button to create a playlist and the search button to find a device playlist. **Recently Added** shows your local playlists. In **Search**, choose Songs, Albums, Artists or Playlists and enter a name to browse public catalogue results.

Guest searches are kept on this device, separate from Spotify search history. Remove individual terms or use **Clear** on the Search screen. The microphone opens your device's speech-recognition app, if one is available; that app processes the spoken query under its own privacy settings.

For Spotify sign-in, use email and enter the code Spotify sends you. Google sign-in can be rejected inside the embedded browser; signing into Chrome does not sign Distortion in.

For an existing public installation, open **Settings → App** (or **About** in Classic) and tap the refresh icon next to the version or open the new APK directly. Keep the app installed while applying the update to retain its settings and stored data. If you are moving from the earlier Distortion **2.4.1** build to the **1.x** series, download the APK manually once. The older updater sees the 1.x version name as earlier.

If Android reports a signature conflict, check whether the installed app came from a different source. A debug or locally signed build may have a different package name or signing key. Removing an app also removes its local data, so confirm which copy you are replacing before uninstalling.

## Playback sources

| Source | How it plays | What to check |
| --- | --- | --- |
| Spotify | The embedded Spotify Web Player plays the track. | Spotify sign-in, connection, track availability, and the active Connect device. |
| Audio extension | Distortion plays audio supplied by the extension. | Whether the extension is enabled, its position in **Settings → Sources**, and whether the provider found a match. |
| Downloaded or device audio | Distortion plays audio stored on the device. A download requires a download-source extension. | Whether the download completed or the device file is still present. |

The quality offered by an extension depends on its provider and the recording it found. Open the player menu for **Sound quality** or **Stats for nerds**. For a source attempt, **Why this source?** can explain which provider was used. AutoMix and the equalizer apply to supported audio played directly by Distortion.

## Mixes, offline favorites, and recap

- **Build a Mix** on Home combines up to four artists or songs. Choose Familiar, Balanced, or Discover to change the balance of selected and recently heard music against recommendations. Tap a result to play it, or name and save the mix as a Spotify playlist.
- **Adaptive Set** in Build a Mix fits music to a target duration. Choose a closing song and optionally order tracks using available energy analysis. Switch on **Offline only** for downloads and device audio. If you have an enabled audio-source extension, a generated mix can also supply streaming tracks. Open the player queue for the live estimate or choose **Keep current queue** to stop automatic changes.
- **Offline Favorites** on Home plays an ordered selection of downloaded songs and audio from your device. Automatic downloads are off by default. Choose a storage budget to save recent favorites through a compatible download extension on an unmetered connection. Automatic copies use the space-saving quality setting. Use **Downloaded** to remove copies you no longer want.
- **Monthly Recap** shows listening time, active days, and top tracks from this device. Browse earlier months, share an image through Android's share sheet, or save eligible top tracks as a private Spotify playlist. A newly installed copy needs listening history before it can show a recap.

## Troubleshooting

On Android devices marked as low RAM or reporting less than 3 GB of usable RAM, Distortion automatically uses lighter visuals. It reduces motion and background loading, disables Canvas, and limits music video resolution. You can enable the same behavior on other phones with **Settings → Data and battery → Battery saver**.

| Symptom | What to check |
| --- | --- |
| Spotify sign-in loops or playback stalls | Try the same account and track in Spotify to isolate the issue. In Distortion, use **Restart the player** in Settings and retry. |
| An extension falls back to Spotify | Check that it is enabled and above Spotify in **Settings → Sources**. Look at **Why this source?** for a match, provider, or connectivity error. |
| Extension installation stops at the confirmation sheet | Review the permissions and check the third-party acknowledgment if shown. Tap **Install** and keep the error text if the package is rejected. |
| Lyrics are missing | Try another source from the lyrics screen, when available. Under **Settings → Lyrics**, the **Genius** switch controls Genius annotations, meanings, and fallback lyrics. It is separate from synchronized lyric sources. |
| A phone update is unavailable | Check that the build is a public Distortion release, or download the matching APK directly from GitHub. |

## Report a problem

1. Record the Distortion version from Settings, Android version, device model, and approximate time of the problem.
2. Describe the steps, expected result, and actual result. For playback reports, say whether Spotify, an extension, or local audio was active. For extension reports, include the extension name and version.
3. If available, use **Export a diagnostic report** in Settings after reproducing the issue. Read the export before sharing it and remove any information you prefer to keep private.
4. File app bugs in [Distortion’s issue tracker](https://github.com/droid2299/Distortion-Android/issues/new). For provider-specific issues, use the extension author’s support channel.

## Accounts and network services

Spotify sign-in runs in the embedded Web Player. Distortion also requests Spotify catalog and account data for features such as search and library. Other features use their own providers, such as Radio Browser for station discovery and enabled sources for lyrics. An installed extension can make the network and account requests described in its granted actions. [Extension permissions](extensions/GUIDE.md#review-permissions-and-updates)
