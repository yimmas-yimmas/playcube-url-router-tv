# PlayCube URL Router + Send to PlayCube

Two Android apps in **one GitHub repository**.

- `app/`: TCL PlayCube TV receiver/router, with 120 dp QR code and saved-link launch fallback.
- `samsung-app/`: Samsung phone companion with Google Code Scanner QR pairing and Android text/plain Share target.

## Build

Go to GitHub **Actions → Build TV and Samsung APKs → Run workflow**. Two APK artifacts are produced. Install the TV APK on PlayCube and Samsung APK on your phone. The GitHub Actions build requires internet access for Gradle dependencies.

## Pair and send

1. On TV, open PlayCube URL Router and start Wi-Fi receiver.
2. On phone, open Send to PlayCube, scan TV QR once (requires Google Play services for the code scanner).
3. From Chrome or another Android app, choose **Share → Send to PlayCube**.
4. Phone and TV must be on same trusted local network. TV's IP or token change requires rescanning.

**TV limitation:** Android TV can prevent background app launches. If the link does not open immediately, bring the TV Router to the foreground to open its saved URL.

**Security:** pairing token travels via local HTTP. Use only a trusted private LAN. QR is a reusable credential, not certificate-based KDE pairing.

**Build status:** source integrated; no local Android SDK/Gradle compilation was performed. Check GitHub Actions build output.
