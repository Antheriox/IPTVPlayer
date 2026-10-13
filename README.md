# IPTV Player (Android)

A simple IPTV client in the style of TV Smarters — log in with an **Xtream Codes**
account (server + username + password) or a plain **M3U playlist URL**, then browse
Live TV / Movies / Series and play streams with ExoPlayer.

## What's included

- Login screen with two modes: Xtream Codes, or M3U URL
- Saved profiles (stored locally on-device, quick reconnect)
- Live TV, Movies, and Series tabs (Movies/Series only apply to Xtream logins —
  a plain M3U playlist is just one flat list of channels grouped by category)
- Category → channel/series list → episode list → player navigation
- Playback via Media3 ExoPlayer (handles HLS `.m3u8` and most IPTV stream formats)
- **Chromecast support** — tap the Cast icon (home screen or player) to send live
  channels, movies and series episodes to a Chromecast / Google TV. Once connected,
  every channel you open plays on the TV; movies/episodes keep their resume position.

## Chromecast notes

- Uses Google's *Default Media Receiver*, so no Cast developer registration is needed.
- The phone and the Chromecast must be on the same Wi-Fi network, and the phone needs
  Google Play Services (the Cast icon is hidden on devices without it).
- The Chromecast fetches the stream itself, straight from your IPTV server:
  - Xtream live channels are cast as HLS (`.m3u8`) instead of `.ts`, since Chromecast
    can't play raw MPEG-TS streams.
  - HLS streams only work on the Chromecast if the IPTV server sends CORS headers;
    some providers don't, and then casting live TV fails while phone playback works.
  - MP4 movies work everywhere; MKV only on newer devices (Chromecast with Google TV).

## What's NOT included (possible next steps)

- Electronic Program Guide (EPG / XMLTV)
- Favorites / search
- Parental controls, multi-screen, catch-up TV / DVR
- App icon / launcher graphics (uses the system default icon placeholder)

## How to build the APK

1. Install **Android Studio** (free): https://developer.android.com/studio
2. Open Android Studio → **Open** → select this `IPTVPlayer` folder
3. Let it sync (first sync will download Gradle + dependencies — needs internet).
   If it asks about the Gradle wrapper, let it generate/use one automatically.
4. Once synced: **Build → Build Bundle(s) / APK(s) → Build APK(s)**
5. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`
6. Copy that file to your Android phone (email, USB, cloud drive, etc.) and open it
   to install. You'll need to allow "install from unknown sources" the first time,
   since this isn't from the Play Store.

## Notes on the Xtream Codes login

Most IPTV subscription providers that advertise "Xtream Codes" or "Xtream UI"
compatibility give you three things: a **server URL** (e.g. `http://example.com:8080`),
a **username**, and a **password** — enter exactly those into the Xtream Codes tab.
The app calls the standard `player_api.php` endpoint that these panels expose.
