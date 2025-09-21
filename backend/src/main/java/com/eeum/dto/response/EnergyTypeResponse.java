package com.eeum.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.*;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class EnergyTypeResponse {
    private String range;
    private Instant from;
    private Instant to;
    private double totalKwh;
    private List<Item> items;

    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @Builder
    public static class Item {
        private String deviceType;
        private double kwh;
        private double percentage;
    }
}
