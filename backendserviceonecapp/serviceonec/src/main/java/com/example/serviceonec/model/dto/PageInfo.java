package com.example.serviceonec.model.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageInfo {

    private int current;
    private int totalPages;
    private long totalElements;
    private int size;

}
