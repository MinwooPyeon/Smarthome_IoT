초기화:
    IR_TX_PIN 설정 (출력 모드)
    LED_PIN 설정 (출력 모드, 선택)
    고주파 캐리어 타이머 준비 (예: 38kHz, 듀티 1/3~1/2)
    송신 큐/버퍼 초기화
    마지막 전송 시각 = 0

    // NEC 타이밍 파라미터 (오차 허용치 TOL은 소프트로 보정)
    CARRIER_HZ = 38000
    MARK_US    = 560          // 각 비트의 'MARK' 길이
    SPACE_0_US = 560          // 비트 0의 'SPACE'
    SPACE_1_US = 1690         // 비트 1의 'SPACE'
    LEAD_LOW_US  = 9000       // 리드 MARK
    LEAD_HIGH_US = 4500       // 리드 SPACE
    REPEAT_HIGH_US = 2250     // 리피트 SPACE
    TRAIL_MARK_US = 560       // 프레임 끝 MARK
    INTER_FRAME_GAP_US = 40000// 프레임 간 최소 간격(예: 40ms)
    MAX_RETRY = 3

메인 루프:
    현재시간_us = micros()

    // 1) 송신 요청 확인 (큐 또는 상태 플래그)
    if (송신요청 없음):
        다음 루프로 이동

    // 2) 프레임 구성 (NEC: 32비트 = Addr(8) + ~Addr(8) + Cmd(8) + ~Cmd(8))
    address = 요청.address
    command = 요청.command
    frame32 = (address << 24) | ((~address & 0xFF) << 16) | (command << 8) | (~command & 0xFF)

    // 3) 프레임 간 간격 보장
    if (현재시간_us - 마지막 전송 시각 < INTER_FRAME_GAP_US):
        // 남은 시간만큼 대기
        wait_us(INTER_FRAME_GAP_US - (현재시간_us - 마지막 전송 시각))

    // 4) 리드 신호 전송 (MARK + SPACE)
    carrier_on(IR_TX_PIN)       // 38kHz 시작
    wait_us(LEAD_LOW_US)        // 9ms MARK
    carrier_off(IR_TX_PIN)      // 캐리어 중단
    wait_us(LEAD_HIGH_US)       // 4.5ms SPACE

    // 5) 32비트 데이터 전송 (MSB → LSB)
    for (i = 31 downTo 0):
        bit = (frame32 >> i) & 1

        // 공통 MARK
        carrier_on(IR_TX_PIN)
        wait_us(MARK_US)        // 560us MARK
        carrier_off(IR_TX_PIN)

        // 비트에 따른 SPACE 길이
        if (bit == 0):
            wait_us(SPACE_0_US) // 560us SPACE
        else:
            wait_us(SPACE_1_US) // 1690us SPACE

    // 6) 트레일 MARK (수신기가 프레임 종료 인지)
    carrier_on(IR_TX_PIN)
    wait_us(TRAIL_MARK_US)      // 560us MARK
    carrier_off(IR_TX_PIN)

    // 7) 전송 성공 처리
    마지막 전송 시각 = micros()
    요청 상태 = "sent"

    // 8) (선택) 반복 프레임 처리: 버튼 유지 시 주기적으로 Repeat 프레임 송신
    if (요청이 '버튼 유지' 타입이라면):
        while (버튼 유지 조건이 참인 동안):
            // 프레임 간 간격 보장
            if (micros() - 마지막 전송 시각 < INTER_FRAME_GAP_US):
                wait_us(INTER_FRAME_GAP_US - (micros() - 마지막 전송 시각))

            // Repeat 프레임 (리드 MARK + Repeat SPACE + 짧은 MARK)
            carrier_on(IR_TX_PIN)
            wait_us(LEAD_LOW_US)        // 9ms MARK
            carrier_off(IR_TX_PIN)
            wait_us(REPEAT_HIGH_US)     // 2.25ms SPACE
            carrier_on(IR_TX_PIN)
            wait_us(MARK_US)            // 560us MARK
            carrier_off(IR_TX_PIN)

            마지막 전송 시각 = micros()

    // 9) (선택) 재전송/에러 처리
    if (수신 ACK 개념이 있다면):
        if (ACK 미수신):
            재시도 횟수 += 1
            if (재시도 횟수 <= MAX_RETRY):
                // 위 3)~6) 재수행
                goto "리드 신호 전송 단계"
            else:
                요청 상태 = "failed"

    // 10) 상태 표시 (선택)
    // 전송 중 LED 점멸 등
    blink(LED_PIN)
