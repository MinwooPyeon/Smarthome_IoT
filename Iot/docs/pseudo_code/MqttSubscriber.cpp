초기화:
    BROKER_ADDR, PORT, CLIENT_ID_SUB 설정
    USERNAME/PASSWORD (선택)
    KEEPALIVE_SEC, CLEAN_SESSION 설정
    구독 토픽 목록 정의:
        - 예) TOPICS = [
            {"topic":"control/ir/#", "qos":1},
            {"topic":"control/reboot", "qos":1},
            {"topic":"config/+/update", "qos":1}
          ]

    메시지 처리 큐 초기화 (thread-safe queue)
    연결상태 = DISCONNECTED
    백오프 = 초기값(1초), 최대백오프 = 32초
    마지막Ping시각 = 0
    마지막Conn시도시각 = 0

메인 루프:
    현재시각 = millis()

    // 1) 연결 상태 관리
    if (연결상태 == DISCONNECTED):
        if (현재시각 - 마지막Conn시도시각 >= 백오프):
            마지막Conn시도시각 = 현재시각
            connect(BROKER_ADDR, PORT, CLIENT_ID_SUB, KEEPALIVE_SEC, CLEAN_SESSION, USERNAME/PASSWORD)
            if (연결성공):
                // 1-1) 구독 재설정 (Clean Session 또는 세션 만료 상황 대비)
                for each t in TOPICS:
                    subscribe(t.topic, t.qos)
                연결상태 = CONNECTED
                백오프 = 초기값으로 리셋
            else:
                백오프 = min(백오프 * 2, 최대백오프)
        다음 루프로 이동

    // 2) keepalive ping
    if (현재시각 - 마지막Ping시각 >= KEEPALIVE_SEC * 1000 * 0.5):
        pingreq()
        if (pingresp 타임아웃):
            disconnect()
            연결상태 = DISCONNECTED
            다음 루프로 이동
        마지막Ping시각 = 현재시각

    // 3) 수신 이벤트 폴/디스패치
    incoming = poll_network_events()  // 블로킹 없이 도착 메시지 확인
    for each evt in incoming:
        if (evt.type == MESSAGE):
            // evt: {topic, payload(bytes), qos, retain, dup}
            // 3-1) 즉시 처리 or 큐잉
            메시지처리큐.push(evt)

        else if (evt.type == SUBACK 실패/네트워크에러 등):
            // 재시도 또는 로그
            handle_error(evt)

    // 4) 메시지 처리(업무 로직)
    while (메시지처리큐 비어있지 않음):
        msg = 메시지처리큐.pop()
        // 토픽 라우팅
        if (match(msg.topic, "control/ir/send")):
            // payload 파싱(JSON 등) → IR 송신 실행(S346 모듈)
            handle_ir_send(msg.payload)

        else if (match(msg.topic, "control/reboot")):
            handle_reboot_command()

        else if (match(msg.topic, "config/+/update")):
            handle_config_update(msg.topic, msg.payload)

        // QoS 1/2면 적절한 ACK 왕복은 네트워크 이벤트 루프가 처리

    // 5) 에러/끊김 대응
    if (네트워크에러 또는 서버 DISCONNECT 감지):
        disconnect()
        연결상태 = DISCONNECTED

    // 6) 속도 조절
    sleep(짧은 주기)
