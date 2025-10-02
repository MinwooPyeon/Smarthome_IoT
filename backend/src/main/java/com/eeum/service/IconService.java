package com.eeum.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.eeum.dto.response.IconResponse;
import com.eeum.entity.RoutineIcon;
import com.eeum.repository.IconRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IconService {
    private final IconRepository iconRepository;

    // 아이콘 전체 목록 조회
    public List<IconResponse> getIcons() {

    	List<RoutineIcon> icons = iconRepository.findAll();
    	
        return icons.stream()
                .map(IconResponse::from)
                .collect(Collectors.toList());
    }
}


