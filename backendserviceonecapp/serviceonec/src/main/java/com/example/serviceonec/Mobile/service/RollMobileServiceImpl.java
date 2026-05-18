package com.example.serviceonec.Mobile.service;

import com.example.serviceonec.Mobile.dto.input.RollMobileInputResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RollMobileServiceImpl implements RollMobileService {

    @Override
    public List<RollMobileInputResponseDto> getAllRolls(String userName) {

        log.info("get All Rolls on 1C {}", userName);
        return List.of(
                RollMobileInputResponseDto.builder()
                        .name("Рулон 0,5 7024")
                        .manufacturer("Россия Премиум")
                        .organization("ООО \"МеталлПрофиль\"")
                        .batch("Б-2024-001")
                        .pricePerTon(new BigDecimal("2500.50"))
                        .pricePerSquareMeter(new BigDecimal("2500.50"))
                        .areaSquareMeters(BigDecimal.valueOf(1234))
                        .weightTons(BigDecimal.valueOf(2.34))
                        .batchDate(LocalDate.of(2024, 1, 15))
                        .build(),
                RollMobileInputResponseDto.builder()
                        .name("Рулон 0,5 7024")
                        .manufacturer("Россия Премиум")
                        .organization("ООО \"МеталлПрофиль\"")
                        .batch("Б-2024-002")
                        .pricePerSquareMeter(new BigDecimal("2550.00"))
                        .pricePerTon(new BigDecimal("2550.00"))
                        .areaSquareMeters(BigDecimal.valueOf(1234))
                        .weightTons(BigDecimal.valueOf(2.34))
                        .batchDate(LocalDate.of(2024, 2, 20))
                        .build(),
                RollMobileInputResponseDto.builder()
                        .name("Рулон 0,5 7024")
                        .manufacturer("Корея")
                        .organization("ООО \"МеталлПрофиль\"")
                        .batch("К-2024-001")
                        .pricePerSquareMeter(new BigDecimal("3200.75"))
                        .pricePerTon(new BigDecimal("3200.75"))
                        .areaSquareMeters(BigDecimal.valueOf(1234))
                        .weightTons(BigDecimal.valueOf(2.34))
                        .batchDate(LocalDate.of(2024, 3, 10))
                        .build(),
                RollMobileInputResponseDto.builder()
                        .name("Рулон 0,7 9003")
                        .manufacturer("Корея")
                        .organization("ООО \"МеталлПрофиль\"")
                        .batch("К-2024-002")
                        .pricePerSquareMeter(new BigDecimal("3400.00"))
                        .pricePerTon(new BigDecimal("3400.00"))
                        .areaSquareMeters(BigDecimal.valueOf(1234))
                        .weightTons(BigDecimal.valueOf(2.34))
                        .batchDate(LocalDate.of(2024, 3, 15))
                        .build(),
                RollMobileInputResponseDto.builder()
                        .name("Рулон 0,7 9003")
                        .manufacturer("Китай")
                        .organization("ЗАО \"СтальПром\"")
                        .batch("CH-2024-001")
                        .pricePerTon(new BigDecimal("2800.00"))
                        .pricePerSquareMeter(new BigDecimal("2800.00"))
                        .areaSquareMeters(BigDecimal.valueOf(1234))
                        .weightTons(BigDecimal.valueOf(2.34))
                        .batchDate(LocalDate.of(2024, 4, 1))
                        .build(),
                RollMobileInputResponseDto.builder()
                        .name("Рулон 0,5 5005")
                        .manufacturer("Китай")
                        .organization("ЗАО \"СтальПром\"")
                        .batch("CH-2024-002")
                        .pricePerTon(new BigDecimal("2700.50"))
                        .pricePerSquareMeter(new BigDecimal("2700.50"))
                        .areaSquareMeters(BigDecimal.valueOf(1234))
                        .weightTons(BigDecimal.valueOf(2.34))
                        .batchDate(LocalDate.of(2024, 4, 10))
                        .build()
        );
    }
}
