# MagicTap

> One tap on an Android home-screen widget powers on your PC — from anywhere, no cloud, no server.

MagicTap is a personal **Wake-on-LAN (WOL)** system. An Android widget sends a signed HTTP request to a tiny always-on **Raspberry Pi Pico W** sitting on your home LAN, and the Pico W fires the magic packet locally. This sidesteps every reason WOL-from-outside normally fails (routers refusing broadcast port-forwarding, disappearing ARP entries, ISP router CAPTCHAs).

No backend server. No database. No account. No telemetry. Your data never leaves your phone.

```
 [ Outside ]                         [ Home LAN ]

 ┌────────────┐   HTTP    ┌────────┐  port    ┌─────────┐
 │  Android   │ ────────► │ Router │ ───────► │ Pico W  │
 │ app/widget │           │  (NAT) │  forward │ (always │
 └────────────┘           └────────┘          │   on)   │
                                              └────┬────┘
                                    UDP broadcast  │  (magic packet)
                                              ┌────▼────┐
                                              │ Target  │
                                              │   PC    │
                                              └─────────┘
```

On home Wi-Fi the app talks to the Pico W directly (no router hairpin needed); outside it goes through a single port-forward. See [`docs/`](docs/) for the full design rationale (Korean).

## Repository layout

| Path | What it is | Stack |
|---|---|---|
| [`srcs/aos-app`](srcs/aos-app) | Android app + home-screen widgets | Kotlin · Jetpack Compose · Glance |
| [`srcs/backend-api`](srcs/backend-api) | Pico W firmware (the "agent") | MicroPython |
| [`docs`](docs) | Design document | — |

> **Why `backend-api` holds firmware, not a server:** the design deliberately has *no* backend server. The Pico W firmware fills the role the planning doc reserved for "a backend, if needed." The folder name is kept for continuity.

## How it works

1. You register a **profile** (one home / one Pico W) and its **PCs** (by MAC address) in the app. Everything is stored encrypted on-device (Android Keystore, AES-256-GCM).
2. Tapping a widget opens a small translucent confirm dialog — *not* the full app — to prevent accidental wakes.
3. On confirm, the app signs `HMAC-SHA256(secret, "MAC|timestamp")` and `POST`s it to the Pico W:
   - first to the **internal** address (300 ms timeout), then falling back to the **external** DDNS address.
4. The Pico W verifies the signature, the ±60 s time window, and a replay ring-buffer, then broadcasts the magic packet to ports 9 and 7 (3× each).

Plaintext HTTP is safe here because the secret is never transmitted — only a one-time signature that can't be replayed. See [`srcs/backend-api/README.md`](srcs/backend-api/README.md#security-model) for the threat model.

## Quick start

The order matters — **prove the Pico W wakes a PC before touching the app.** A single `curl` proves the whole path.

1. **Physical prep** — router port-forward, target-PC BIOS/OS WOL settings, DuckDNS domain. Full checklist: [one-time setup](#one-time-physical-setup) below.
2. **Flash the Pico W** — [`srcs/backend-api/README.md`](srcs/backend-api/README.md).
3. **Build the app** — open [`srcs/aos-app`](srcs/aos-app) in Android Studio and run. [`srcs/aos-app/README.md`](srcs/aos-app/README.md).

## One-time physical setup

### Router (tested against SK Broadband, applies broadly)
- [ ] Confirm the router is in **router mode** (AP/bridge mode can't port-forward — the most common failure).
- [ ] Give the Pico W a **fixed IP** (DHCP reservation or static on the Pico W).
- [ ] Add **one** port-forward: external port (e.g. `18080`) → `PicoW-IP:80`.
- [ ] Make sure **AP isolation / guest network** is off for the Pico W's Wi-Fi.

### Target PC
- [ ] BIOS/UEFI: enable *Resume by PCI-E / Network Device* (or equivalent).
- [ ] Device Manager → NIC → Power Management → *Allow this device to wake the computer* + *Only allow a magic packet…*.
- [ ] NIC Advanced tab: enable *Wake on Magic Packet*.
- [ ] **Control Panel → Power Options → turn Fast Startup OFF** — with it on, WOL often fails from a full shutdown.
- [ ] Prefer a **wired** connection (wireless WOL is unreliable).

### Pico W
- [ ] Flash MicroPython, upload the firmware + your `config.json`.
- [ ] Keep it on USB power 24/7 (router USB port or a wall adapter — ~0.5 W).

### External
- [ ] Create a DuckDNS domain and token.

## Design principles

- **Deterministic over convenient** — scoped to personal use so the network can be fixed and every feature works 100% of the time.
- **No data collection** — `INTERNET` is the only permission; no location, no storage, no analytics.
- **Fail loud, retry cheap** — no PC power-state monitoring; if a tap doesn't wake it, tap again.

Explicitly **out of scope** (by design): PC auto-discovery, power-state monitoring, Play Store release, remote shutdown, iOS.

## License

[MIT](LICENSE) © 2026 devbomu
