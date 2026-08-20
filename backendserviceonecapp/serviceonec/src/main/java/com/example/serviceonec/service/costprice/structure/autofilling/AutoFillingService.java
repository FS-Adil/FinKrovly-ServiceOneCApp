package com.example.serviceonec.service.costprice.structure.autofilling;

import com.example.serviceonec.controller.costprice.output.CostPriceAutoFillingControllerOutput;
import com.example.serviceonec.controller.costprice.output.CostPriceControllerOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class AutoFillingService {

    List<String> whitelist = List.of(
            "Работа",
            "Перевозка",
            "Поддон"
    );

    public List<CostPriceAutoFillingControllerOutput> getAutoFilling(List<CostPriceControllerOutput> productList) {

        List<CostPriceAutoFillingControllerOutput> result = new ArrayList<>(productList.size());

        String autoFillingStatus;

        for (CostPriceControllerOutput product : productList) {

            BigDecimal cost = product.getCost();
            BigDecimal price = product.getPrice();
            autoFillingStatus = "Да";

            if (cost.compareTo(BigDecimal.ZERO) == 0) {
                if (whitelist.contains(product.getCategories())){
                    autoFillingStatus = "Нет";
                    cost = BigDecimal.ZERO;
                } else {
                    autoFillingStatus = "Нет";
                    cost = price.multiply(BigDecimal.valueOf(0.85));
                }
            }

            result.add(
                    CostPriceAutoFillingControllerOutput.builder()
                            .refKey(product.getRefKey())
                            .number(product.getNumber())
                            .name(product.getName())
                            .characteristic(product.getCharacteristic())
                            .batch(product.getBatch())
                            .measurementUnit(product.getMeasurementUnit())
                            .categories(product.getCategories())
                            .quantity(product.getQuantity())
                            .price(price)
                            .cost(cost)
                            .autoFilling(autoFillingStatus)
                            .build()
            );
        }
        return result;
    }
}
