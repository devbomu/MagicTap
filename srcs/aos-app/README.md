# MagicTap 안드로이드 앱

Kotlin · Jetpack Compose · Glance 위젯. [MagicTap](../../README.md)의 휴대폰 쪽 구성 요소로,
프로필·PC 관리, 서명된 wake 전송, 홈 화면 위젯 2종을 담당합니다.

## 요구 사항

- Android Studio(Ladybug 이상) + **JDK 17 이상**
- Android SDK Platform **36**
- **Android 12(API 31)** 이상 기기/에뮬레이터

## 빌드 & 실행

`srcs/aos-app`를 Android Studio에서 열어 Run 하거나, 커맨드라인에서:

```bash
./gradlew :app:installDebug
```

> **최초 체크아웃 시:** `gradle/wrapper/gradle-wrapper.jar`는 의도적으로 커밋하지 않습니다.
> Android Studio로 열면 자동 생성됩니다. CLI만 쓰는 경우 시스템 Gradle로 한 번 생성하세요:
>
> ```bash
> gradle wrapper --gradle-version 8.11.1
> ```

주요 설정([`app/build.gradle.kts`](app/build.gradle.kts)): `applicationId = "com.magictap"`,
`minSdk 31`, `targetSdk 36`, `versionName 1.0.0`. 권한은 `INTERNET`과 `ACCESS_WIFI_STATE`(아래
config 자동 생성용)뿐이며, 클라우드 백업은 꺼져 있습니다(`allowBackup=false`).

## 최초 사용

1. **프로필 추가**(집 / Pico W 1대): 별칭, 내부 LAN 주소, 외부 DDNS 주소 + 포트. 32바이트 HMAC
   비밀키가 자동 생성됩니다 — 이 값을 **Pico W의 `config.json`에 넣어야** 합니다.
2. **연결 테스트**(프로필 편집 화면)로 내부·외부 두 경로를 각각 확인합니다. 도달 여부(`/ping`)뿐
   아니라 **비밀키 일치 여부(`/verify`)까지** 검사하므로, 설정 실수를 가장 빠르게 잡아냅니다.
3. **PC 추가**는 MAC 주소로(구분자 자유 — `AA:BB:CC:DD:EE:FF`로 정규화됨).
4. 앱에서 **켜기**를 누르거나, 홈 화면에 위젯을 배치합니다.

### Pico config.json 자동 생성

프로필 편집 화면의 **Pico 설정 생성**은 휴대폰의 현재 Wi-Fi·네트워크 기본값과 프로필 비밀키를
채워 바로 업로드 가능한 `config.json`을 만들고, 시스템 파일 선택기로 내보냅니다. 비밀키는
프로필에서 그대로 실려 갈 뿐, 이 화면에서 입력하거나 노출하지 않습니다. (업로드·재부팅은 수동,
검증은 위 연결 테스트로.)

## 위젯

- **목록형 위젯** — 배치 시 프로필 1개를 선택. 그 프로필의 PC들을 세로로 보여주고, 각 행이 곧 켜기 버튼.
- **단일 아이콘형 위젯** — 프로필 → PC 순으로 선택. 한 번 누르면 그 PC를 켬.

위젯을 누르면 앱 본체가 아니라 작은 반투명 확인 창(`ConfirmActivity`)이 떠서, 실수로 눌러도
wake가 나가지 않습니다. 위젯 탭은 사용자 상호작용이라 Android 12+ 백그라운드 시작 제한에서
면제되므로 앱을 띄우지 않고도 동작합니다(기획서 §7.3).

## 아키텍처

단일 JSON 문서를 Android Keystore의 AES-256-GCM 키로 암호화해 저장합니다(deprecated된
`security-crypto` 라이브러리는 의도적으로 사용하지 않음, 기획서 §8). DB 없음, 내 Pico W 외의
네트워크 호출 없음.

```
com.magictap
├─ MagicTapApplication / AppContainer   수동 DI (repository, store, client)
├─ MainActivity                         Compose 호스트 + 내비게이션 (enableEdgeToEdge)
├─ data
│  ├─ model            Profile / Pc / AppData (kotlinx.serialization)
│  ├─ crypto           KeystoreManager(at-rest), BackupCrypto(PBKDF2 내보내기/가져오기)
│  ├─ store            SecureStore(암호화 파일)
│  └─ WolRepository    StateFlow 단일 진실 원천
├─ net
│  ├─ HmacSigner       HMAC-SHA256(secret, "mac|ts")
│  ├─ MacUtils         MAC 정규화/검증
│  └─ WolClient        OkHttp; 내부→외부 폴백(§5.3), /ping·/wake·/verify
├─ ui
│  ├─ main / profile / pc / settings    화면 + 뷰모델
│  ├─ pico             config.json 생성기(PicoConfig, PicoConfigDialog)
│  └─ components / theme
└─ widget
   ├─ ListWidget / SingleWidget         Glance 위젯 + 리시버
   ├─ WidgetTheme                       위젯 브랜드 색상(고정 밝은 톤, 흰 배경)
   ├─ *ConfigActivity                   배치 시 구성
   └─ ConfirmActivity                   반투명 확인 + 전송
```

### 내부/외부 자동 전환(`WolClient`)

내부 주소로 먼저 `/ping`(300ms)을 던져 집 LAN 여부를 판단합니다. 응답하면 내부로 wake,
아니면 외부 DDNS 주소로 폴백(5s)합니다. 집 Wi-Fi에서 NAT 헤어핀이 없으면 외부 주소가 실패하기
때문에 내부를 항상 먼저 시도합니다. HTTP 시도마다 타임스탬프·서명을 새로 만들므로 내부→외부
폴백이 Pico W의 재전송 필터에 걸리지 않습니다.

> 연결 테스트의 `/verify`는 내부·외부를 병렬로 시도하는데, MAC 필드에 매번 임의 nonce를 넣어
> 두 요청의 서명이 같아져 재전송 필터에 걸리는(=거짓 "비밀키 불일치") 일을 방지합니다.

## 내보내기 / 가져오기

설정 → 내보내기는 단일 JSON 파일을 씁니다. 기본은 **암호화**(PBKDF2-HMAC-SHA256 20만 회 →
AES-256-GCM)이고, **비밀키 제외** 모드는 구조만 백업합니다. 가져오기는 암호화 여부를 감지해
암호를 묻고, 병합/덮어쓰기를 고르게 합니다.

## 라이선스

[MIT](../../LICENSE) — [MagicTap](../../README.md) 프로젝트의 일부.
