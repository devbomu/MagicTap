"""Request authentication (design doc §5.1).

    sig = HMAC-SHA256(secret, mac + "|" + ts)

A request is accepted only when all three hold:
  1. the signature matches,
  2. ``ts`` is within ``window`` seconds of the Pico's (NTP-synced) clock,
  3. the signature has not been seen recently (replay defence).

MicroPython ships SHA-256 in ``hashlib`` but no ``hmac`` module, so HMAC is implemented
here. The replay buffer keys on the full signature rather than on ``ts`` alone: two
different PCs woken in the same second produce different signatures and are both allowed,
while a captured request replayed verbatim is rejected.
"""

import hashlib
import ubinascii

_BLOCK_SIZE = 64  # SHA-256 block size in bytes


def _hmac_sha256(key, msg):
    if len(key) > _BLOCK_SIZE:
        key = hashlib.sha256(key).digest()
    key = key + b"\x00" * (_BLOCK_SIZE - len(key))
    inner = hashlib.sha256(bytes(b ^ 0x36 for b in key) + msg).digest()
    return hashlib.sha256(bytes(b ^ 0x5C for b in key) + inner).digest()


def _const_eq(a, b):
    # Length-independent-ish constant-time compare for equal-length hex strings.
    if len(a) != len(b):
        return False
    result = 0
    for x, y in zip(a, b):
        result |= ord(x) ^ ord(y)
    return result == 0


class Authenticator:
    def __init__(self, secret_b64, window=60, replay_size=32):
        self._key = ubinascii.a2b_base64(secret_b64)
        self._window = window
        self._replay_size = replay_size
        self._recent_sigs = []

    def verify(self, mac, ts, sig_hex, now):
        """Returns ``"ok"`` if the (mac, ts, sig) triple is authentic, fresh and unused.

        Otherwise a reason the caller can surface to distinguish causes that otherwise look
        identical to the app:
          * ``"clock"``  – timestamp outside the ±window; almost always the Pico's NTP time
                           isn't set yet (NOT a wrong secret).
          * ``"replay"`` – this exact signature was already used.
          * ``"auth"``   – bad signature (wrong secret) or malformed timestamp.
        """
        try:
            ts = int(ts)
        except (ValueError, TypeError):
            return "auth"

        if abs(now - ts) > self._window:
            return "clock"

        sig_hex = str(sig_hex).lower()
        if sig_hex in self._recent_sigs:
            return "replay"

        expected = ubinascii.hexlify(_hmac_sha256(self._key, (mac + "|" + str(ts)).encode())).decode()
        if not _const_eq(expected, sig_hex):
            return "auth"

        self._recent_sigs.append(sig_hex)
        while len(self._recent_sigs) > self._replay_size:
            self._recent_sigs.pop(0)
        return "ok"
