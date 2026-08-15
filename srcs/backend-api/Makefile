# MagicTap Pico W firmware — mpremote / curl helpers.
#
# Override the serial device per command:
#   make sync PORT=/dev/cu.usbmodem11101
# config.json is gitignored and the app exports it under a different name, so point CONFIG
# at your exported file:
#   make install CONFIG=~/Downloads/magictap-picoconfig.json PORT=/dev/cu.usbmodem11101
# curl targets talk to the running Pico over HTTP; override its address with IP=...

PORT   ?= /dev/ttyACM0
IP     ?= 192.168.0.50
CONFIG ?= config.json

.DEFAULT_GOAL := help
.PHONY: help ports upload config reset sync install repl ls cat-config time ntp netcheck dnstest fixdns wipe log ping

help:
	@echo "MagicTap Pico helpers   (PORT=$(PORT)  IP=$(IP)  CONFIG=$(CONFIG))"
	@echo ""
	@echo "  make ports       list connected serial devices  (find your PORT=…)"
	@echo "  make install     upload code + push config + reset   (first time / config changed)"
	@echo "  make sync        upload code + reset                 (most common while debugging)"
	@echo "  make upload      copy *.py to the Pico"
	@echo "  make config      copy CONFIG to the Pico as config.json"
	@echo "  make reset       reboot the Pico"
	@echo "  make repl        serial REPL   (Ctrl-] to exit)"
	@echo "  make ls          list files on the Pico"
	@echo "  make cat-config  print the Pico's config.json"
	@echo "  make time        print the Pico clock  (epoch + NTP check)"
	@echo "  make ntp         test DNS + force an NTP sync  (shows the exact error)"
	@echo "  make netcheck    show live ifconfig + test raw outbound (8.8.8.8:53)"
	@echo "  make dnstest     raw UDP DNS query to 8.8.8.8  (UDP-blocked vs resolver-bug)"
	@echo "  make fixdns      live: set DNS via ipconfig, then try to resolve"
	@echo "  make wipe        delete ALL files on the Pico  (careful!)"
	@echo "  make log / ping  GET /log or /ping over HTTP"
	@echo ""
	@echo "  config is outside git — point CONFIG at your exported file:"
	@echo "    make install CONFIG=~/Downloads/magictap-picoconfig.json PORT=/dev/cu.usbmodem11101"

upload:
	mpremote connect $(PORT) fs cp *.py :

config:
	mpremote connect $(PORT) fs cp $(CONFIG) :config.json

reset:
	mpremote connect $(PORT) reset

sync: upload reset

install: upload config reset

ports:
	mpremote connect list

repl:
	mpremote connect $(PORT) repl

ls:
	mpremote connect $(PORT) fs ls

cat-config:
	@mpremote connect $(PORT) fs cat config.json

time:
	mpremote connect $(PORT) exec "import time, clock; print('epoch year:', time.gmtime(0)[0]); print('pico thinks:', time.gmtime(time.time())); print('unix_now  :', clock.unix_now())"

ntp:
	mpremote connect $(PORT) exec "import socket; print('DNS pool.ntp.org ->', socket.getaddrinfo('pool.ntp.org', 123)[0][-1])"
	mpremote connect $(PORT) exec "import ntptime, time; ntptime.host = 'pool.ntp.org'; ntptime.settime(); print('NTP OK ->', time.gmtime())"

netcheck:
	mpremote connect $(PORT) exec "import network, socket; w = network.WLAN(network.STA_IF); print('connected:', w.isconnected()); print('ifconfig (ip,subnet,gw,dns):', w.ifconfig()); s = socket.socket(); s.settimeout(4); s.connect(('8.8.8.8', 53)); print('outbound 8.8.8.8:53 -> OK'); s.close()"

dnstest:
	mpremote connect $(PORT) exec "import socket; q=b'\xaa\xaa\x01\x00\x00\x01\x00\x00\x00\x00\x00\x00'+b''.join(bytes([len(p)])+p.encode() for p in 'pool.ntp.org'.split('.'))+b'\x00\x00\x01\x00\x01'; s=socket.socket(socket.AF_INET, socket.SOCK_DGRAM); s.settimeout(4); s.sendto(q, ('8.8.8.8', 53)); r=s.recv(512); print('UDP DNS reply bytes:', len(r)); s.close()"

fixdns:
	mpremote connect $(PORT) exec "import network, socket; network.ipconfig(dns='8.8.8.8'); print('network.ipconfig(dns=) OK; resolving...'); print('resolve ->', socket.getaddrinfo('pool.ntp.org', 123)[0][-1])"

wipe:
	mpremote connect $(PORT) exec "import os; [os.remove(f) for f in os.listdir()]"

log:
	@curl -s http://$(IP)/log; echo

ping:
	@curl -s http://$(IP)/ping; echo
