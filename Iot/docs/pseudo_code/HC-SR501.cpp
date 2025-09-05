초기화:
    PIR_PIN 설정 (입력 모드)
    LED_PIN 설정 (출력 모드, 선택)
    전원 켠 시각 기록
    warmedUp = false

메인 루프:
    현재 시간 = millis()

    // 1) 센서 워밍업 체크
    if (현재 시간 - 전원 켠 시각 >= WARMUP_TIME):
        warmedUp = true
    else:
        warmedUp = false

    if (warmedUp == false):
        워밍업 중이므로 아무 작업도 하지 않음
        다음 루프로 이동

    // 2) PIR 센서 상태 읽기
    현재 상태 = PIR_PIN 값 (HIGH 또는 LOW)

    // 3) 디바운스 처리
    if (현재 상태 != 이전 상태):
        if (현재 시간 - 마지막 상태변경 시간 >= DEBOUNCE_TIME):
            마지막 상태변경 시간 = 현재 시간
            이전 상태 = 현재 상태

            // 4) 움직임 감지 이벤트 처리
            if (현재 상태 == HIGH):
                if (현재 시간 - 마지막 이벤트 시간 >= COOLDOWN_TIME):
                    마지막 이벤트 시간 = 현재 시간
                    "Motion Detected" 처리 수행 (LED 켜기, 로그, 알림 등)

    // 5) 상태 표시 (선택)
    if (현재 상태 == HIGH):
        LED 켜기
    else:
        LED 끄기
