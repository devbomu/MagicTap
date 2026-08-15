"""Async HTTP server (design doc §5.2, §6.2).

Endpoints:
  GET  /ping   -> {"ok": true, "uptime": <ms>, "fw": "<version>"}  (no auth)
  POST /wake   -> {"ok": true} | 401 {"ok": false, "err": "auth"}  (HMAC-authenticated)
  POST /verify -> {"ok": true} | 401 {"ok": false, "err": "auth"}  (HMAC, no side effect)
  GET  /log    -> {"ok": true, "log": [...]}                       (debug, last 32 requests)

Built on asyncio streams. The Pico W has very few sockets, so connections are handled
one at a time and closed immediately (``Connection: close``) — no keep-alive, no
parallelism, exactly as the design doc requires.
"""

try:
    import asyncio
except ImportError:  # older MicroPython
    import uasyncio as asyncio

import json
import time

import clock

_REASONS = {
    200: "OK",
    400: "Bad Request",
    401: "Unauthorized",
    403: "Forbidden",
    404: "Not Found",
    500: "Internal Server Error",
}

# Cap request bodies: /wake and /verify payloads are ~100 bytes. A huge Content-Length
# would otherwise let an unauthenticated client exhaust the Pico's tiny RAM.
_MAX_BODY = 1024

# Drop a client that can't send a complete request within this many seconds. Without it a
# slow or never-finishing connection (slowloris) could hold one of the Pico's very few
# sockets open indefinitely; a handful would exhaust them and block legitimate wakes.
_READ_TIMEOUT_S = 5


def _is_private(ip):
    """True for RFC1918 / loopback source addresses. Keeps /log LAN-only so a
    port-forwarded Pico never leaks MACs and wake history to the internet."""
    try:
        parts = [int(x) for x in ip.split(".")]
    except (ValueError, AttributeError):
        return False
    if len(parts) != 4:
        return False
    a, b = parts[0], parts[1]
    if a == 10 or a == 127:
        return True
    if a == 192 and b == 168:
        return True
    if a == 172 and 16 <= b <= 31:
        return True
    return False


