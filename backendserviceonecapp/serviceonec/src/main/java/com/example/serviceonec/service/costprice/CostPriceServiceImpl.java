package com.example.serviceonec.service.costprice;

import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CostPriceServiceImpl implements CostPriceService {
    @Override
    public List<CostPriceControllerOutput> getAllCostPrice() {

        return List.of();
    }
}
