package com.example.serviceonec.Mobile.controller;


import com.example.serviceonec.Mobile.dto.input.RollMobileInputResponseDto;
import com.example.serviceonec.Mobile.service.RollMobileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/mobile/roll")
@RequiredArgsConstructor
@Slf4j
public class RollMobileController {

    private final RollMobileService rollMobileService;

    @GetMapping("/get-all")
    public ResponseEntity<List<RollMobileInputResponseDto>> getAllRolls(
            @RequestParam(required = false) String userName
    ) {

        List<RollMobileInputResponseDto> list = rollMobileService.getAllRolls(userName);

        if (list.isEmpty()) {
            ResponseEntity.notFound();
        }

        return ResponseEntity.ok(list);
    }

}
