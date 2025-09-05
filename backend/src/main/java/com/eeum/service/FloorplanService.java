package com.eeum.service;

import com.eeum.dto.request.FloorplanRequest;
import com.eeum.entity.Floorplans;
import com.eeum.repository.FloorplanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class FloorplanService {

    private final FloorplanRepository floorplanRepository;

    @Transactional
    public Integer addFloorplan(FloorplanRequest req) {
        if (req.getHomeId() == null) {
            throw new IllegalArgumentException("homeId는 필수입니다.");
        }

        Floorplans fp = new Floorplans();
        fp.setHomeId(req.getHomeId());
        fp.setImageUrl(req.getImageUrl());
        fp.setSquare(req.getSquare());
        fp.setCreatedAt(OffsetDateTime.now());

        Floorplans saved = floorplanRepository.save(fp);
        return saved.getFloorplanId();
    }
}
