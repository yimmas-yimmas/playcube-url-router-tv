# PlayCube URL Router (Kotlin / Google TV)

Routes incoming HTTP(S) `ACTION_VIEW` links:

- Paths ending in `.m3u8` (case-insensitive, including query strings) -> explicit Just+ Player activity `pl.waskysoft.player/com.brouken.player.PlayerActivity`, with `application/vnd.apple.mpegurl` MIME.
- Paths ending in `/master.txt` -> same Just+ Player activity.
- All other HTTP(S) URLs -> Via Browser (`mark.via.gp`).

**Important:** This app cannot make KDE Connect target it automatically. Google TV must first select the router as the handler for incoming HTTPS links. Android may prefer a browser, especially if a browser holds the BROWSER role. Android's chooser/default behavior is device-dependent. Setting this router as the default for generic HTTPS links may also route normal links through the router (which forwards them to Via). This app does not copy cookies or HTTP headers; protected HLS streams may still fail.

## Build (Android Studio recommended)

1. Install Android Studio and its Android SDK Platform 35 / Build Tools.
2. Open this folder as an Android Studio project and allow Gradle sync. Android Studio can install the compatible Gradle distribution; this source archive does **not** include a Gradle wrapper or precompiled APK.
3. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. APK: `app/build/outputs/apk/debug/app-debug.apk`.
5. Install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

CLI alternative if Gradle 8.7+ and Android SDK are configured: `gradle assembleDebug`.

## ADB tests

Test the router **explicitly** (works even before setting any defaults):

```sh
adb shell am start -n com.playcube.urlrouter/.RouterActivity -a android.intent.action.VIEW -d 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
```

Test a `/master.txt` link by replacing `YOUR_REAL_URL` (avoid sharing private signed URLs):

```sh
adb shell am start -n com.playcube.urlrouter/.RouterActivity -a android.intent.action.VIEW -d 'YOUR_REAL_URL'
```

Test normal websites (should launch Via):

```sh
adb shell am start -n com.playcube.urlrouter/.RouterActivity -a android.intent.action.VIEW -d 'https://example.org/'
```

Check if the router is registered:

```sh
adb shell cmd package query-activities --brief -a android.intent.action.VIEW -d 'https://example.org/'
```

Test the generic chooser:

```sh
adb shell am start -a android.intent.action.VIEW -d 'https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8'
```

If router appears, select **PlayCube URL Router > Always** where available. Then share a link via KDE Connect. If KDE Connect still launches the browser, its intent dispatch/default browser role is taking precedence. Do not clear browser data. A custom receiving service or alternative sharing mechanism may then be required.

If you previously removed Via's browser role and wish to restore it:

```sh
adb shell cmd role add-role-holder android.app.role.BROWSER mark.via.gp
```

## Limitations

- HLS recognition is path-based. A generic `.txt` or redirect to HLS without these patterns will go to Via.
- The stream must be playable by Just+ Player independently.
- Android's default-app behavior differs between Google TV versions. No guarantee of automatic KDE Connect interception.
- Uses target SDK 28 solely for sideload/testing legacy implicit web-link resolution. This is not a Play Store-ready app; modern target SDK migration requires additional domain-link strategy.
- Source-only project; build requires Android SDK/Gradle.

## Build in GitHub Actions (no Android Studio required)

1. Create a GitHub repository (prefer **Private**).
2. Upload the *contents* of this project folder to the repository root, including `.github/workflows/build-apk.yml` (hidden `.github` folder).
3. Open **Actions → Build PlayCube URL Router APK → Run workflow** (or push to `main` to trigger automatically).
4. Wait for the workflow to finish. Under its run page, download the `playcube-url-router-apk` artifact and unzip it.
5. Install `app-debug.apk` using `adb install -r app-debug.apk`.

The workflow uses Java 17, Gradle 8.10.2, and the Android SDK on GitHub-hosted runners. It has not been executed or verified here. If the workflow fails, share the build log.
