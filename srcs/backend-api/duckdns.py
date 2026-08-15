"""DuckDNS updater (design doc §6.2).

Kept dependency-free: a minimal HTTPS GET over ``ssl`` + ``socket`` instead of pulling in
``urequests``. Every call is wrapped so a DDNS failure can never affect the wake path —
the caller treats this as best-effort.
"""

import socket
import ssl

_HOST = "www.duckdns.org"


def _https_get(host, path, timeout=6):
    addr = socket.getaddrinfo(host, 443)[0][-1]
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(addr)
        # MicroPython's ssl doesn't verify certs by default; acceptable here since the
        # request carries only the (public) domain and token to a fixed host.
        stream = ssl.wrap_socket(sock, server_hostname=host)
        request = "GET {} HTTP/1.0\r\nHost: {}\r\nConnection: close\r\n\r\n".format(path, host)
        stream.write(request.encode())
        chunks = []
        while True:
            data = stream.read(256)
            if not data:
                break
            chunks.append(data)
        return b"".join(chunks)
    finally:
        try:
            sock.close()
        except OSError:
            pass


def update(domain, token, ip=""):
    """Best-effort DuckDNS update. Returns True on an ``OK`` response, else False."""
    if not domain or not token:
        return False
    path = "/update?domains={}&token={}&ip={}".format(domain, token, ip)
    try:
        response = _https_get(_HOST, path)
        return b"OK" in response
    except Exception:
        return False
