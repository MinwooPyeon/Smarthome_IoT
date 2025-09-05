0) 모듈 인터페이스 가정(예시 시그니처)
interface IDht11 {
    bool ready();                                   // 최소 주기 충족 여부
    bool read(out float tempC, out float hum);      // 성공 시 true
    uint32 minIntervalMs();                         // 보통 2000ms
}

interface IIRReceiver { // VS1813
    // 엣지/펄스 수집은 모듈 내부. 프레임 완성 시 이벤트/폴링으로 전달
    bool tryPopFrame(out IRFrame frame);            // non-blocking
}

struct IRFrame {
    string protocol;                                // "NEC", ...
    bool   isRepeat;
    uint8  addr;                                    // 프로토콜별 필드
    uint8  cmd;
    uint64 tsMs;
}

interface IIRTransmitter { // S346
    bool sendNEC(uint8 addr, uint8 cmd, uint32 holdMs=0); // 프레임 간 간격 등 내부 보장
}

interface IMqttClient {
    bool connect();
    bool isConnected();
    void publish(string topic, string payload, int qos=1, bool retain=false);
    // 수신은 폴링 또는 콜백. 여기선 폴링 가정:
    bool tryPopMessage(out MqttMsg msg);            // non-blocking
    void subscribe(string topic, int qos=1);
    void loopOnce();                                 // keepalive/네트워크 이벤트 처리
}

struct MqttMsg {
    string topic;
    string payload;
    int    qos;
    bool   retain;
}

interface IMatterStack {
    bool  init();
    void  loopOnce();                                // 세션/이벤트 펌프
    void  reportAttribute(string cluster, any value);// ex) ("Temperature", 24.3)
    bool  tryPopEvent(out MatterEvt evt);            // 명령/커미셔닝 이벤트 등
}

struct MatterEvt {
    string type;                                     // "ClusterCommand", "Commission", ...
    string cluster;                                  // "IRControl", "OnOff", "Temperature", ...
    map<string, any> payload;                        // { protocol, addr, cmd, hold_ms, ... }
}

// 유틸: thread-safe queue(또는 lock-free 큐)를 가정
interface IQueue<T> {
    void push(T item);
    bool tryPop(out T item);                         // non-blocking
}

1) 오케스트레이터 전역 리소스
전역:
    // 모듈 인스턴스 (DI 또는 팩토리 주입)
    IDht11        dht11
    IIRReceiver   irRx
    IIRTransmitter irTx
    IMqttClient   mqtt
    IMatterStack  matter

    // 큐
    IQueue<IRFrame>   irRecvQueue
    IQueue<MqttMsg>   mqttRxQueue
    IQueue<MqttMsg>   mqttTxQueue
    IQueue<MatterEvt> matterEvtQueue
    IQueue<IRFrame>   irSendQueueLogical // Matter/MQTT→IR로 변환된 송신 작업

    // 상태/타이머
    uint64 lastDhtMs = 0
    uint64 nowMs = 0
    uint64 healthMs = 0
    const  uint32 HEALTH_INTERVAL_MS = 15000

    // 토픽 규약
    const string T_SENS_DHT    = "sensors/dht11"
    const string T_EVT_IR_RX   = "events/ir/received"
    const string T_EVT_IR_TX   = "events/ir/sent"
    const string T_DEV_STATUS  = "device/status"
    const string T_CTRL_IR_TX  = "control/ir/send"
    const string T_CTRL_MATTER = "control/matter/#"    // 예: 테스트/우회 제어용
    const string T_CONFIG_ALL  = "config/#"

    // 매핑(예시): IR <-> Matter 논리 디바이스
    Map<(string proto,uint8 addr,uint8 cmd), MatterEvt> IRtoMatterMap
    Map<string, IRFrame> MatterToIRMap // key: "OnOff:On", "OnOff:Off" 등

2) 초기화 / 메인 루프
초기화:
    // 1) 네트워크/MQTT
    mqtt.connect()                               // 내부 재시도/백오프는 mqtt.loopOnce()가 처리
    mqtt.subscribe(T_CTRL_IR_TX, 1)
    mqtt.subscribe(T_CTRL_MATTER, 1)
    mqtt.subscribe(T_CONFIG_ALL, 1)
    mqtt.publish(T_DEV_STATUS, "{\"state\":\"online\"}", 0, true)

    // 2) Matter
    matter.init()
    // Temperature/Humidity/IRControl 등 클러스터는 모듈 내부에서 등록되었다고 가정

    // 3) 센서/IR
    // dht11/irRx/irTx는 하드웨어 초기화 완료 상태
    // irRx는 내부에서 엣지/프레임 수집 시작

    // 4) 타이머 초기화
    lastDhtMs = millis()
    healthMs  = millis()

