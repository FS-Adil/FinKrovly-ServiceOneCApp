package com.example.serviceonec.service.costprice.structure.production;

import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.repository.production.ProductionRepository;
import com.example.serviceonec.service.production.ProductionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class CostPriceStructureProduction {

    private final ProductionRepository productionRepository;

    private final ProductionService productionService;

    public void getAllProduction(List<ExpendEntity> expendEntities) {
        log.debug("🔍 Загрузка документов по производству");

        // Необходимо сравнить UUID расходников и производства,
        // для выявления недостающих производств привязанных к заказам покупателей
        if (expendEntities == null || expendEntities.isEmpty()) {
            log.debug("Список расходников пуст");
//            return Collections.emptyMap();
        }
        List<UUID> docOrders = expendEntities.stream()
                .map(ExpendEntity::getDocOrder)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.debug("Получено {} уникальных docOrder расходников", docOrders.size());

        List<ProductionEntity> productionEntities = productionRepository.findAll();
        if (productionEntities.isEmpty()) {
            log.debug("Список документов по производству пуст");
        }
        List<UUID> customerOrderKey = productionEntities.stream()
                .map(ProductionEntity::getCustomerOrderKey)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        log.debug("Получено {} уникальных customerOrderKey документов по производству", customerOrderKey.size());

        Set<UUID> customerOrderSet = new HashSet<>(customerOrderKey);
        List<UUID> missingCustomerOrderKeys = docOrders.stream()
                .filter(key -> !customerOrderSet.contains(key))
                .toList();
        if (!missingCustomerOrderKeys.isEmpty()) {
            log.info("🔄 Обнаружено {} расходников без документов на производство, загружаем из 1С...", missingCustomerOrderKeys.size());
            try {

                productionService.getAllProductionByCustomerOrders(missingCustomerOrderKeys);

            } catch(Exception e){
                log.error("❌ Ошибка загрузки недостающих документов по производству: {}", e.getMessage(), e);
            }
        }
    }



//        List<ProductionEntity> allProductions = productionRepository.findAllByCustomerOrderKeyIn(docOrders);
//        log.debug("Загружено {} записей запасов из БД", allProductions.size());


}
