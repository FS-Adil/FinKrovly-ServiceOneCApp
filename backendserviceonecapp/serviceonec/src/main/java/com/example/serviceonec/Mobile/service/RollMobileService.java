package com.example.serviceonec.Mobile.service;

import com.example.serviceonec.Mobile.dto.input.RollMobileInputResponseDto;

import java.util.List;

public interface RollMobileService {
    List<RollMobileInputResponseDto> getAllRolls(String userName);
}
