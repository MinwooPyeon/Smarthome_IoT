package com.eeum.service.code;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * 이메일 인증코드를 메모리에 저장하는 간단한 구현체.
 * 
 * Redis 대신 로컬 개발/테스트용으로 사용
 */
@Component
public class InMemoryCodeStore implements VerificationCodeStore {

    private static class Entry {
        final String code;
        final Instant expiresAt;
        Entry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
        boolean expired() { return Instant.now().isAfter(expiresAt); }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String email, String code, long ttlMinutes) {
        store.put(key(email), new Entry(code, Instant.now().plusSeconds(ttlMinutes * 60)));
    }

    @Override
    public String get(String email) {
        Entry e = store.get(key(email));
        if (e == null) return null;
        if (e.expired()) {
            store.remove(key(email));
            return null;
        }
        return e.code;
    }

    @Override
    public boolean exists(String email) {
        return get(email) != null;
    }

    @Override
    public void delete(String email) {
        store.remove(key(email));
    }

    private String key(String email) { return "email:code:" + email; }
}
