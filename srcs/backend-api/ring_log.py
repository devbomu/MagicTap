"""Fixed-size in-memory request log, exposed via ``GET /log`` for debugging.

Holds only the most recent N entries (default 32, per the design doc §6.2). Nothing is
written to flash — this is diagnostic, not persistent.
"""


class RingLog:
    def __init__(self, size=32):
        self._size = size
        self._items = []

    def add(self, entry):
        self._items.append(entry)
        while len(self._items) > self._size:
            self._items.pop(0)

    def items(self):
        return list(self._items)
