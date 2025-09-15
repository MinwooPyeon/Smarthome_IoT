package com.eeum.dto.response;

import com.eeum.entity.RoutineIcon;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class IconResponse {
    private Integer iconId;
    private String url;

    public static IconResponse from(RoutineIcon icon) {
        return IconResponse.builder()
                .iconId(icon.getIconId())
                .url(icon.getIconUrl())
                .build();
    }
}
