초기화:
    IR_PIN 설정 (입력 모드)
    LED_PIN 설정 (출력 모드, 선택)
    타이머 준비 (micros() 사용 가정)

    // 수신 버퍼/상태
    수신버퍼[최대펄스개수] = 비움
    펄스수 = 0
    수집중 = false
    프레임준비됨 = false

    이전 레벨 = IR_PIN 현재 값 (HIGH/LOW)
    마지막엣지시각_us = micros()
    디코드결과 = 없음
    반복신호여부 = false

    // 타이밍 파라미터 (NEC 예시)
    FRAME_GAP_US = 10000            // 이 시간 이상 무펄스면 프레임 종료
    TOL = 0.3                        // 허용 오차(30%) 
    NEC_리드LOW = 9000, NEC_리드HIGH = 4500
    NEC_BIT_LOW = 560, NEC_0_HIGH = 560, NEC_1_HIGH = 1690
    NEC_REPEAT_HIGH = 2250           // Repeat 프레임 식별용(예: 2.25ms)

메인 루프:
    현재시간_us = micros()

    // 1) 엣지(상승/하강) 검출
    현재 레벨 = IR_PIN 값 (HIGH 또는 LOW)
    if (현재 레벨 != 이전 레벨):
        펄스길이_us = 현재시간_us - 마지막엣지시각_us
        마지막엣지시각_us = 현재시간_us
        이전 레벨 = 현재 레벨

        // 1-1) 프레임 경계 판단
        if (펄스길이_us > FRAME_GAP_US):
            if (수집중 && 펄스수 > 0):
                프레임준비됨 = true        // 이전 프레임 수집 완료
            수집중 = true
            펄스수 = 0
        else:
            // 1-2) 펄스 기록
            if (수집중):
                수신버퍼[펄스수] = 펄스길이_us
                펄스수 += 1

    // 2) 무엣지 상태가 오래 지속되면 프레임 종료로 간주 (보조 안전장치)
    if (수집중 && (현재시간_us - 마지막엣지시각_us > FRAME_GAP_US) && (펄스수 > 0)):
        프레임준비됨 = true

    // 3) 프레임 디코딩 (NEC 예시)
    if (프레임준비됨):
        프레임준비됨 = false

        // 3-1) 리드(시작) 패턴 검증: LOW≈9ms, HIGH≈4.5ms
        lead_low = 수신버퍼[0]
        lead_high = 수신버퍼[1]
        리드정상 = in_range(lead_low, NEC_리드LOW, TOL) && in_range(lead_high, NEC_리드HIGH, TOL)

        if (!리드정상):
            // Repeat 프레임인지 확인 (LOW≈9ms, HIGH≈2.25ms 등)
            repeat_가능 = in_range(lead_low, NEC_리드LOW, TOL) && in_range(lead_high, NEC_REPEAT_HIGH, TOL)
            if (repeat_가능):
                반복신호여부 = true
                onIRRepeat()                 // 필요 시 반복 처리 콜백
            else:
                // 알 수 없는 프레임 → 버림
                수집중 = false
                펄스수 = 0
            다음 루프로 이동

        // 3-2) 비트 파싱
        비트값 = 0 (32bit 컨테이너)
        비트인덱스 = 0
        i = 2    // 리드 이후부터 시작 (LOW/HIGH가 교대로 온다고 가정)

        while (i + 1 < 펄스수 && 비트인덱스 < 32):
            bit_low  = 수신버퍼[i + 0]
            bit_high = 수신버퍼[i + 1]

            // 각 비트는 LOW≈560us + (HIGH≈560us -> '0') 또는 (HIGH≈1690us -> '1')
            bit_low_ok = in_range(bit_low, NEC_BIT_LOW, TOL)
            if (!bit_low_ok):
                // 비트 구조 불량 → 프레임 폐기
                break

            if (in_range(bit_high, NEC_0_HIGH, TOL)):
                // 비트 0
                비트값 = (비트값 << 1) | 0
            else if (in_range(bit_high, NEC_1_HIGH, TOL)):
                // 비트 1
                비트값 = (비트값 << 1) | 1
            else:
                // HIGH 길이 불일치 → 프레임 폐기
                break

            비트인덱스 += 1
            i += 2

        // 3-3) 32비트 수신 검증 및 바이트단위 해석
        if (비트인덱스 == 32):
            addr      = (비트값 >> 24) & 0xFF
            naddr     = (비트값 >> 16) & 0xFF
            cmd       = (비트값 >>  8) & 0xFF
            ncmd      = (비트값 >>  0) & 0xFF

            // 보수 검증 (NEC: naddr == ~addr, ncmd == ~cmd)
            if ((naddr == (~addr & 0xFF)) && (ncmd == (~cmd & 0xFF))):
                디코드결과 = cmd
                반복신호여부 = false
                onIRCommand(디코드결과)      // 사용자 명령 처리
                // (선택) LED 점멸 등 피드백
            else:
                // 체크 불일치 → 폐기
                pass
        else:
            // 비트 수 부족/파싱 실패 → 폐기
            pass

        // 3-4) 다음 프레임 대비 초기화
        수집중 = false
        펄스수 = 0

    // 4) 상태 표시 (선택)
    // 유효한 명령 수신 시 LED 토글/점멸 등
