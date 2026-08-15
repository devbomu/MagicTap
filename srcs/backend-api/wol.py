"""Magic-packet construction and broadcast (design doc §5.4).

The packet is broadcast to the subnet directed-broadcast address on UDP ports 9 and 7,
three times each at 100 ms spacing, since UDP delivery isn't guaranteed. This all happens
*inside* the LAN — the whole reason a Pico W agent exists.
"""

import socket
import time


def _mac_to_bytes(mac):
    cleaned = mac.replace(":", "").replace("-", "").replace(".", "").strip()
    if len(cleaned) != 12:
        raise ValueError("bad MAC: %r" % mac)
    return bytes(int(cleaned[i:i + 2], 16) for i in range(0, 12, 2))


def magic_packet(mac):
    return b"\xff" * 6 + _mac_to_bytes(mac) * 16


def broadcast_address(ip, subnet):
    ip_parts = [int(x) for x in ip.split(".")]
    mask_parts = [int(x) for x in subnet.split(".")]
    bcast = [(ip_parts[i] & mask_parts[i]) | (~mask_parts[i] & 0xFF) for i in range(4)]
    return ".".join(str(x) for x in bcast)


def send(mac, local_ip, subnet="255.255.255.0", ports=(9, 7), repeat=3, gap_ms=100):
    """Broadcasts the magic packet. Returns the broadcast address used."""
    packet = magic_packet(mac)
    bcast = broadcast_address(local_ip, subnet)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    try:
        for _ in range(repeat):
            for port in ports:
                try:
                    sock.sendto(packet, (bcast, port))
                except OSError:
                    pass  # keep firing the remaining packets
            time.sleep_ms(gap_ms)
    finally:
        sock.close()
    return bcast
