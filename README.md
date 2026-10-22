# tvBOX — Receiver + Send to tvBOX

Two Android applications in one GitHub repository:

- `receiver_app/`: **tvBOX Receiver** (Google TV / TCL PlayCube).
- `phone-sender-app/`: **Send to tvBOX** (Android mobile companion).

## tvBOX Receiver changes (v1.4.0)

- **Wi-Fi Receiver is enabled by default.** The first launch starts a foreground receiver service on port `8765`.
- If enabled, the receiver attempts to restart after `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` (TV reboot and app update). Service uses `START_STICKY` if Android terminates it.
- The TV interface has an ON/OFF switch. Choosing OFF is saved and survives reboots; turning ON again restores automatic start.
- QR code enlarged from **120 dp to 180 dp**, while encoding the same local IP and access token.
- Subtitle beneath Wi-Fi Receiver now reads **“Receive URLs from Mobile Phone.”**
- Original Java/Kotlin package and `applicationId` remain `com.playcube.urlrouter`, so existing pairing and settings can survive an update **if the signing certificate is unchanged**. Renaming the Gradle module does not rename the Android package.
- Existing player/browser routing, stored-link fallback, and other TV settings are retained.

## Build with GitHub Actions

Upload the **contents** of this ZIP to the existing repo root. **Delete the old top-level `app/` folder** from GitHub; it has been renamed to `receiver_app/`. Do not leave both directories.

Actions → **Build TV and Samsung APKs** → Run workflow.

Artifacts:

- `tvbox-receiver-apk` → `receiver_app-debug.apk` for TV.
- `send-to-playcube-samsung-apk` → `phone-sender-app-debug.apk` for phone (app name is **Send to tvBOX**).

Android TV users: install the receiver APK (same application ID). Open it once after installing to initialize the receiver; thereafter it attempts to restart on reboot. The phone APK does not need reinstalling for this update.

## Share via Android

1. Make sure your phone and TV are on the same local Wi-Fi network.
2. Open **tvBOX Receiver** on TV. The Wi-Fi receiver toggle defaults ON.
3. Scan the QR in **Send to tvBOX** once. Subsequent Android **Share → Send to tvBOX** sends URLs directly with the saved token.
4. If Android TV blocks a background activity launch, open **tvBOX Receiver** from the TV home screen to launch the pending link.

## Limitations

Android TV may stop apps under severe power management, when force-stopped, or before an app is launched once after installation. Boot-time foreground-service starts are firmware-dependent. A running Wi-Fi server does **not** guarantee a player/browser will be allowed to appear from the background.

The pairing token travels over unencrypted HTTP on the local Wi-Fi; only use on a trusted network and keep the QR private.

This repository is source code and still needs GitHub Actions compilation and a test on PlayCube.
