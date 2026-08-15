# MagicTap Android app

Kotlin · Jetpack Compose · Glance widgets. The phone side of [MagicTap](../../README.md):
manage profiles/PCs, send the signed wake, and offer two home-screen widgets.

## Requirements

- Android Studio (Ladybug or newer) with **JDK 17+**
- Android SDK Platform **36**
- A device/emulator on **Android 12 (API 31)** or newer

## Build & run

Open `srcs/aos-app` in Android Studio and Run, or from the command line:

```bash
./gradlew :app:installDebug
```

> **First checkout:** `gradle/wrapper/gradle-wrapper.jar` is intentionally not committed.
> Opening the project in Android Studio generates it automatically. For CLI-only setups,
> generate it once with a system Gradle:
>
> ```bash
> gradle wrapper --gradle-version 8.11.1
> ```

Key config (in [`app/build.gradle.kts`](app/build.gradle.kts)): `applicationId = "com.magictap"`,
`minSdk 31`, `targetSdk 36`. `INTERNET` is the only permission; backups are disabled.

## First-time use

1. **Add a profile** (your home / one Pico W): alias, internal LAN address, external DDNS
   address + port. A 32-byte HMAC secret is generated for you — **copy it into the Pico W's
   `config.json`**.
2. **Connection test** in the profile editor pings the Pico W over both the internal and
   external paths and reports each — the fastest way to catch a setup mistake.
3. **Add PCs** by MAC address (any separator; it's normalized to `AA:BB:CC:DD:EE:FF`).
4. **Tap 켜기** in the app, or add a widget.

## Widgets

- **List widget** — pick a profile at placement; shows its PCs, each row a wake button.
- **Single-icon widget** — pick profile → PC; one tap wakes that PC.

A tap opens a small translucent confirm dialog (`ConfirmActivity`) — not the full app — so
an accidental touch can't fire a wake. This works without launching the app because a
widget tap is a user interaction, exempt from Android 12+ background-start limits (§7.3).

## Architecture

Single JSON document, encrypted at rest with an Android Keystore AES-256-GCM key (the
deprecated `security-crypto` library is deliberately avoided, per the design doc §8). No
database, no network calls except to your own Pico W.

```
com.magictap
├─ MagicTapApplication / AppContainer   manual DI (repository, store, client)
├─ MainActivity                         Compose host + navigation
├─ data
│  ├─ model            Profile / Pc / AppData (kotlinx.serialization)
│  ├─ crypto           KeystoreManager (at-rest), BackupCrypto (PBKDF2 export/import)
│  ├─ store            SecureStore (encrypted file)
│  └─ WolRepository    StateFlow single source of truth
├─ net
│  ├─ HmacSigner       HMAC-SHA256(secret, "mac|ts")
│  ├─ MacUtils         MAC normalization/validation
│  └─ WolClient        OkHttp; internal→external fallback (§5.3)
├─ ui
│  ├─ main / profile / pc / settings    screens + view models
│  └─ components / theme
└─ widget
   ├─ ListWidget / SingleWidget         Glance widgets + receivers
   ├─ *ConfigActivity                   placement configuration
   └─ ConfirmActivity                   translucent confirm + send
```

### Internal/external switch (`WolClient`)

Ping the internal address first (300 ms). If it answers, wake internally; otherwise fall
back to the external DDNS address (5 s). Internal-first matters because home Wi-Fi without
NAT hairpin would make the external address fail. A fresh timestamp+signature per attempt
means the fallback never trips the Pico W's replay filter.

## Export / import

Settings → export writes a single JSON file. Default is **encrypted** (PBKDF2-HMAC-SHA256,
200k iterations → AES-256-GCM); a **secret-excluded** mode backs up structure only. Import
detects encryption, prompts for the passphrase, and offers merge or overwrite.

## License

[MIT](../../LICENSE) — part of the [MagicTap](../../README.md) project.
