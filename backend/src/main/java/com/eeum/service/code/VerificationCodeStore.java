package com.eeum.service.code;

public interface VerificationCodeStore {
    void save(String email, String code, long ttlMinutes);
    String get(String email);
    boolean exists(String email);
    void delete(String email);
}
