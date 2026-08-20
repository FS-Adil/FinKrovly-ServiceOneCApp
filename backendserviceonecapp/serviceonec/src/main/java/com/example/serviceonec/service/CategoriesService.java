package com.example.serviceonec.service;

import com.example.serviceonec.model.dto.InputApiResponse;
import com.example.serviceonec.model.dto.response.CategoriesInputResponseDto;

import java.util.List;

public interface CategoriesService {
    List<CategoriesInputResponseDto> getAllCategories();
}
