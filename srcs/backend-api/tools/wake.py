#!/usr/bin/env python3
"""Sign and send a /wake (or /ping) to a MagicTap Pico W — no Android app needed.

This is the "prove the Pico W wakes a PC before building the app" step (design doc §12).
Pure standard library.

Examples:
    # Connection test
    python3 wake.py --host 192.168.35.50 --ping

    # Wake a PC on the LAN
    python3 wake.py --host 192.168.35.50 --mac AA:BB:CC:DD:EE:FF --secret <base64-secret>

    # Wake from outside, through the port-forward
    python3 wake.py --host myhome.duckdns.org --port 18080 \\
        --mac AA:BB:CC:DD:EE:FF --secret <base64-secret>
"""

import argparse
import base64
import hashlib
import hmac
import json
import sys
import time
import urllib.error
import urllib.request


def canonical_mac(mac: str) -> str:
    cleaned = "".join(c for c in mac.upper() if c in "0123456789ABCDEF")
    if len(cleaned) != 12:
        raise SystemExit(f"invalid MAC address: {mac!r}")
    return ":".join(cleaned[i:i + 2] for i in range(0, 12, 2))


def sign(secret_b64: str, mac: str, ts: int) -> str:
    key = base64.b64decode(secret_b64)
    return hmac.new(key, f"{mac}|{ts}".encode(), hashlib.sha256).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="MagicTap wake/ping test client")
    parser.add_argument("--host", required=True, help="Pico W address (LAN IP or DDNS host)")
    parser.add_argument("--port", type=int, default=80)
    parser.add_argument("--mac", help="target PC MAC (any separators)")
    parser.add_argument("--secret", help="base64 HMAC secret (same value as the Pico's config.json)")
    parser.add_argument("--ping", action="store_true", help="just call /ping (no auth)")
    parser.add_argument("--timeout", type=float, default=5.0)
    args = parser.parse_args()

    base = f"http://{args.host}:{args.port}"

    if args.ping:
        with urllib.request.urlopen(f"{base}/ping", timeout=args.timeout) as resp:
            print(resp.status, resp.read().decode())
        return 0

    if not args.mac or not args.secret:
        parser.error("--mac and --secret are required unless --ping is used")

    mac = canonical_mac(args.mac)
    ts = int(time.time())
    body = json.dumps({"mac": mac, "ts": ts, "sig": sign(args.secret, mac, ts)}).encode()
    request = urllib.request.Request(
        f"{base}/wake",
        data=body,
        headers={"Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(request, timeout=args.timeout) as resp:
            print(resp.status, resp.read().decode())
    except urllib.error.HTTPError as e:
        print(e.code, e.read().decode())
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
