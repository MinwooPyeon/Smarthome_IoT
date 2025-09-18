package com.eeum.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.eeum.entity.IrSignal;
import com.eeum.mqtt.inbound.EnvIn;
import com.eeum.mqtt.inbound.ErrorIn;
import com.eeum.mqtt.inbound.IrProtocolIn;
import com.eeum.mqtt.inbound.IrSignalIn;
import com.eeum.mqtt.inbound.RequestIn;
import com.eeum.repository.IrSignalRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * MQTT 수신 메시지 처리 서비스.
 * - JSON 파싱(ObjectMapper)
 * - Bean Validation으로 필수/형식 검증
 * - msgId 기반 멱등(중복) 제거
 * - (TODO) 저장/가공/메트릭/재발행 포인트
 *
 * 사용법:
 *   MqttStarter.messageArrived(...) 에서
 *   ioService.onMessage(topic, message.getPayload());
 */
@Slf4j
@Service
public class MqttIoService {
	
	@Autowired
    private IrSignalRepository irSignalRepository;

    // 알 수 없는 필드는 무시
    private final ObjectMapper om = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Bean Validation
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    // 간단한 멱등 캐시: msgId -> 수신시각(ms)
    // 운영에선 Redis/DB 등으로 교체 권장
    private final ConcurrentHashMap<String, Long> seen = new ConcurrentHashMap<>();
    private static final long DEDUP_TTL_MS = 10 * 60 * 1000L; // 10분

    // MQTT 콜백 진입점.
    public void onMessage(String topic, byte[] payloadBytes) {
        final String payload = new String(payloadBytes, StandardCharsets.UTF_8);

        try {
            if (topic.endsWith("/env")) {
                handleEnv(payload);
            } 
            else if (topic.endsWith("/irSignal")) {
                handleIr(payload);
            } 
            else if (topic.endsWith("/irProtocol")) {
                handleIrProtocol(payload);
            }
            else if (topic.endsWith("/error")) {
                handleError(payload);
            }
            else if (topic.endsWith("/request")) {
                handleRequest(payload);
            }
            else {
                log.debug("[MQTT] bypass: {} -> {}", topic, payload);
            }
            
        } catch (Exception e) {
            log.error("[MQTT] onMessage error (topic={}): {}", topic, e.getMessage(), e);
        }
    }

    // hub/{deviceId}/env
    public void handleEnv(String json) {
        try {
            EnvIn m = om.readValue(json, EnvIn.class);

            // 스키마 접두사 확인
            if (m.getSchema() == null || !m.getSchema().startsWith("env/")) {
                log.warn("[ENV] invalid schema: {}", m.getSchema());
                return;
            }

            // 필수/형식 검증
            if (!validate(m)) return;

            // 멱등(중복) 제거
            if (isDup(m.getMsgId())) {
                log.debug("[ENV] dup msgId={}, drop", m.getMsgId());
                return;
            }

            // (TODO) DB 저장/메트릭/알림 등
            log.info("[ENV] ok device={} ts={} t={} h={} dew={} hi={} abs={} pmv={} ppd={} wbgt={}",
                    m.getDeviceId(), m.getTs(),
                    m.getTemperature(), m.getHumidity(),
                    m.getDewPoint(), m.getHeadIndex(), m.getAbsHumidity(),
                    m.getPmv(), m.getPpd(), m.getWbgt());

        } catch (Exception e) {
            log.error("[ENV] parse/handle error: {}", e.getMessage(), e);
        }
    }

    // hub/{deviceId}/irSignal
    public void handleIr(String json) {
        try {
            IrSignalIn m = om.readValue(json, IrSignalIn.class);

            if (!validate(m)) return;
            if (m.getRawData() == null || m.getRawData().length == 0) return;
            if (isDup(m.getMsgId())) return;

            String model = m.getBrand() + "_" + m.getDevice();
            String name = m.getFunction();

            IrSignal existing = irSignalRepository.findByModelAndName(model, name).orElse(null);

            IrSignal entity;
            if (existing != null) {
                // 덮어쓰기
                existing.setSamplesUs(m.getRawData());
                entity = existing;
                log.info("[IR] 덮어쓰기: model={}, function={}", model, name);
            } else {
                // 새로 저장
                entity = IrSignal.builder()
                    .name(name)
                    .samplesUs(m.getRawData())
                    .model(model)
                    .protocolId(null)
                    .build();
                log.info("[IR] 새로 저장: model={}, function={}", model, name);
            }

            irSignalRepository.save(entity);

        } catch (Exception e) {
            log.error("[IR] 파싱 또는 저장 오류: {}", e.getMessage(), e);
        }
    }
    
    // hub/{deviceId}/irProtocol
    public void handleIrProtocol(String json) {
        try {
            IrProtocolIn m = om.readValue(json, IrProtocolIn.class);

            if (!validate(m)) return;

            // (TODO) 2단계: protocol DB upsert 정책 반영
            log.info("[IRP] brand={} device={} proto={} unit={} gap={} avg_len={} header=[{},{}] zero=[{},{}] one=[{},{}]",
                m.getBrand(), m.getDevice(), m.getProtocolName(), m.getUnit(), m.getGap(), m.getAvgLen(),
                safe(m.getHeader(),0), safe(m.getHeader(),1), safe(m.getZero(),0), safe(m.getZero(),1),
                safe(m.getOne(),0), safe(m.getOne(),1));

        } catch (Exception e) {
            log.warn("[IRP] parse/handle error: {}", e.getMessage(), e);
        }
    }

    // hub/{deviceId}/error
    public void handleError(String json) {
        try {
            ErrorIn m = om.readValue(json, ErrorIn.class);
            if (!validate(m)) return;

            // 문서 명세 그대로 출력
            log.warn("[ERR] tx_id={} error={} msg={}",
            		m.getTxId(), m.getError(), m.getMessage());

        } catch (Exception e) {
            log.warn("[ERR] parse/handle error: {}", e.getMessage(), e);
        }
    }

    // hub/{deviceId}/request
    public void handleRequest(String json) {
        try {
            RequestIn m = om.readValue(json, RequestIn.class);
            if (!validate(m)) return;

            log.info("[REQ] type={} streaming={}", m.getType(), m.isStreaming());
            // (TODO) 2단계: type=env일 때 streaming=true면 서버측 정책에 따라 제어 메시지 발행 or 단순 수신 허용
        } catch (Exception e) {
            log.warn("[REQ] parse/handle error: {}", e.getMessage(), e);
        }
    }
    

    // ---------- 내부 유틸 ----------

    private boolean validate(Object bean) {
        Set<ConstraintViolation<Object>> v = validator.validate(bean);
        if (!v.isEmpty()) {
            ConstraintViolation<Object> one = v.iterator().next();
            log.warn("[VALIDATION] {} {} -> {}",
                    bean.getClass().getSimpleName(),
                    one.getPropertyPath(),
                    one.getMessage());
            return false;
        }
        return true;
    }

    private boolean isDup(String msgId) {
        if (msgId == null || msgId.isBlank()) return false;
        long now = System.currentTimeMillis();
        Long prev = seen.putIfAbsent(msgId, now);
        if (prev != null) return true;

        // 간헐 청소 (부하 적음)
        if (seen.size() % 100 == 0) {
            long cutoff = now - DEDUP_TTL_MS;
            seen.entrySet().removeIf(e -> e.getValue() < cutoff);
        }
        return false;
    }

    private static int safe(int[] a, int idx) {
        if (a == null || a.length <= idx) return -1;
        return a[idx];
    }
}