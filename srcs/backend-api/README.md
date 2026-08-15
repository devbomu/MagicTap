# MagicTap Pico W firmware

The always-on LAN agent that receives a signed HTTP request from the MagicTap app and
fires the Wake-on-LAN magic packet locally. MicroPython, no cloud, no dependencies beyond
what the standard Pico W build ships with.

> Despite living under `backend-api/`, this is **not** a server you host — it's firmware
> for a ~$6 board. The design has no backend; the Pico W fills that role. See the
> [repo README](../../README.md) for the whole picture.

## Why a Pico W (not a port-forward, not an always-on PC)

Waking a PC from outside your home needs the magic packet **broadcast inside the LAN**.
Home routers generally refuse to port-forward to a broadcast address, ARP entries for a
powered-off PC expire, and ISP routers' remote-WOL pages hide behind CAPTCHAs. A tiny
always-on device with a fixed private IP sidesteps all of it: ordinary port-forwarding
reaches it, and it broadcasts from inside. (An always-on PC can't be the agent — you
can't use the very machine you're trying to wake, and you might turn the helper PC off.)

## Files

| File | Role |
|---|---|
| `main.py` | Boot sequence + watchdog + the three async tasks |
| `config.py` | Loads `config.json` over defaults |
| `wifi_manager.py` | Fixed-IP Wi-Fi with backoff, NTP sync |
| `clock.py` | Unix-time conversion (**epoch gotcha — read it**) |
| `auth.py` | HMAC-SHA256 verify + timestamp window + replay defence |
| `wol.py` | Magic-packet build + subnet broadcast |
| `http_server.py` | asyncio HTTP server (`/wake`, `/ping`, `/log`) |
| `duckdns.py` | Best-effort DDNS refresh (isolated) |
| `ring_log.py` | Last-32-requests debug log |
| `config.example.json` | Template — copy to `config.json` |
| `tools/wake.py` | Desktop test client (stdlib Python) |

## Security model

The Pico speaks plain HTTP — running TLS on it is impractical. Safety comes from the
request signature, not the transport (design doc §5.1):

```
sig = HMAC-SHA256(secret, mac + "|" + ts)
```

A request is honored only if **all three** hold:

1. the signature matches (so the 32-byte secret is proven without ever being sent),
2. `ts` is within ±60 s of the Pico's NTP-synced clock,
3. that exact signature hasn't been seen recently (replay ring-buffer of 32).

So a sniffed request can't be replayed, and the secret never travels. Worst case if the
port is found and abused: someone can turn your PC *on*. Use a non-standard external port
anyway.

## Setup

1. **Flash MicroPython** for your board from <https://micropython.org/download/> (Pico W
   or Pico 2 W). Thonny → *Install MicroPython* is the easy path.
2. **Create `config.json`** from `config.example.json`. Paste the `secret` shown in the
   app's profile editor. Set `static_ip`/`gateway` to match your LAN, or leave `static_ip`
   empty for DHCP (then reserve the IP on your router).
3. **Upload** every `.py` file plus `config.json` to the board (Thonny, `mpremote`, or
   `rshell`). `config.json` is gitignored — keep your real secret off GitHub.
4. **Reset** the board. It joins Wi-Fi, syncs time, and starts serving on port 80.

`mpremote` example:

```bash
mpremote connect /dev/ttyACM0 fs cp *.py config.json :
mpremote connect /dev/ttyACM0 reset
```

## Verify before building the app

Prove the whole path with the test client — if this wakes your PC, the app will too:

```bash
# reachable?
python3 tools/wake.py --host 192.168.35.50 --ping

# wake it (on the LAN)
python3 tools/wake.py --host 192.168.35.50 --mac AA:BB:CC:DD:EE:FF --secret <base64-secret>
```

`/ping` needs no auth, so a bare `curl http://192.168.35.50/ping` also confirms the server
is up.

## Endpoints

| Method / Path | Auth | Response |
|---|---|---|
| `GET /ping` | none | `{"ok":true,"uptime":<ms>,"fw":"1.0.0"}` |
| `POST /wake` | HMAC | `200 {"ok":true}` / `401 {"ok":false,"err":"auth"}` |
| `GET /log` | none | `{"ok":true,"log":[…last 32…]}` (debug) |

## Gotchas

- **NTP is mandatory.** The Pico has no RTC; without a time sync every wake fails the ±60 s
  check. The firmware retries NTP until it succeeds, then every 6 hours.
- **Epoch.** MicroPython on the Pico counts from 2000-01-01; the app uses 1970. `clock.py`
  bridges the 946684800-second gap. Don't "simplify" it away.
- **AP isolation / guest networks** block broadcast — connect the Pico to your **main**
  Wi-Fi, not a guest SSID.
- **Wired target PC.** Same subnet as the Pico so the router bridges the broadcast;
  wireless WOL is unreliable.
- **Sockets are scarce.** Requests are handled one at a time by design — don't add
  keep-alive or parallel handling.

## License

[MIT](../../LICENSE) — part of the [MagicTap](../../README.md) project.
