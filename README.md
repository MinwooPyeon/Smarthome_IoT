# EEUM 스마트홈 IoT

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [팀원 소개](#팀원-소개)
- [기술 스택](#기술-스택)
- [서비스 기능 소개](#서비스-기능-소개)
- [핵심 기능](#핵심-기능)
- [시스템 아키텍처](#시스템-아키텍처)
- [인프라](#인프라)
- [임베디드](#임베디드)

---

## 프로젝트 개요

IOT 미지원 레거시 가전에 IR 제어를 적용하여 스마트홈 환경을 구축하는 통합 솔루션입니다.
음성 명령, 평면도 기반 디바이스 관리, 루틴 자동화, AI 자동 제어를 하나의 앱에서 제공합니다.

| 항목 | 내용 |
|------|------|
| 개발 기간 | 2025.08.25 ~ 2025.10.02 (6주) |
| 팀원 | 6명 |
| 주요 특징 | 음성 인식(웨이크워드 + STT + NLU), IR 기반 레거시 가전 제어, AI 자동 제어, 평면도 시각화 |

---

## 팀원 소개

| 이름 | 역할 |
|------|------|
| 박주현 | PM, Embedded (IR 수신/온습도), MQTT 설계, MFC 대시보드, AI (LightGBM) |
| 편민우 | Embedded (ESP32 IR 송신, FreeRTOS, NVS, MQTT, WiFi, TLS) |
| 이유민 | Backend (REST API, MQTT 연동, 스케줄러), Infra (EC2, Docker, Jenkins, Nginx) |
| 윤경진 | AI (웨이크워드, STT/NLU, TTS, 대화형 루틴), Backend (MQTT 제어), Android (음성 서비스) |
| 이채영 | Android (평면도, 루틴 CRUD, 네이버 지도), Design (Figma, 로고) |
| 이태훈 | Android (디바이스/사용량/마이페이지 탭, QR 스캔, Vico 차트), Design (Figma) |

---

## 기술 스택

### Android

![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?logo=android&logoColor=white)

### Voice AI

![Picovoice](https://img.shields.io/badge/Picovoice-000000?logoColor=white)
![Android STT](https://img.shields.io/badge/Android%20STT-3DDC84?logo=android&logoColor=white)
![Android TTS](https://img.shields.io/badge/Android%20TTS-3DDC84?logo=android&logoColor=white)
![Regex NLU](https://img.shields.io/badge/Regex%20NLU-FF6F00?logoColor=white)
![Rule-based Grammar](https://img.shields.io/badge/Rule--based%20Grammar-006699?logoColor=white)

### Backend

![SpringBoot](https://img.shields.io/badge/SpringBoot-6DB33F?logo=springboot&logoColor=white)
![MQTT](https://img.shields.io/badge/MQTT-660066?logo=eclipsemosquitto&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?logo=postgresql&logoColor=white)

### Infra

![Nginx](https://img.shields.io/badge/Nginx-009639?logo=nginx&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?logo=docker&logoColor=white)
![Jenkins](https://img.shields.io/badge/Jenkins-D24939?logo=jenkins&logoColor=white)

### Embedded

![C++ STL](https://img.shields.io/badge/C++%20STL-00599C?logo=c%2B%2B&logoColor=white)
![ESP-IDF](https://img.shields.io/badge/ESP--IDF-E7352C?logo=espressif&logoColor=white)
![FreeRTOS](https://img.shields.io/badge/FreeRTOS-0099CC?logo=freertos&logoColor=white)
![NVS Flash](https://img.shields.io/badge/NVS%20Flash-444444?logo=espressif&logoColor=white)
![RMT](https://img.shields.io/badge/RMT-FF8800?logo=espressif&logoColor=white)
![UART](https://img.shields.io/badge/UART-006600?logoColor=white)

### Autopilot AI

![Python](https://img.shields.io/badge/Python-3776AB?logo=python&logoColor=white)
![LightGBM](https://img.shields.io/badge/LightGBM-00C78C?logo=lightgbm&logoColor=white)
![Scikit-learn](https://img.shields.io/badge/Scikit--learn-F7931E?logo=scikit-learn&logoColor=white)
![K-Means](https://img.shields.io/badge/K--Means-FF6F00?logoColor=white)
![PMV/PPD Model](https://img.shields.io/badge/PMV%2FPPD%20Model-006699?logoColor=white)

---

## 서비스 기능 소개

### 평면도

<details open>
  <summary>이미지 펼치기/접기</summary>

  <p float="left" align="center">
    <a href="result/image.png"><img src="result/image.png" width="32%"/></a>
    <a href="result/image%201.png"><img src="result/image%201.png" width="32%"/></a>
    <a href="result/image%202.png"><img src="result/image%202.png" width="32%"/></a>
  </p>
</details>

### 디바이스

<details open>
  <summary>이미지 펼치기/접기</summary>

  <p float="left" align="center">
    <a href="result/image%203.png"><img src="result/image%203.png" width="32%"/></a>
    <a href="result/image%204.png"><img src="result/image%204.png" width="32%"/></a>
  </p>
  <p float="left" align="center">
    <a href="result/image%205.png"><img src="result/image%205.png" width="32%"/></a>
    <a href="result/image%206.png"><img src="result/image%206.png" width="32%"/></a>
  </p>
</details>

### 루틴

<details open>
  <summary>이미지 펼치기/접기</summary>

  <p float="left" align="center">
    <a href="result/image%207.png"><img src="result/image%207.png" width="32%"/></a>
    <a href="result/image%208.png"><img src="result/image%208.png" width="32%"/></a>
    <a href="result/image%209.png"><img src="result/image%209.png" width="32%"/></a>
  </p>
  <p float="left" align="center">
    <a href="result/image%2010.png"><img src="result/image%2010.png" width="32%"/></a>
    <a href="result/image%2011.png"><img src="result/image%2011.png" width="32%"/></a>
    <a href="result/image%2012.png"><img src="result/image%2012.png" width="32%"/></a>
  </p>
  <p float="left" align="center">
    <a href="result/image%2013.png"><img src="result/image%2013.png" width="32%"/></a>
    <a href="result/image%2014.png"><img src="result/image%2014.png" width="32%"/></a>
  </p>
</details>

### 사용량

<details open>
  <summary>이미지 펼치기/접기</summary>

  <p float="left" align="center">
    <a href="result/image%2015.png"><img src="result/image%2015.png" width="32%"/></a>
    <a href="result/image%2016.png"><img src="result/image%2016.png" width="32%"/></a>
  </p>
  <p float="left" align="center">
    <a href="result/image%2017.png"><img src="result/image%2017.png" width="32%"/></a>
    <a href="result/image%2018.png"><img src="result/image%2018.png" width="32%"/></a>
  </p>
</details>

### 음성 인식

- [개별 제어](https://youtube.com/shorts/5yrWLTCyypk)
- [방별 제어](https://youtube.com/shorts/bE8je6z2MmY)
- [전체 제어](https://youtube.com/shorts/iQVL4P4wQwU)
- [상태 질의](https://youtube.com/shorts/u85Gj2mkLp8)
- [루틴 생성](https://youtube.com/shorts/uV-kE1EFaH4)

### 임베디드

<details open>
  <summary>이미지 펼치기/접기</summary>

  <p float="left" align="center">
    <a href="result/image%2019.png"><img src="result/image%2019.png" width="32%"/></a>
  </p>
  <p float="left" align="center">
    <a href="result/image%2020.png"><img src="result/image%2020.png" width="32%"/></a>
  </p>
</details>

---

## 핵심 기능

### 규칙 기반 음성 인식

- Picovoice Porcupine 웨이크워드("제니야") 감지 → Google STT 전환
- `Grammar.yml → RuleCompiler → Regex` 기반 Intent/Slot 매핑 파이프라인
- **31개 의도**, **79만 가지 발화 조합** 지원, TTS 결과 피드백
- 다절 분리(예: "에어컨 켜고 불 꺼") + 컨텍스트 상속 지원
- Android Foreground Service 기반 백그라운드 상주, FCM 딥링크 연동

### 평면도 기반 디바이스 관리

- 실제 평면도 이미지를 DB에 저장하고 방을 색상으로 매핑
- 디바이스 아이콘 위치를 x, y 비율 좌표(0.0~1.0)로 저장 → 화면 크기 무관 정확한 위치 표시
- 드래그 기반 아이콘 배치, 픽셀 색상 샘플링으로 방 자동 매핑

### 루틴 기능

- 요일/시간 기반 가전 예약 제어 (스케줄러: `@Scheduled`, Asia/Seoul 기준 매 분 실행)
- 대화형 루틴 생성 지원 — 이름/설명/요일/시간/동작을 음성으로 단계별 설정
- 루틴 실행 시 FCM 푸시 발송

### 마이크로초 단위 IR 제어

- ESP32 RMT 모듈로 IR 신호를 마이크로초 단위 정밀 제어
- 단순 루프 대비 제어 딜레이 **50μs → 1μs** 수준으로 개선
- 설정은 ESP32 NVS 플래시에 영구 저장

### 보안 아키텍처

- WPA2 기반 Wi-Fi + MQTT TLS 암호화 채널
- 허가된 클라이언트만 브로커 접속 (계정 인증 + ACL 토픽 제한)
- 시리얼 통신 토큰 인증, Rate Limiting (초당 10개 메시지 제한)

### 자동 제어 (AI)

- 온습도 센서 → **WBGT, PMV, PPD, 이슬점, 절대습도** 6가지 열쾌적 지표 산출
- **LightGBM Regressor**로 20분 뒤 PMV 예측 (n_estimators=800, shuffle=False)
- 목적 함수 `J = α·PPD + β·PowerProxy + γ·ΔSetpointPenalty` 최소화로 최적 풍량/세트포인트 추천

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Android App                          │
│    (Jetpack Compose, Voice AI, Naver Map, CameraX)      │
└──────────────────────────┬──────────────────────────────┘
                           │ REST API / MQTT
┌──────────────────────────▼──────────────────────────────┐
│                 Backend (Spring Boot)                   │
│      (Device/Routine API, MQTT Broker, Scheduler)       │
└──────────────────────────┬──────────────────────────────┘
                           │ MQTT (TLS)
┌──────────────────────────▼──────────────────────────────┐
│               RPi Hub (C++, mosquitto)                  │
│    (DHT11, IR Receiver, Analyzer, CSV Logger)           │
└──────────────────────────┬──────────────────────────────┘
                           │ UART / MQTT
┌──────────────────────────▼──────────────────────────────┐
│         ESP32 IR Remote (C++, FreeRTOS)                 │
│       (IR Send/Receive, NVS, WiFi, TLS MQTT)            │
└─────────────────────────────────────────────────────────┘
```

---

## 인프라

| 구성 요소 | 내용 |
|-----------|------|
| EC2 | Docker Compose 멀티 서비스 운영 |
| 네트워크 | 외부: Nginx(80/443), MQTT(8883 TLS) / 내부: Spring Boot, PostgreSQL, Redis (도커 네트워크 격리) |
| CI/CD | Jenkins — Checkout → Build/Test → 이미지 빌드/푸시 → 원격 배포 |
| 시크릿 | `/etc/eeum`, `/etc/secrets/firebase` 서버 내 보호 디렉토리 관리 |
| MQTT | Mosquitto 브로커, 외부 TLS 8883 / 내부 1883, ACL 토픽 범위 제한 |

---

## 임베디드

### ESP32 IR Remote Controller

ESP32-WROOM-32E 기반 IR 송수신, MQTT 통신, 시리얼 제어, WiFi 관리, FreeRTOS 멀티태스킹을 통합 운영합니다.

```
header/
  core/        — Config (NVS 영구 저장), Platform 추상화, Security (TLS/인증)
  hardware/    — IR 송신(RMT), IR 수신(FreeRTOS 큐), 가전 제어, IR 학습
  network/     — MQTT Client (TLS), Serial Controller (JSON 명령어)
src/
  main.cpp     — NVS → 설정 로드 → WiFi → 하드웨어 → FreeRTOS 태스크 생성
```

| 모듈 | 내용 |
|------|------|
| IR 송신 | RMT 채널 1, 38kHz 캐리어, NEC 프로토콜 68개 타이밍 배열 |
| IR 수신 | RMT 채널 0, FreeRTOS 큐 20개, NEC 자동 디코딩 |
| MQTT | PubSubClient, TLS 8883, QoS 1, 자동 재연결 |
| WiFi | WPA2, 연결 실패 시 30초 간격 3회 재시도 |
| 시리얼 | UART 115200bps, JSON 명령어 40종, Rate Limiting |
| FreeRTOS | MQTT(8KB), IR 수신(4KB), PIR(4KB) 태스크, 우선순위 1-5 |
| 설정 | NVS Flash 영구 저장, ArduinoJson 직렬화 |

### RPi Hub (EEUM Hub)

라즈베리파이에서 DHT11 환경 샘플 수집, IR 프레임 캡처, 열쾌적 지표 산출, MQTT 송수신, CSV 비동기 로깅을 운영합니다.

```
actuator/     — DHT11 (pigpio 정밀 타이밍, 재시도/쿨다운), IR 수신 (gap 기반 프레임 분리)
analyzer/     — 이슬점/열지수/절대습도/WBGT/PMV/PPD 계산
manager/      — DataManager (스레드 안전 버퍼), CsvManager (비동기 배치 플러시),
                MqttManager (연결/구독/동적 라우팅), ActuatorManager (센서 루프 독립 스레드)
mqtt/         — mosquitto 래퍼 (TLS, subscribe/publish/loop)
csv/          — CsvMapper/Reader/Writer (헤더 자동 처리, 일자 롤링)
```
