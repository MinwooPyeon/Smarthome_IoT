package com.eeum.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eeum.entity.IrButton;

@Repository
public interface IrButtonRepository extends JpaRepository<IrButton, Integer> {
    Optional<IrButton> findByModelAndCategory(String model, String category);
}