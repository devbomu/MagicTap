"""MagicTap Pico W firmware — entry point (design doc §6).

Boot sequence: load config -> start watchdog -> connect Wi-Fi -> NTP -> start HTTP server,
then run three cooperative tasks forever:
  * the HTTP server (wake / ping / log),
  * a watchdog feeder,
  * a maintenance loop (Wi-Fi recovery, periodic NTP resync, DuckDNS refresh).

The watchdog is the single most important requirement: if anything wedges the event
loop for longer than WDT_TIMEOUT_MS, the board reboots itself back to a working state.
"""

try:
    import asyncio
except ImportError:  # older MicroPython
    import uasyncio as asyncio

import time

import machine

import auth as auth_mod
import clock
import config
import duckdns
import wol
from http_server import HttpServer
from ring_log import RingLog
from wifi_manager import WiFiManager

FW_VERSION = "1.0.0"

WDT_TIMEOUT_MS = 8000
NTP_RESYNC_MS = 6 * 3600 * 1000     # 6 hours
DDNS_INTERVAL_MS = 5 * 60 * 1000    # 5 minutes
MAINTENANCE_TICK_MS = 20 * 1000     # 20 seconds
MAX_WIFI_FAILURES = 5


def _safe_ntp(wifi, wdt):
    wdt.feed()
    try:
        wifi.sync_time()
        return True
    except Exception:
        return False


async def _watchdog_task(wdt):
    while True:
        wdt.feed()
        await asyncio.sleep_ms(2000)


async def _maintenance_task(cfg, wifi, wdt, log, time_synced):
    failures = 0
    last_ntp = time.ticks_ms()
    last_ddns = None  # None => update on the first connected pass

    while True:
        wdt.feed()

        if not wifi.isconnected():
            if wifi.connect(feed=wdt.feed):
                failures = 0
                if _safe_ntp(wifi, wdt):
                    time_synced = True
                    last_ntp = time.ticks_ms()
            else:
                failures += 1
                log.add({"t": clock.unix_now(), "event": "wifi_fail", "n": failures})
                if failures >= MAX_WIFI_FAILURES:
                    machine.reset()
        else:
            # Retry NTP until the clock is set, then only every NTP_RESYNC_MS.
            if (not time_synced or time.ticks_diff(time.ticks_ms(), last_ntp) > NTP_RESYNC_MS) and _safe_ntp(wifi, wdt):
                time_synced = True
                last_ntp = time.ticks_ms()

            if cfg["duckdns_domain"] and (
                last_ddns is None or time.ticks_diff(time.ticks_ms(), last_ddns) > DDNS_INTERVAL_MS
            ):
                wdt.feed()
                # Send NO ip so DuckDNS records the request's *public* source IP. Passing
                # the Pico's own wifi.ip() would register a LAN address (e.g. 192.168.0.50),
                # which is useless from outside — and behind double NAT the Pico can't even
                # know its public IP, so letting DuckDNS detect it is the only correct way.
                ok = duckdns.update(cfg["duckdns_domain"], cfg["duckdns_token"])
                last_ddns = time.ticks_ms()
                log.add({"t": clock.unix_now(), "event": "ddns", "ok": ok})

        await asyncio.sleep_ms(MAINTENANCE_TICK_MS)


async def main():
    cfg = config.load()
    log = RingLog(32)
    wdt = machine.WDT(timeout=WDT_TIMEOUT_MS)

    wifi = WiFiManager(cfg)
    if not wifi.connect(feed=wdt.feed):
        # Couldn't join the network at boot: reboot and try from scratch.
        time.sleep(1)
        machine.reset()

    time_synced = _safe_ntp(wifi, wdt)

    authenticator = auth_mod.Authenticator(cfg["secret"])
    server = HttpServer(cfg, wifi, authenticator, log, wol, FW_VERSION)
    await server.start()

    asyncio.create_task(_watchdog_task(wdt))
    asyncio.create_task(_maintenance_task(cfg, wifi, wdt, log, time_synced))

    while True:
        await asyncio.sleep(3600)


try:
    asyncio.run(main())
finally:
    asyncio.new_event_loop()
