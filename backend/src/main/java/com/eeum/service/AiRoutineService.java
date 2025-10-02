package com.eeum.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.eeum.entity.AiRoutine;
import com.eeum.repository.AiRoutineRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiRoutineService {
    private final AiRoutineRepository aiRoutineRepository;

    public List<AiRoutine> getAll() {
        return aiRoutineRepository.findAll();
    }
}
