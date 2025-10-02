package com.eeum.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ir_button")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IrButton {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer buttonId;

    private String label;    
    private String category; 
    private String model;    
}