초기화:
    // ───────── 시스템/네트워크 ─────────
    NVS 초기화
    Wi-Fi STA 설정(SSID/PASS) 및 자동 재연결 정책 준비
    NTP 동기화(선택) → 타임스탬프 용

    // ───────── MQTT ─────────
    MQTT 설정:
        BROKER_ADDR, PORT, CLIENT_ID, KEEPALIVE
        USERNAME/PASSWORD(선택), CLEAN_SESSION
        LWT: topic="device/status", payload="offline", retain=true
    MQTT 연결 상태 = DISCONNECTED, 백오프 초기화(예: 1s, max 32s)
    구독 토픽:
        control/ir/send          // {"protocol":"NEC","addr":0x10,"cmd":0xAF,"hold_ms":500}
        control/matter         // (선택) 디버그/우회 제어
        config                 // 런타임 파라미터 조정

    // ───────── Matter (ESP-Matter) ─────────
    Matter Core 초기화
    Fabric/Node/ACL 로드
    노드에 클러스터 바인딩:
        - IRControl(가상 장치) : {IRSendCommand(protocol, addr, cmd, hold_ms)}
        - (선택) OnOff/Generic : IR 매핑 대상으로 사용
    Commissioning 가능 상태(BLE on-network, QR 등) 준비
    Matter 이벤트 큐 준비

    // ───────── IR Sender (S346 등 IR LED) ─────────
    IR_TX_PIN 출력 설정
    38kHz 캐리어 생성기 준비:
        - RMT 채널 또는 LEDC PWM 채널 구성 (duty ≈ 33~50%)
        - carrier_on()/carrier_off() 추상화
    NEC 송신 타이밍 상수:
        LEAD_LOW=9000us, LEAD_HIGH=4500us
        MARK=560us, SPACE0=560us, SPACE1=1690us
        REPEAT_HIGH=2250us, TRAIL_MARK=560us
        INTER_GAP=40000us (프레임 간 최소 간격)

    // ───────── 큐/상태 ─────────
    IRSendQueue = 빈 큐         // Matter/MQTT → IR 송신 작업
    MqttRxQueue = 빈 큐
    MqttTxQueue = 빈 큐
    MatterEvtQueue = 빈 큐

    마지막_헬스_ms = millis()
    HEALTH_INTERVAL_MS = 15000

    // ───────── 매핑 정책 ─────────
    Matter→IR 매핑 테이블 예:
        "OnOff:On"  → {protocol:"NEC", addr:0x10, cmd:0x01}
        "OnOff:Off" → {protocol:"NEC", addr:0x10, cmd:0x02}

    // ───────── 시작 알림 ─────────
    Wi-Fi 연결 시도 시작

메인 루프:
    now_ms = millis()

    // 1) Wi-Fi 연결 관리
    if (Wi-Fi 미연결):
        재연결 시도(백오프/이벤트 기반)
        다음 루프로 이동

    // 2) MQTT 연결/유지
    if (MQTT DISCONNECTED):
        백오프 경과 시 connect()
        성공 시:
            subscribe(control/ir/send, QoS1)
            subscribe(control/matter/#, QoS1)
            subscribe(config/#, QoS1)
            publish("device/status", {"state":"online","uptime_ms":now_ms}, qos=0, retain=true)
        실패 시:
            백오프 증가 후 다음 루프
    else:
        mqtt.loopOnce()                // keepalive/ACK/이벤트 펌프
        while (mqtt.tryPopMessage(out msg)):
            MqttRxQueue.push(msg)

    // 3) Matter 이벤트 펌프
    matter.loopOnce()
    while (matter.tryPopEvent(out evt)):
        MatterEvtQueue.push(evt)

    // 4) MQTT 수신 라우팅 → IR 송신 작업
    while (!MqttRxQueue.empty()):
        msg = MqttRxQueue.pop()
        if (match(msg.topic, "control/ir/send")):
            job = parse_ir_job(msg.payload)        // {protocol, addr, cmd, hold_ms}
            if (job.valid): IRSendQueue.push(job)
        else if (match(msg.topic, "control/matter/*")):
            mevt = mapMqttToMatter(msg)            // 선택: 테스트/브릿지
            if (mevt.valid): MatterEvtQueue.push(mevt)
        else if (match(msg.topic, "config/*")):
            apply_config(msg)                      // duty, gap, report, log level 등

    // 5) Matter 명령 처리 → IR 송신 작업
    while (!MatterEvtQueue.empty()):
        evt = MatterEvtQueue.pop()
        if (evt.type == "ClusterCommand"):
            if (evt.cluster == "IRControl"):
                // payload: {protocol, addr, cmd, hold_ms}
                IRSendQueue.push( evt.payload.asIRJob() )
            else if (evt.cluster == "OnOff"):
                key = "OnOff:" + evt.payload["op"] // "On" | "Off"
                if (MatterToIRMap.contains(key)):
                    IRSendQueue.push( MatterToIRMap[key] )
        else if (evt.type == "Commissioning"):
            handle_commissioning(evt)              // Fabric/ACL 갱신 등

    // 6) IR 송신 실행(NEC 예시)
    if (!IRSendQueue.empty()):
        job = IRSendQueue.pop()                    // {protocol, addr, cmd, hold_ms}
        if (job.protocol == "NEC"):
            ensure_inter_frame_gap(INTER_GAP)

            // 리드(MARK + SPACE)
            carrier_on();  wait_us(LEAD_LOW);      // 9ms MARK
            carrier_off(); wait_us(LEAD_HIGH);     // 4.5ms SPACE

            // 32비트 프레임: addr, ~addr, cmd, ~cmd (MSB→LSB)
            frame32 = (job.addr<<24) | ((~job.addr&0xFF)<<16)
                    | (job.cmd<<8)  | (~job.cmd&0xFF)

            for i in 31..0:
                bit = (frame32>>i)&1
                carrier_on();  wait_us(MARK);      // 공통 MARK
                carrier_off();
                if (bit==0) wait_us(SPACE0) else wait_us(SPACE1)

            // 트레일 MARK
            carrier_on();  wait_us(TRAIL_MARK); carrier_off()

            // (선택) 버튼 유지: Repeat 프레임 반복
            remain_ms = job.hold_ms
            while (remain_ms > 0):
                ensure_inter_frame_gap(INTER_GAP)
                carrier_on();  wait_us(LEAD_LOW);  carrier_off()
                wait_us(REPEAT_HIGH)               // 2.25ms SPACE
                carrier_on();  wait_us(MARK);      carrier_off()
                remain_ms -= estimated_repeat_period_ms

            // 피드백
            MqttTxQueue.push({
                topic: "events/ir/sent",
                payload: {"protocol":"NEC","addr":job.addr,"cmd":job.cmd,"ts":now_ms},
                qos:0, retain:false
            })
            matter.reportAttribute("IRControl.LastCmd", {"addr":job.addr,"cmd":job.cmd})

    // 7) MQTT 송신 큐 플러시
    while (mqtt.isConnected() && !MqttTxQueue.empty()):
        out = MqttTxQueue.pop()
        mqtt.publish(out.topic, json(out.payload), qos=out.qos, retain=out.retain)

    // 8) 헬스/상태 리포트
    if (now_ms - 마지막_헬스_ms >= HEALTH_INTERVAL_MS):
        mqtt.publish("device/status", {"state":"online","uptime_ms":now_ms}, qos=0, retain=true)
        matter_health_check()
        마지막_헬스_ms = now_ms

    // 9) CPU 양보
    delay(짧은 주기)
