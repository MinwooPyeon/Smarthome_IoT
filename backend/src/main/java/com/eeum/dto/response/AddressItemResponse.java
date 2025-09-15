package com.eeum.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Getter
public class AddressItemResponse {
    private Integer addressId;
    private Double longitude;
    private Double latitude;
    private String homeName;
}
