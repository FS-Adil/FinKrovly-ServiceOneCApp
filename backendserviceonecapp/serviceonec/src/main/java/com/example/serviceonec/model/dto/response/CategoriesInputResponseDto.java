package com.example.serviceonec.model.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoriesInputResponseDto {

    private String description;
    private String refKey;

}
