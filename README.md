# tvBOX URL Router — TV + Samsung

Single GitHub repository containing two Android application modules.

- `app`: TCL tvBOX / Google TV URL receiver and router (unchanged from prior working combined project).
- `samsung-app`: Samsung phone companion, modern dark UI and one-scan QR pairing.

## Samsung features

- Branded dark home screen, pairing status, quick actions, clipboard sending and last 10 successfully sent links.
- Scan existing tvBOX TV QR once; stores the LAN IP and receiver token locally.
- **Share → Send to tvBOX**: accepts Android `ACTION_SEND` `text/plain`, extracts the first HTTP(S) URL, sends it immediately if paired, and returns to the originating app after a successful HTTP 200 response. No confirmation screen required.
- If unpaired, the scanner opens first and sends the pending shared link after pairing.
- On failure, leaves the app open with a retry option.
- Pairing settings let you rescan or forget the device.

## Build

GitHub Actions → **Build TV and Samsung APKs** → Run workflow.
Install `samsung-app-debug.apk` on the phone; the TV APK is not changed by this update.

## Usage

1. Open tvBOX Router on TV and start Wi-Fi receiver.
2. Open Send to tvBOX on Samsung, scan TV's QR code.
3. In Chrome or another Android app, tap Share → Send to tvBOX. The URL is sent automatically.

Both devices must be on the same trusted local network. The pairing token is transmitted over local HTTP, not HTTPS; do not use on untrusted networks. The TV app may still need to be foregrounded to launch playback, depending on Android TV background-launch restrictions.

## Build status

Source assembled and ZIP checked, but Android Gradle compilation and device testing have not been performed here.
