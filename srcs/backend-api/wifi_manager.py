"""Wi-Fi connection + NTP time sync (design doc §6.2).

Fixed IP, exponential backoff on failure, and a bounded number of attempts (never an
unbounded busy loop). A ``feed`` callback is invoked during every wait so the caller can
pet the watchdog while connecting.
"""

import time

import network
import ntptime


def _noop():
    pass


class WiFiManager:
    def __init__(self, cfg):
        self._cfg = cfg
        self._wlan = network.WLAN(network.STA_IF)

    def connect(self, feed=_noop, max_attempts=6):
        wlan = self._wlan
        wlan.active(True)
        self._apply_static()

        for attempt in range(max_attempts):
            if wlan.isconnected():
                self._apply_static()
                return True
            try:
                wlan.connect(self._cfg["wifi_ssid"], self._cfg["wifi_pass"])
            except OSError:
                pass

            # Wait up to ~10 s for association, feeding the watchdog throughout.
            self._wait(feed, 10000, until=wlan.isconnected)
            if wlan.isconnected():
                self._apply_static()
                return True

            # Exponential backoff (capped) before the next attempt.
            backoff_s = min(2 ** attempt, 20)
            self._wait(feed, backoff_s * 1000)

        return wlan.isconnected()

    def isconnected(self):
        return self._wlan.isconnected()

    def ip(self):
        return self._wlan.ifconfig()[0]

    def sync_time(self):
        """Blocking NTP sync. Raises on failure — callers isolate it."""
        ntptime.host = self._cfg["ntp_host"]
        ntptime.settime()

    def _apply_static(self):
        """Apply the fixed IP / subnet / gateway, and register the DNS server.

        On MicroPython v1.24+ (recent Pico W builds) the DNS server is a stack-global
        setting via ``network.ipconfig(dns=...)`` — it is NOT taken from the per-interface
        ifconfig() tuple. Without the explicit call, getaddrinfo() fails with OSError(-2)
        even though ifconfig() reads the DNS address back and raw UDP/TCP to the resolver
        both work. ``ifconfig`` still sets the static address; the DNS call is guarded so
        older firmware (where the tuple sufficed) keeps working. Also re-applied after
        association, since the resolver only registers reliably on the live link.
        """
        ip = self._cfg["static_ip"]
        if ip:
            self._wlan.ifconfig((ip, self._cfg["subnet"], self._cfg["gateway"], self._cfg["dns"]))
        dns = self._cfg["dns"]
        if dns:
            try:
                network.ipconfig(dns=dns)
            except (AttributeError, ValueError, OSError):
                pass

    @staticmethod
    def _wait(feed, ms, until=None):
        end = time.ticks_add(time.ticks_ms(), ms)
        while time.ticks_diff(end, time.ticks_ms()) > 0:
            if until is not None and until():
                return
            feed()
            time.sleep_ms(200)
