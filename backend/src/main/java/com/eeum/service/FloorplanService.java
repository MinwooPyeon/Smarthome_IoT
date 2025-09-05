package com.eeum.service;

import com.eeum.dto.request.FloorplanRequest;
import com.eeum.entity.Floorplan;
import com.eeum.repository.FloorplanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FloorplanService {

    private final FloorplanRepository floorplanRepository;

    @Transactional
    public Integer addFloorplan(FloorplanRequest req) {
        if (req.getHomeId() == null) {
            throw new IllegalArgumentException("homeId는 필수입니다.");
        }

        Floorplan fp = new Floorplan();
        fp.setHomeId(req.getHomeId());
        fp.setImageUrl(req.getImageUrl());
        fp.setSquare(req.getSquare());
        fp.setCreatedAt(OffsetDateTime.now());

        Floorplan saved = floorplanRepository.save(fp);
        return saved.getFloorplanId();
    }
    
    @Transactional(readOnly = true)
    public List<Floorplan> getFloorplans(Integer homeId) {
        if (homeId == null) {
            throw new IllegalArgumentException("homeId는 필수입니다.");
        }
        return floorplanRepository.findByHomeId(homeId);
    }
}


