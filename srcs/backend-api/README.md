# MagicTap Pico W 펌웨어

MagicTap 앱에서 서명된 HTTP 요청을 받아 LAN 내부에서 Wake-on-LAN 매직 패킷을 발사하는,
상시 대기 에이전트입니다. MicroPython으로 작성되었고, Pico W 표준 빌드에 들어 있는 것 외의
의존성은 없습니다.

> `backend-api/` 아래에 있지만 이것은 **호스팅하는 서버가 아니라** ~$6짜리 보드용 펌웨어입니다.
> 이 설계에는 백엔드가 없고 Pico W가 그 역할을 합니다. 전체 그림은 [저장소 README](../../README.md)를
> 참고하세요.

## 왜 Pico W인가 (포트포워딩도, 상시 켠 PC도 아님)

집 밖에서 PC를 깨우려면 매직 패킷이 **LAN 내부에서 브로드캐스트**되어야 합니다. 그런데 가정용
공유기는 대개 브로드캐스트 주소로의 포트포워딩을 거부하고, 꺼진 PC의 ARP 엔트리는 만료되며,
통신사 공유기의 원격 WOL 페이지는 캡차 뒤에 숨어 있습니다. 고정 사설 IP를 가진 작은 상시 기기를
두면 이 문제가 전부 사라집니다 — 일반 포트포워딩으로 도달하고, 브로드캐스트는 내부에서 발사됩니다.
(상시 켠 PC는 에이전트로 못 씁니다. 깨우려는 그 PC 자신을 쓸 수 없고, 헬퍼 PC를 꺼버릴 수도 있으니까요.)

## 파일

| 파일 | 역할 |
|---|---|
| `main.py` | 부팅 시퀀스 + 워치독 + 협력형 비동기 태스크 3개 |
| `config.py` | 기본값 위에 `config.json`을 로드 |
| `wifi_manager.py` | 고정 IP Wi-Fi 접속(백오프 재시도), DNS 설정, NTP 동기화 |
| `clock.py` | 유닉스 시각 변환 (**에포크 함정 — 꼭 읽을 것**) |
| `auth.py` | HMAC-SHA256 검증 + 타임스탬프 창 + 재전송 방어 |
| `wol.py` | 매직 패킷 생성 + 서브넷 브로드캐스트 |
| `http_server.py` | asyncio HTTP 서버 (`/ping`·`/wake`·`/verify`·`/log`) |
| `duckdns.py` | 베스트에포트 DDNS 갱신(격리) |
| `ring_log.py` | 최근 32건 요청 디버그 로그 |
| `config.example.json` | 템플릿 — `config.json`으로 복사 |
| `tools/wake.py` | 데스크톱 테스트 클라이언트(표준 라이브러리만) |
| `Makefile` | mpremote / curl 헬퍼 (아래 참고) |

## 보안 모델

Pico는 평문 HTTP로 통신합니다 — TLS 운용은 비현실적입니다. 안전성은 전송이 아니라 요청 **서명**에서
옵니다(기획서 §5.1):

```
sig = HMAC-SHA256(secret, mac + "|" + ts)
```

다음 **세 조건이 모두** 성립할 때만 요청을 처리합니다.

1. 서명이 일치할 것 (32바이트 비밀키를 한 번도 보내지 않고 증명),
2. `ts`가 Pico의 NTP 동기화 시각 ±60초 이내일 것,
3. 그 서명이 최근에 사용된 적 없을 것 (재전송 링버퍼 32건).

따라서 스니핑한 요청은 재전송이 불가능하고 비밀키는 오가지 않습니다. 요청 본문은 1KB로 제한해
비정상적으로 큰 페이로드를 막습니다. 포트가 발견·악용되는 최악의 경우라도 피해는 "내 PC가 *켜짐*"뿐이지만,
그래도 외부 포트는 비표준으로 쓰세요.

## 설치

1. **MicroPython 굽기** — 보드에 맞는 이미지를 <https://micropython.org/download/>에서 받습니다
   (Pico W 또는 Pico 2 W). Thonny → *Install MicroPython*이 가장 쉽습니다.
2. **`config.json` 만들기** — `config.example.json`을 복사하고, 앱 프로필 편집 화면의 `secret`을
   붙여 넣습니다. `static_ip`/`gateway`를 LAN에 맞추거나, `static_ip`를 비우면 DHCP를 씁니다(이때는
   공유기에서 IP를 고정 할당).
   - 또는 **앱의 "Pico 설정 생성"**으로 `config.json`을 만들어 내보낸 뒤 그대로 올릴 수 있습니다.
