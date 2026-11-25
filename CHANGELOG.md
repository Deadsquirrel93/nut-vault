# Changelog

All notable changes to NutVault are documented here.

## 2.0.0 — 2026-11-25

### Design & UX
- Complete Material 3 redesign with full light, dark, and system themes plus optional Material You dynamic colors on Android 12+.
- New Squirrel Vault adaptive launcher icon (with Android 13+ themed monochrome layer).
- Compact home screen with color monograms, 1-tap copy for login and password, filter chips (All, Favorites, 2FA), and sorting (Recent, A–Z, Starred).
- Edge-to-edge UI targeting Android 17 (API 37).
- Redesigned credential detail screen with clickable URLs, syntax-highlighted monospace passwords, enlarged password viewer, and timestamps.

### Features
- **Encrypted notes & favorites** for credentials (non-destructive Room migration `1 → 2` preserving all existing entries and Android Keystore keys).
- **Offline 2FA (TOTP)** authenticator inside credentials with live 6-digit codes, 30s countdown ring, and `otpauth://` URI support.
- **Android Autofill Service** to fill saved logins and passwords in browsers and apps with biometric or master password unlock.
- **Password Health** audit screen highlighting weak and reused passwords with an overall vault score.
- **Standalone Password Generator** accessible from the home screen with instant preview and persisted preferences.
- **Configurable auto-lock timeout** (Immediately, 30s, 1m, 5m).
- **Backup import modes**: Merge with existing entries (skipping duplicates) or Replace entire vault. Fully backward-compatible with v1.0.0 `.vault` files.
- **Offline Updates** section in Settings showing the installed version and linking to GitHub Releases and Obtainium without any network permission.
- **System language** option alongside Russian and English, with Android 13+ per-app language support.

### Security & Bug Fixes
- Fixed crash on login when device biometric enrollment changes (`KeyPermanentlyInvalidatedException` now safely resets biometric unlock and falls back to master password).
- Marked copied passwords and 2FA codes as sensitive (`EXTRA_IS_SENSITIVE`) and moved clipboard auto-clear to the application scope using `clearPrimaryClip()`.
- Moved PBKDF2 hashing, Keystore crypto, and backup file I/O off the main thread.
- Added constant-time password hash verification (`MessageDigest.isEqual`) and live lockout countdown display on the login screen.
- Fixed folder rename input bug, generator empty character-set crash, and percentage-based corner radii.

### Build & Platform
- Upgraded to Gradle 9.8, AGP 9.4, Kotlin 2.4, KSP, Room 2.8 (with exported schemas), and Compose BOM `2026.09.00`.
- Enabled R8 minification and resource shrinking, reducing release APK size from ~60 MB to ~4.7 MB.
- Signed with the same certificate as `v1.0.0` for seamless in-place updates without data or key loss.

## 1.0.0 — 2026-03-22

First public release.