메인 루프:
    nowMs = millis()

    // ── MQTT 네트워킹 유지/수신 폴링
    mqtt.loopOnce()
    while (mqtt.tryPopMessage(out msg)):
        mqttRxQueue.push(msg)

    // ── Matter 이벤트 펌프
    matter.loopOnce()
    while (matter.tryPopEvent(out evt)):
        matterEvtQueue.push(evt)

    // ── IR 수신 프레임 폴링 → 큐잉
    while (irRx.tryPopFrame(out frame)):
        irRecvQueue.push(frame)

    // ── DHT11 주기 읽기 → MQTT & Matter 리포트
    if (dht11.ready() && nowMs - lastDhtMs >= dht11.minIntervalMs()):
        float tC, hum
        if (dht11.read(out tC, out hum)):
            mqttTxQueue.push( { T_SENS_DHT,
                                payload: json({"temp":tC,"hum":hum,"ts":nowMs}),
                                qos:1, retain:false } )
            matter.reportAttribute("Temperature", tC)
            matter.reportAttribute("Humidity", hum)
        lastDhtMs = nowMs

    // ── IR 수신 처리 → MQTT 알림 + (옵션) Matter 매핑 트리거
    while (irRecvQueue.tryPop(out rx)):
        mqttTxQueue.push( { T_EVT_IR_RX,
                            payload: json({"protocol":rx.protocol,"addr":rx.addr,
                                           "cmd":rx.cmd,"repeat":rx.isRepeat,"ts":rx.tsMs}),
                            qos:1, retain:false } )

        if (IRtoMatterMap.contains((rx.protocol, rx.addr, rx.cmd))):
            matterEvtQueue.push(IRtoMatterMap[(rx.protocol, rx.addr, rx.cmd)])

    // ── Matter 명령 처리 → IR 송신 작업으로 변환
    while (matterEvtQueue.tryPop(out mevt)):
        if (mevt.type == "ClusterCommand"):
            if (mevt.cluster == "IRControl"):
                // payload: { protocol, addr, cmd, hold_ms }
                IRFrame job = {
                    protocol: mevt.payload["protocol"],
                    addr:     (uint8)mevt.payload["addr"],
                    cmd:      (uint8)mevt.payload["cmd"],
                    tsMs:     nowMs
                }
                job.holdMs = mevt.payload.getOr("hold_ms", 0)
                irSendQueueLogical.push(job)
            else:
                // 예: OnOff → IR 매핑
                string key = mevt.cluster + ":" + mevt.payload["op"]   // "OnOff:On"
                if (MatterToIRMap.contains(key)):
                    irSendQueueLogical.push(MatterToIRMap[key])

    // ── MQTT 제어 수신 → IR 송신 작업으로 변환
    while (mqttRxQueue.tryPop(out m)):
        if (topicMatch(m.topic, T_CTRL_IR_TX)):
            // payload: {"protocol":"NEC","addr":16,"cmd":175,"hold_ms":500}
            IRFrame job = parseIRJob(m.payload)   // validation 포함
            if (job.valid):
                irSendQueueLogical.push(job)
        else if (topicMatch(m.topic, T_CTRL_MATTER)):
            // 필요 시 MQTT→Matter 이벤트 변환(테스트/운영 정책에 따라)
            MatterEvt x = mapMqttToMatter(m)
            if (x.valid): matterEvtQueue.push(x)
        else if (topicMatch(m.topic, T_CONFIG_ALL)):
            applyConfig(m)                        // 리포트 주기/오차 허용/매핑 갱신 등

    // ── IR 송신 실행(S346) 및 피드백
    if (irSendQueueLogical.tryPop(out txJob)):
        if (txJob.protocol == "NEC"):
            bool ok = irTx.sendNEC(txJob.addr, txJob.cmd, txJob.holdMs)
            if (ok):
                mqttTxQueue.push( { T_EVT_IR_TX,
                                    payload: json({"protocol":"NEC","addr":txJob.addr,
                                                   "cmd":txJob.cmd,"ts":nowMs}),
                                    qos:0, retain:false } )
            else:
                // 필요 시 재시도/에러로그

    // ── MQTT 송신 큐 플러시
    while (mqtt.isConnected() && mqttTxQueue.tryPop(out outMsg)):
        mqtt.publish(outMsg.topic, outMsg.payload, outMsg.qos, outMsg.retain)

    // ── 헬스/상태 리포트 & 하우스키핑
    if (nowMs - healthMs >= HEALTH_INTERVAL_MS):
        mqtt.publish(T_DEV_STATUS, json({"state":"online","uptime_ms":nowMs}), 0, true)
        healthMs = nowMs

    // ── CPU 양보
    sleep(짧은 주기)

설계 포인트 (간략)

의존성 역전: 오케스트레이터는 인터페이스만 의존 → 하드웨어/라이브러리 교체 용이.

비동기/백프레셔: 모든 외부 이벤트는 큐로 흡수 → 일관된 처리 순서 보장.

단일 책임:

Dht11는 측정만,

IRReceiver는 프레임 완성만,

IRTransmitter는 시간 정밀 송신만,

MqttClient/MatterStack은 네트워킹만 담당.

매핑 계층: IR↔Matter, MQTT↔Matter 변환은 중앙 테이블로 분리하여 정책 변경을 쉽게.

장애 내성: MQTT 재접속/keepalive는 loopOnce()에서 처리, Matter도 동일. IR 송신은 실패시 재시도 정책을 모듈 내부 또는 상위에서 선택.

원하시면 위 pseudo code를 바로 헤더/구현(.h/.cpp) 스켈레톤으로 변환해 드리고,
pigpio + paho.mqtt.cpp + Matter SDK 조합에 맞춘 구체 어댑터(Adapter)도 함께 제공할 수 있습니다.