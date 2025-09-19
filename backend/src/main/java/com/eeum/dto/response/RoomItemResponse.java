package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomItemResponse {
    private Integer roomId;
    private String  roomName;
    private Integer roomColor;
}
