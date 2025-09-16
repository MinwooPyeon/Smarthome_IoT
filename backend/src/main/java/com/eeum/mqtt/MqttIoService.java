package com.eeum.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.eeum.mqtt.inbound.EnvIn;
import com.eeum.mqtt.inbound.IrSignalIn;
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
            
            else if (topic.endsWith("/irsignal")) {
                handleIr(payload);
            } 
            
            else {
                log.debug("[MQTT] bypass: {} -> {}", topic, payload);
            }
            
        } catch (Exception e) {
            log.error("[MQTT] onMessage error (topic={}): {}", topic, e.getMessage(), e);
        }
    }

    // hub/{deviceId}/env 처리
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

            // 교정 적용(있으면)
            Double t = applyOffset(m.getTemperature(), m.getCalib() != null ? m.getCalib().getTOffset() : null);
            Double h = applyOffset(m.getHumidity(),   m.getCalib() != null ? m.getCalib().getHOffset() : null);

            // (TODO) DB 저장/메트릭/알림 등
            log.info("[ENV] ok device={} ts={} t={} h={} gas={}",
                    m.getDeviceId(), m.getTs(), t, h, m.getGasDensity());

        } catch (Exception e) {
            log.error("[ENV] parse/handle error: {}", e.getMessage(), e);
        }
    }

    /**
     * hub/{deviceId}/irsignal 처리:
     *  - 파싱 → 검증 → (data 또는 rawData+timing 확인) → 멱등 → (TODO 저장/학습통계)
     */
    public void handleIr(String json) {
        try {
            IrSignalIn m = om.readValue(json, IrSignalIn.class);

            if (m.getSchema() == null || !m.getSchema().startsWith("irsignal/")) {
                log.warn("[IR ] invalid schema: {}", m.getSchema());
                return;
            }

            if (!validate(m)) return;

            boolean hasData = m.getData() != null && !m.getData().isBlank();
            boolean hasRaw  = m.getRawData() != null && !m.getRawData().isEmpty() && m.getTiming() != null;
            if (!hasData && !hasRaw) {
                log.warn("[IR ] neither data nor rawData+timing present (msgId={})", m.getMsgId());
                return;
            }

            if (isDup(m.getMsgId())) {
                log.debug("[IR ] dup msgId={}, drop", m.getMsgId());
                return;
            }

            // (TODO) 저장/사전 업데이트/품질 통계
            int rawCount = (m.getRawData() == null ? 0 : m.getRawData().size());
            log.info("[IR ] ok device={} ts={} enc={} repeat={} quality={} dataLen={} raw#={}",
                    m.getDeviceId(), m.getTs(), m.getEncoding(),
                    m.getRepeat(), m.getQuality(),
                    (m.getData() == null ? 0 : m.getData().length()),
                    rawCount);

        } catch (Exception e) {
            log.error("[IR ] parse/handle error: {}", e.getMessage(), e);
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

    private static Double applyOffset(Double value, Double offset) {
        if (value == null) return null;
        if (offset == null) return value;
        return value + offset;
    }
}
