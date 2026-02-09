package com.example.serviceonec.service.rolllist;

import com.example.serviceonec.model.dto.response.rolllist.RollListItemResponseDto;
import com.example.serviceonec.model.entity.rolllist.RollListEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RollListService {
    List<RollListEntity> getAllClosedRoll();
}
