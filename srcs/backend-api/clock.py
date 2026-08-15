"""Unix-time helper.

The Android app signs requests with standard Unix time (1970-01-01), and the HMAC
timestamp check compares against the Pico's clock. But MicroPython builds disagree on the
epoch: traditional bare-metal RP2040/RP2350 builds count from 2000-01-01, while some newer
builds (and the Unix port) already count from 1970-01-01. Flashing the "wrong" .uf2 would
otherwise push every timestamp 30 years off and make every wake fail the ±60 s window.

So we DETECT the epoch at runtime — ``time.gmtime(0)[0]`` is the epoch's year — and only
add the 30-year gap on 2000-epoch builds. Don't hardcode it back.
"""

import time

EPOCH_OFFSET = 946684800 if time.gmtime(0)[0] == 2000 else 0  # 2000-epoch builds need the gap; 1970 builds don't


def unix_now():
    """Current time as Unix seconds (UTC). Requires NTP to have run first."""
    return time.time() + EPOCH_OFFSET
