# NutVault

NutVault is a local-first Android password manager for a single user. It stores folders, credentials, notes, and 2FA TOTP secrets encrypted on the device, works completely offline without any network permission, and uses a clean Material 3 Jetpack Compose interface.

NutVault is open source under the `MIT` license. No ads, no tracking, no analytics, no cloud sync, no monetization.

Official source & releases: [github.com/Deadsquirrel93/nut-vault](https://github.com/Deadsquirrel93/nut-vault)

## Main Features

- **Encrypted local vault** — titles, URLs, logins, passwords, notes, and 2FA secrets encrypted via Android Keystore (AES-256-GCM)
- **Master password + Biometric unlock** — salted PBKDF2-HMAC-SHA256 (210,000 iterations), rate-limited lockout, safe biometric key invalidation handling
- **Configurable auto-lock** — Immediately, 30s, 1m, or 5m after leaving the app
- **Offline 2FA (TOTP)** — supports Base32 secrets and `otpauth://` URIs with live countdown and 1-tap copy
- **Android Autofill Service** — fill saved credentials in browsers and apps with biometric/master unlock
- **Password Health** — detects weak and reused passwords with an overall vault score
- **Password Generator** — instant preview, configurable rules, persisted preferences, standalone access from Home
- **Favorites, folders, search & sorting** — 1-tap copy login/password directly from the list
- **Protected clipboard** — marks sensitive values (`EXTRA_IS_SENSITIVE`) and auto-clears after 30 seconds
- **Encrypted `.vault` backup** — AES-256-GCM + PBKDF2 export/import with Merge and Replace modes (backward-compatible with v1.0.0 backups)
- **Light, dark & system themes** — plus optional Material You dynamic colors on Android 12+
- **Offline updates** — links to GitHub Releases and Obtainium without requesting `INTERNET` permission

## Tech Stack

- Kotlin 2.4, Jetpack Compose, Material 3 (Edge-to-Edge)
- Room 2.8 (KSP, exported schemas, non-destructive migrations)
- Android Keystore, BiometricPrompt, Android AutofillService
- R8 minification & resource shrinking

## Build & Test

```bash
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

Release APK output: `app/build/outputs/apk/release/app-release.apk`

## License

`MIT` — see [LICENSE](LICENSE).
