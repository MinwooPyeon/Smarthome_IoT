package com.eeum.notification;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.eeum.fcm.FcmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final FcmService fcmService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void onRoutineExecuted(RoutineExecutedEvent e) {
        try {
            String topic = "home-" + e.homeId();
            String title = "루틴 완료";
            String body  = "\"" + e.routineName() + "\" 루틴이 동작되었어요";

            Map<String, String> data = Map.of(
                    "type", "ROUTINE_EXECUTED",
                    "routineId", String.valueOf(e.routineId()),
                    "routineName", e.routineName()
            );

            fcmService.sendToTopic(topic, title, body, data);
            log.info("[FCM] routine executed push sent: topic={}, routineId={}", topic, e.routineId());
        } catch (Exception ex) {
            log.warn("[FCM] routine executed push failed: routineId={}, err={}", e.routineId(), ex.toString(), ex);
        }
    }
}
