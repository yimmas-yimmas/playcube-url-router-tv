# tvBOX — Receiver + Send to tvBOX

Two Android applications in one GitHub repository:

- `receiver_app/`: **tvBOX Receiver** (Google TV / TCL PlayCube).
- `phone-sender-app/`: **Send to tvBOX** (Android mobile companion).

## tvBOX Receiver changes (v1.4.1)

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

## Remote / keyboard focus (this update)

- Both the **tvBOX Receiver** and **Send to tvBOX** interfaces highlight the focused
  action with a bright cyan 3dp border and vivid blue background; focus also
  enlarges the selected row slightly.
- DPAD up/down follows the vertical action list, even when the screen scrolls.
- Press OK/Enter to activate the highlighted row.
- The receiver Wi-Fi switch is controlled by its entire focusable row, avoiding
  an invisible child-switch focus target.
- The right-hand **Copy** controls on TV also show focus when selected.
- No existing pairing token, package ID, or share protocol was changed.

### Focus check after installation

1. On tvBOX Receiver, **Wi-Fi Receiver** is initially highlighted with a cyan outline.
2. Press **Down** repeatedly. The highlight moves to Open in, Access Token, Refresh QR/IP,
   Open Last Received Link, then Advanced Settings. **OK** still selects the item.
3. On Send to tvBOX (with hardware keyboard/remote), arrow through the pairing button,
   connection test (when paired), and quick action cards.
4. If the highlight fails to change on a specific popup dialog, capture the dialog title.

## Permanent Android APK signing (2026-10-27)

Both `receiver_app` and `phone-sender-app` use the **same stable private PKCS#12 signing key** in GitHub Actions. The key is **not committed** to the repository. The GitHub workflow requires these two GitHub Actions **repository secrets**:

1. `TVBOX_KEYSTORE_BASE64` — paste the **entire single line** from the private `keystore-base64.txt` file supplied separately.
2. `TVBOX_SIGNING_PASSWORD` — paste the password from the private `signing-password.txt` file supplied separately.

In GitHub, navigate to **Settings → Secrets and variables → Actions → New repository secret**. Create both secrets; then open **Actions → Build TV and Samsung APKs → Run workflow**.

Do not upload the private signing ZIP, `.p12` or credentials to GitHub or an issue. Save a private offline backup of the key and password. If the key is lost, you cannot update APKs already signed with it without uninstalling them first.

Artifacts keep their existing names (`tvbox-receiver-apk`, `send-to-playcube-samsung-apk`) and APK paths. Both Debug variants are signed with the stable certificate when built on Actions. The workflow checks that both signatures match.

**One-time migration:** The installed v1.4.2 tvBOX Receiver APK and previous phone APKs were signed with transient debug keys. Switching to the new signing key **requires uninstalling each affected app once**. This may erase pairing tokens and settings. After the new stably signed version is installed, subsequent `adb install -r` upgrades built from the same GitHub secrets will work, without another uninstall, provided you retain the same application ID and key.

Android debug builds run locally outside Actions continue to use the normal local debug keystore unless `TVBOX_KEYSTORE_FILE` and `TVBOX_SIGNING_PASSWORD` are set; do not distribute those locally signed builds as upgrades to the GitHub builds.
