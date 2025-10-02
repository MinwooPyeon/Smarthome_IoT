package com.eeum.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor 
@NoArgsConstructor 
@Getter
public class AddressListResponse {
    private List<AddressItemResponse> items;
}
