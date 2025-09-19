package com.eeum.dto.response;

import java.time.Instant;
import java.util.List;
import lombok.*;

@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class EnergySeriesResponse {
    private String range;
    private Instant from;
    private Instant to;
    private double totalKwh;
    private List<Point> series;

    @Getter 
    @Setter 
    @NoArgsConstructor 
    @AllArgsConstructor 
    @Builder
    public static class Point {
        private String label;
        private double kwh;
    }
}
