초기화:
    BROKER_ADDR, PORT, CLIENT_ID_PUB 설정
    USERNAME/PASSWORD (선택)
    KEEPALIVE_SEC, CLEAN_SESSION, LWT(선택) 설정
    기본 QoS = 1, RETAIN 기본값 = false
    PUBLISH_TOPIC(s) 정의 (예: "sensors/dht11", "events/pir", ...)

    송신큐 초기화 (thread-safe queue)
    연결상태 = DISCONNECTED
    백오프 = 초기값(예: 1초), 최대백오프 = 32초
    마지막Ping시각 = 0
    마지막Conn시도시각 = 0

메인 루프:
    현재시각 = millis()

    // 1) 연결 상태 관리
    if (연결상태 == DISCONNECTED):
        if (현재시각 - 마지막Conn시도시각 >= 백오프):
            마지막Conn시도시각 = 현재시각
            connect(BROKER_ADDR, PORT, CLIENT_ID_PUB, KEEPALIVE_SEC, CLEAN_SESSION, USERNAME/PASSWORD, LWT)
            if (연결성공):
                연결상태 = CONNECTED
                백오프 = 초기값으로 리셋
            else:
                백오프 = min(백오프 * 2, 최대백오프)
        다음 루프로 이동

    // 2) keepalive ping (헬스 체크)
    if (현재시각 - 마지막Ping시각 >= KEEPALIVE_SEC * 1000 * 0.5):
        pingreq()
        if (pingresp 타임아웃):
            disconnect()
            연결상태 = DISCONNECTED
            다음 루프로 이동
        마지막Ping시각 = 현재시각

    // 3) 송신큐에서 메시지 꺼내기
    if (송신큐 비어있지 않음):
        msg = 송신큐.pop()
        // msg: {topic, payload(bytes), qos, retain, props(선택)}
        publish(msg.topic, msg.payload, msg.qos or 기본QoS, msg.retain or RETAIN 기본값)

        // 3-1) QoS 처리
        if (msg.qos == 1):
            if (PUBACK 타임아웃):
                // 재전송 정책
                재전송 또는 실패 표기
        else if (msg.qos == 2):
            // PUBREC -> PUBREL -> PUBCOMP 왕복
            // 단계별 타임아웃/재전송 처리

    // 4) 브로커 알림 처리(이벤트 루프)
    // 예: DISCONNECT 통지, 네트워크 오류
    if (네트워크에러 발생):
        disconnect()
        연결상태 = DISCONNECTED

    // 5) 속도 조절
    sleep(짧은 주기)