3. **업로드** — 모든 `.py`와 `config.json`을 보드에 올립니다(Thonny, `mpremote`, `rshell` 등).
   `config.json`은 gitignore 대상이니 실제 비밀키를 GitHub에 올리지 마세요.
4. **리셋** — 보드가 Wi-Fi에 붙고 시각을 맞춘 뒤 80번 포트에서 서비스를 시작합니다.

`mpremote` 예시:

```bash
mpremote connect /dev/ttyACM0 fs cp *.py config.json :
mpremote connect /dev/ttyACM0 reset
```

### Makefile 헬퍼

반복 작업은 동봉된 `Makefile`로 줄일 수 있습니다. 시리얼 장치는 `PORT=`, HTTP 주소는 `IP=`,
내보낸 설정 파일은 `CONFIG=`로 지정합니다.

```bash
make ports                              # 연결된 시리얼 장치 나열 (PORT= 찾기)
make install CONFIG=~/Downloads/magictap-picoconfig.json PORT=/dev/cu.usbmodem11101
make sync PORT=...                      # 코드만 재업로드 + 리셋
make ping IP=192.168.0.50               # curl /ping (생존 확인)
make log  IP=192.168.0.50               # curl /log  (최근 요청 이력)
make time / make ntp                    # 시각 확인 / NTP 강제 동기화
make netcheck / make dnstest / make fixdns   # 네트워크 진단
```

## 앱 빌드 전에 검증하기

테스트 클라이언트로 전체 경로를 먼저 증명하세요 — 이걸로 PC가 켜지면 앱으로도 켜집니다:

```bash
# 도달 확인
python3 tools/wake.py --host 192.168.35.50 --ping

# 깨우기 (LAN 안에서)
python3 tools/wake.py --host 192.168.35.50 --mac AA:BB:CC:DD:EE:FF --secret <base64-secret>
```

`/ping`은 인증이 필요 없으므로 `curl http://192.168.35.50/ping`만으로도 서버 생존을 확인할 수 있습니다.

## 엔드포인트

| 메서드 / 경로 | 인증 | 응답 |
|---|---|---|
| `GET /ping` | 없음 | `{"ok":true,"uptime":<ms>,"fw":"1.0.0"}` |
| `POST /wake` | HMAC | `200 {"ok":true}` / `401 {"ok":false,"err":"auth\|clock\|replay"}` |
| `POST /verify` | HMAC | `200 {"ok":true}` / `401 …` — 부수 효과 없음(브로드캐스트 안 함). 앱 연결 테스트의 비밀키 검증용 |
| `GET /log` | LAN 전용 | `{"ok":true,"log":[…최근 32건…]}` (디버그). 외부 소스 IP는 `403` |

`err` 사유는 원인을 구분해 줍니다: `auth`(서명 불일치=비밀키 오류), `clock`(±60초 밖=대개 Pico의
시각 미동기화), `replay`(이미 사용된 서명). 앱은 이 값으로 "비밀키 불일치"와 "시각 오류"를 구분해
보여 줍니다.

## 함정

- **NTP 필수.** Pico에는 RTC가 없어, 시각 동기화 전에는 모든 wake가 ±60초 검사에서 실패합니다.
  펌웨어는 성공할 때까지 NTP를 재시도하고 이후 6시간마다 다시 맞춥니다.
- **에포크.** Pico의 MicroPython은 2000-01-01부터 시각을 세고 앱은 1970 기준입니다. `clock.py`가
  빌드의 에포크를 자동 감지해 946684800초 차이를 메웁니다. "단순화"한다고 없애지 마세요.
- **DNS.** MicroPython v1.24+에서는 `ifconfig` 튜플만으로 DNS가 설정되지 않아 이름 해석이 실패할 수
  있습니다. `wifi_manager.py`가 `network.ipconfig(dns=…)`로 DNS를 별도 지정합니다(NTP·DuckDNS에 필요).
- **무선 격리 / 게스트 네트워크**는 브로드캐스트를 막습니다 — Pico는 게스트가 아닌 **메인** Wi-Fi에 붙이세요.
- **대상 PC는 유선.** Pico와 같은 서브넷이어야 공유기가 브로드캐스트를 전달합니다. 무선 WOL은 불안정합니다.
- **소켓이 부족합니다.** 요청은 설계상 한 번에 하나씩 처리합니다 — keep-alive나 병렬 처리를 넣지 마세요.

## 라이선스

[MIT](../../LICENSE) — [MagicTap](../../README.md) 프로젝트의 일부.
