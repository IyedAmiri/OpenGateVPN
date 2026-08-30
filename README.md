# OpenGate VPN

A lightweight, open-source VPN client for Android, built around the free public
[VPN Gate](https://www.vpngate.net/en/) relay network. **6.7 MB** APK, no account,
no ads, no tracking — pick a gateway and connect.

## Features

- **Extremely lightweight** — a 6.7 MB release APK and a small runtime
  footprint: R8-minified, dead feature code stripped from the VPN engine,
  event-driven telemetry instead of polling, and no background work beyond the
  tunnel itself. Tuned for low CPU, RAM, and battery usage on modest hardware.
- **Smart Connect** — automatically ranks every node in the public VPN Gate
  directory (ping, throughput, uptime, active sessions, residential-host
  filtering) and connects you to the best one.
- **Full server explorer** — browse, search, and filter all available gateways
  by country or residential likelihood; sort by ping, speed, sessions, or a
  composite quality score.
- **Seamless server switching** — change gateways mid-session without leaks or
  confusing states.
- **Pause / Resume** — temporarily bypass the tunnel and restore it later.
- **Live telemetry** — real upload/download speeds and session totals straight
  from the tunnel's byte counters.
- **Diagnostics console** — a terminal-style log feed with one-tap copy.
- **Keep-alive recovery** — a persistent job service restores the tunnel if the
  process is killed in the background.
- **Race-free connection state machine** — unified retries, honest status badge,
  no phantom states.

## How it works

OpenGate fetches the public VPN Gate directory over HTTPS, parses the OpenVPN
profiles embedded in it, and hands the selected profile to a bundled OpenVPN
engine. All traffic runs through a standard Android `VpnService` tunnel using
OpenVPN (UDP/TCP, AES encryption). The app connects with placeholder
credentials — VPN Gate relays accept anonymous connections.

## Download

Grab the latest signed APK from
[**Releases**](../../releases) and install it directly
(enable *Install unknown apps* for your browser/file manager when prompted).

Requires Android 7.0+ (arm64-v8a or armeabi-v7a devices).

## Building from source

```bash
# Debug build (auto-signed, installable)
./gradlew assembleDebug

# Release build (R8-minified)
./gradlew assembleRelease
```

Output lands in `app/build/outputs/apk/<variant>/`. Debug builds are signed
automatically. Release builds expect a local keystore (see `signingConfigs` in
`app/build.gradle.kts`) and fall back to the debug key if none is configured.

## Project structure

Standard Android Gradle project with two modules, built with Kotlin, Jetpack
Compose (Material 3), and a strict MVVM separation between UI and data.

```
app/                                # The Android application (Kotlin + Compose)
└── src/main/java/com/payaq/opengatevpn/
    ├── MainActivity.kt             # Entry activity; VPN permission flow,
    │                               #   OpenVPN status listener wiring
    ├── data/
    │   ├── model/
    │   │   ├── VpnServer.kt        # Raw directory row incl. Base64 config
    │   │   └── VpnServerDisplay.kt # Lightweight model for Compose lists
    │   ├── remote/
    │   │   └── VpnGateApi.kt       # HTTPS fetch + CSV parse of vpngate.net,
    │   │                           #   strict filtering, deduplication
    │   ├── repository/
    │   │   └── VpnRepository.kt    # Single data entry point for the ViewModel
    │   └── vpn/
    │       └── VpnManager.kt       # Profile parsing, connect/disconnect,
    │                               #   pause/resume of the OpenVPN tunnel
    └── ui/
        ├── main/
        │   ├── MainScreen.kt       # Root scaffold, tabs, splash animation
        │   ├── ConnectionTab.kt    # Connect button, timer, telemetry, server card
        │   ├── ServersTab.kt       # Server explorer: search/filter/sort
        │   ├── LogsTab.kt          # Diagnostics console + security info
        │   └── VpnViewModel.kt     # Connection state machine, retries,
        │                           #   ranking, telemetry state
        ├── theme/                  # Colors, typography, dark terminal theme
        └── util/
            └── CountryFlagUtil.kt  # Country code → flag emoji

vpnLib/                             # OpenVPN engine module (attribution above)
└── src/main/
    ├── java/de/blinkt/openvpn/
    │   ├── core/                   # OpenVPNService (the VpnService tunnel),
    │   │                           #   management thread, profile handling,
    │   │                           #   status/log pipeline, keep-alive job
    │   ├── api/                    # Internal permission/profile glue
    │   └── ...                     # VpnProfile, LaunchVPN activity
    ├── jniLibs/                    # Prebuilt libopenvpn.so (arm64-v8a, armeabi-v7a)
    ├── assets/                     # PIE launcher stubs for the native binary
    └── aidl/                       # Internal service interfaces
```

`app` never talks to the native OpenVPN binary directly — it goes through
`VpnManager` → `vpnLib`'s `OpenVPNService`, with connection status flowing back
via `VpnStatus` listeners.

## Acknowledgements

- [**vpnLib**](https://github.com/hoang-rio/vpnLib) — the VPN engine module this
  project bundles (stripped of unused features: Tor/Orbot support, external-app
  auth, remote-control AIDL API, and the x86/x86_64 ABIs).
- [**OpenVPN for Android (ics-openvpn)**](https://github.com/schwabe/ics-openvpn)
  by Arne Schwabe — vpnLib is itself built on top of this project (v0.7.64).
- [**VPN Gate**](https://www.vpngate.net/en/) — the public volunteer relay
  network and directory API this client consumes.

## License

This project is released under the **GNU GPL v3** (see
[LICENSE](LICENSE)). It includes vpnLib, which is likewise GPLv3 and built on
OpenVPN for Android. If you fork or redistribute, your derivative must remain
open source under the same license.

## Disclaimer

OpenGate VPN relies on servers volunteered by strangers on the internet through
the VPN Gate academic experiment. Treat it accordingly: don't send credentials
or sensitive data you wouldn't trust to an unknown relay operator, and be aware
that VPN Gate logs and publishes relay traffic metadata as part of its research
mission. This software is provided without warranty of any kind.
