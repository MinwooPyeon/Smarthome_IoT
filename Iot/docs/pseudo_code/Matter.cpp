초기화:
    // 1) Matter 스택 초기화
    Matter Core Library 초기화
    Fabric Config 로드 (Fabric ID, Node ID, Vendor ID 등)
    Commissioning 상태 확인

    // 2) 네트워크 초기화
    Wi-Fi / Ethernet 연결 설정
    Thread 네트워크 스택 초기화 (선택)
    BLE Advertising 활성화 (초기 설정 필요 시)

    // 3) 디바이스 라우팅 구성
    - OnOff Switch 클러스터(전원 제어)
    - Temperature & Humidity 센서 클러스터
    - IR Control 브릿지 클러스터
    - OTA Update 클러스터(선택)

    // 4) Security 초기화
    - CASE(Session) 핸드셰이크 준비
    - DAC 인증서 로드
    - PASE(Password Authenticated Session Establishment) 활성화
    - ACL(Access Control List) 로드

    // 5) MQTT 브릿지 설정(선택)
    - Broker 주소, Port, Topic 구조 설정
    - Matter <-> MQTT 변환 테이블 구성
    예) "matter/device/0x1234/state" <-> "mqtt/device/1/state"

    // 6) 이벤트 큐 초기화
    Matter Event Queue 준비
    Sensor Data Queue 준비
    Control Command Queue 준비

    Matter Stack Start()


메인 루프:
    현재시간 = millis()

    // 1) Matter 네트워크 상태 관리
    if (네트워크 연결 안 됨):
        재연결 시도
        다음 루프로 이동

    // 2) Commissioning 모드 처리
    if (새로운 디바이스 페어링 요청 감지):
        start_commissioning()
        if (성공):
            디바이스 Fabric에 추가
            ACL 업데이트
        else:
            에러 로그 후 무시

    // 3) 센서 데이터 수집 및 Matter 특성 업데이트
    if (DHT11 새 데이터 도착):
        temp, hum = 센서 값 읽기
        update_matter_attribute(cluster=Temperature, value=temp)
        update_matter_attribute(cluster=Humidity, value=hum)

    if (PIR motion 감지됨):
        update_matter_attribute(cluster=Occupancy, value=1)
    else:
        update_matter_attribute(cluster=Occupancy, value=0)

    // 4) Matter Command 수신 처리
    while (Matter Event Queue 비어있지 않음):
        evt = Matter Event Queue.pop()
        if (evt.type == "OnOffCommand"):
            if (evt.value == ON):
                장치 전원 켜기
            else:
                장치 전원 끄기
        else if (evt.type == "IRSendCommand"):
            IR 송신 명령 = evt.payload
            IR LED로 S346 송신 실행
        else if (evt.type == "OTAUpdate"):
            OTA 펌웨어 업데이트 진행

    // 5) MQTT 브릿지 동기화 (선택)
    while (MQTT 구독 메시지 있음):
        msg = MQTT 메시지 pop()
        변환 = MQTT → Matter Command 매핑
        Matter Command Queue.push(변환)

    // 6) OTA 업데이트 체크 (선택)
    if (현재시간 - 마지막펌웨어체크시각 >= OTA_CHECK_INTERVAL):
        check_for_ota_update()
        마지막펌웨어체크시각 = 현재시간

    // 7) Keepalive & Health Check
    if (현재시간 - 마지막Ping >= KEEPALIVE_INTERVAL):
        Matter Session HealthCheck()
        if (세션끊김):
            세션 재설정
        마지막Ping = 현재시간

    // 8) 이벤트 대기 및 속도 제어
    sleep(짧은 주기)

Matter 통신 흐름 요약
단계	기능	설명
1	Commissioning	QR 코드, BLE 또는 IP를 통한 최초 설정
2	Fabric Join	네트워크에 디바이스를 등록 (Node ID 발급)
3	Secure Session	PASE / CASE 기반 암호화 세션 수립
4	Cluster Control	OnOff, Level, Temp/Humidity, IR 등 개별 클러스터 명령
5	Attribute Report	센서 값이나 상태를 Matter 네트워크로 브로드캐스트
6	Bridging	MQTT, Zigbee, Z-Wave 등 외부 프로토콜 연동 가능
7	OTA Update	펌웨어 업데이트 클러스터 지원 가능