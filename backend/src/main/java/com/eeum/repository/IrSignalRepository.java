package com.eeum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eeum.entity.IrSignal;

@Repository
public interface IrSignalRepository extends JpaRepository<IrSignal, Integer> {
    Optional<IrSignal> findByModelAndName(String model, String name);
}
