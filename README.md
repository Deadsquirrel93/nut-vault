# NutVault

NutVault is a local-first Android password manager for a single user. It stores folders and credentials only on the device, works without a backend, does not use cloud sync, and is designed for everyday personal use with a clean Jetpack Compose interface.

## What NutVault Does

NutVault lets you:

- create, edit, move, and delete credentials
- group credentials into folders or keep them in the root vault
- copy title, URL, login, and password independently
- hide and reveal passwords on demand
- generate strong passwords with configurable rules
- evaluate password strength while typing or generating
- lock the app with a master login and master password
- unlock with biometrics when enabled
- export the vault into an encrypted `.vault` file
- import an encrypted vault file and restore folders and credentials

## Product Highlights

- Offline by design: no backend, no sync, no network dependency
- One-user secure flow: first launch requires registration of the local master account
- Local encryption: sensitive credential fields are stored encrypted on device
- Modern Android stack: Kotlin, Jetpack Compose, Room, Material 3
- Practical security controls: Android Keystore, BiometricPrompt, clipboard cleanup, app autolock, protected screens
- Minimal interface: fast access to folders, credentials, search, generator, and settings

## Security Model

NutVault is built around a local-only security model:

- master password is not stored in plain text
- master password is stored as a salted PBKDF2-HMAC-SHA256 hash
- credential secrets are stored encrypted with device-backed cryptography
- biometric unlock is used only as an unlock method, not as the primary source of truth
- exported vault files are encrypted and protected by a separate export passphrase
- the app does not send secrets over the network

## Main Features

### Credentials

Each credential can contain:

- title
- URL
- login
- password

Supported actions:

- create and edit
- move between folders
- delete with confirmation
- copy any field separately
- reveal or hide password without changing stored value

### Folders

- create folders
- rename folders
- delete folders with destructive confirmation
- open a folder and browse its credentials
- keep credentials inside folders or in the root vault

### Search

- search across all credentials from the home screen
- search inside a specific folder

### Password Generator

- choose length
- include or exclude uppercase, lowercase, digits, and symbols
- regenerate instantly
- apply generated password directly to the credential form
- see password strength immediately

### Authentication

- mandatory initial registration on first launch
- login with master login and master password
- optional biometric unlock
- immediate biometric offer after registration
- biometric prompt can open automatically on login when enabled

### Import and Export

- encrypted `.vault` export
- encrypted import with replacement confirmation
- handling for invalid or incompatible vault files

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Android Keystore
- BiometricPrompt
- MVVM-style state management with repositories and use cases

## Project Structure

- `app/src/main/java/com/example/passwordvault/app` — app container and root app state
- `app/src/main/java/com/example/passwordvault/core` — auth, crypto, localization, password tooling
- `app/src/main/java/com/example/passwordvault/data` — Room database, entities, DAOs, repository implementations
- `app/src/main/java/com/example/passwordvault/domain` — models, repository contracts, use cases
- `app/src/main/java/com/example/passwordvault/ui` — Compose screens, components, navigation, view models

## Build Requirements

- Android Studio with current Android SDK
- JDK 17
- Android SDK configured locally

## Build and Run

```bash
./gradlew assembleDebug
```

Debug APK output:

`app/build/outputs/apk/debug/app-debug.apk`

## Tests

```bash
./gradlew testDebugUnitTest
```

## Current Product Scope

NutVault is intentionally scoped for one person on one device at a time:

- no multi-user support
- no cloud backup
- no server API
- no password recovery flow
- no internet sync

That keeps the product small, private, and predictable.