class HttpServer:
    def __init__(self, cfg, wifi, auth, log, wol, fw_version):
        self._cfg = cfg
        self._wifi = wifi
        self._auth = auth
        self._log = log
        self._wol = wol
        self._fw = fw_version
        self._boot_ticks = time.ticks_ms()

    async def start(self):
        await asyncio.start_server(self._handle, "0.0.0.0", self._cfg["http_port"])

    async def _handle(self, reader, writer):
        try:
            # Bound the time spent reading the request so a slow or incomplete client can't
            # pin one of the Pico's scarce sockets open (slowloris / socket exhaustion).
            try:
                parsed = await asyncio.wait_for(self._read_request(reader), _READ_TIMEOUT_S)
            except asyncio.TimeoutError:
                return  # drop the connection; the finally-block closes it

            if parsed is None:
                return  # peer connected but sent nothing
            method, path, body = parsed
            if not method:
                await self._send(writer, 400, {"ok": False, "err": "bad_request"})
                return

            if path == "/ping" and method == "GET":
                await self._ping(writer)
            elif path == "/wake" and method == "POST":
                await self._wake(writer, body)
            elif path == "/verify" and method == "POST":
                await self._verify(writer, body)
            elif path == "/log" and method == "GET":
                peer = writer.get_extra_info("peername")
                if peer and _is_private(peer[0]):
                    await self._send(writer, 200, {"ok": True, "log": self._log.items()})
                else:
                    await self._send(writer, 403, {"ok": False, "err": "forbidden"})
            else:
                await self._send(writer, 404, {"ok": False, "err": "not_found"})
        except Exception:
            try:
                await self._send(writer, 500, {"ok": False, "err": "server"})
            except Exception:
                pass
        finally:
            await self._close(writer)

    async def _read_request(self, reader):
        """Read the request line, headers, and (for POST) the body.

        Returns ``(method, path, body)``. Returns ``None`` if the peer sent nothing, and
        ``("", "", b"")`` for a malformed request line so the caller can answer 400. All of
        the blocking reads live here so [_handle] can wrap them in a single timeout.
        """
        request_line = await reader.readline()
        if not request_line:
            return None
        parts = request_line.decode().split()
        if len(parts) < 2:
            return "", "", b""
        method, raw_path = parts[0], parts[1]
        path = raw_path.split("?", 1)[0]
        content_length = await self._read_headers(reader)
        body = await self._read_body(reader, content_length) if method == "POST" else b""
        return method, path, body

    @staticmethod
    async def _read_headers(reader):
        content_length = 0
        while True:
            line = await reader.readline()
            if not line or line == b"\r\n":
                break
            name, _, value = line.partition(b":")
            if name.strip().lower() == b"content-length":
                try:
                    content_length = int(value.strip())
                except ValueError:
                    content_length = 0
        return content_length

    @staticmethod
    async def _read_body(reader, length):
        chunks = []
        remaining = min(length, _MAX_BODY)
        while remaining > 0:
            chunk = await reader.read(remaining)
            if not chunk:
                break
            chunks.append(chunk)
            remaining -= len(chunk)
        return b"".join(chunks)

    async def _ping(self, writer):
        uptime = time.ticks_diff(time.ticks_ms(), self._boot_ticks)
        await self._send(writer, 200, {"ok": True, "uptime": uptime, "fw": self._fw})

    async def _wake(self, writer, body):
        try:
            data = json.loads(body)
            mac = data["mac"]
            ts = data["ts"]
            sig = data["sig"]
        except (ValueError, KeyError, TypeError):
            await self._send(writer, 400, {"ok": False, "err": "bad_request"})
            return

        now = clock.unix_now()
        reason = self._auth.verify(mac, ts, sig, now)
        authentic = reason == "ok"
        entry = {"t": now, "mac": mac, "ok": authentic}

        if not authentic:
            entry["err"] = reason
            self._log.add(entry)
            await self._send(writer, 401, {"ok": False, "err": reason})
            return

        try:
            entry["bcast"] = self._wol.send(mac, self._wifi.ip(), self._cfg["subnet"])
            self._log.add(entry)
            await self._send(writer, 200, {"ok": True})
        except Exception:
            entry["err"] = "wol"
            self._log.add(entry)
            await self._send(writer, 500, {"ok": False, "err": "wol"})

    async def _verify(self, writer, body):
        """Proves the shared secret with no side effect — used by the app's setup check.

        Same signed payload as /wake, but never broadcasts. Lets the app confirm the
        secret matches without turning a PC on.
        """
        try:
            data = json.loads(body)
            mac = data["mac"]
            ts = data["ts"]
            sig = data["sig"]
        except (ValueError, KeyError, TypeError):
            await self._send(writer, 400, {"ok": False, "err": "bad_request"})
            return
        reason = self._auth.verify(mac, ts, sig, clock.unix_now())
        if reason == "ok":
            await self._send(writer, 200, {"ok": True})
        else:
            await self._send(writer, 401, {"ok": False, "err": reason})

    async def _send(self, writer, status, obj):
        body = json.dumps(obj).encode()
        head = "HTTP/1.1 {} {}\r\n".format(status, _REASONS.get(status, "OK"))
        writer.write(head.encode())
        writer.write(b"Content-Type: application/json\r\n")
        writer.write("Content-Length: {}\r\n".format(len(body)).encode())
        writer.write(b"Connection: close\r\n\r\n")
        writer.write(body)
        await writer.drain()

    @staticmethod
    async def _close(writer):
        try:
            await writer.drain()
        except Exception:
            pass
        try:
            writer.close()
            await writer.wait_closed()
        except Exception:
            pass
