package com.example.serviceonec.service.assembly_expend;

import com.example.serviceonec.model.entity.CharacteristicEntity;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.assembly.AssemblyExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import com.example.serviceonec.repository.CharacteristicRepository;
import com.example.serviceonec.repository.NomenclatureRepository;
import com.example.serviceonec.repository.assembly.AssemblyExpendRepository;
import com.example.serviceonec.repository.expend.ExpendRepository;
import com.example.serviceonec.repository.expend.ExpendStocksRepository;
import com.example.serviceonec.repository.inventory.InventoryStocksRepository;
import com.example.serviceonec.repository.invoice.InvoiceStocksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssemblyExpendServiceImpl implements AssemblyExpendService{

    private final AssemblyExpendRepository assemblyExpendRepository;
    private final ExpendRepository expendRepository;
    private final ExpendStocksRepository expendStocksRepository;
    private final NomenclatureRepository nomenclatureRepository;
    private final CharacteristicRepository characteristicRepository;
    private final InvoiceStocksRepository invoiceStocksRepository;


    @Override
    public Page<AssemblyExpendEntity> addAllAssemblyExpend() {
        log.info("-->AssemblyExpendServiceImpl-->addAllAssemblyExpend--вызов метода");

        assemblyExpendRepository.deleteAll();
        log.info("-->Чистим таблицу assembly_expend");

        int pageSize = 300;
        int pageNumber = 0;
        Page<ExpendEntity> page;

        do {
            log.info("-->addAllAssemblyExpend-->do-while-->pageNumber:{}--pageSize:{}", pageNumber, pageSize);
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            page = expendRepository.findAll(pageable);

            // Обработка данных текущей страницы
            for (ExpendEntity entity : page.getContent()) {
                UUID uuid = entity.getRefKey();
//                log.info("-->for-->pageNumber:{}--pageSize:{}--ExpendEntity:{}", pageNumber, pageSize, uuid);

                List<ExpendStocksEntity> expendStocksEntities = expendStocksRepository.findAllByRefKey(uuid);

                if (expendStocksEntities.isEmpty()) {
                    ExpendStocksEntity expendStocksEntity = new ExpendStocksEntity();

                    expendStocksEntity.setPrice(new BigDecimal("0.0000"));
                    expendStocksEntity.setRefKey(uuid);
                    expendStocksEntity.setCharacteristicKey(UUID.randomUUID());
                    expendStocksEntity.setNomenclatureKey(UUID.randomUUID());
                    expendStocksEntity.setLineNumber("0000");
                    expendStocksEntity.setMeasurementUnit(UUID.randomUUID());
                    expendStocksEntity.setQuantity(new BigDecimal("0.0000"));

                    expendStocksEntities.add(expendStocksEntity);
                }

                for (ExpendStocksEntity stocksEntity : expendStocksEntities) {
//                    log.info("-->for-->pageNumber:{}--pageSize:{}--ExpendStocksEntity:{}", pageNumber, pageSize, stocksEntity.getRefKey());

                    NomenclatureEntity nomenclatureEntity = nomenclatureRepository.findByRefKey(stocksEntity.getNomenclatureKey());
                    CharacteristicEntity characteristicEntity = characteristicRepository.findByRefKey(stocksEntity.getCharacteristicKey());

                    String description_nom;
                    if (nomenclatureEntity != null) {
                        description_nom = nomenclatureEntity.getDescription();
                    } else {
                        // Обработка null значений
                        description_nom = "-";
                        log.info("-->No nomenclature for ExpendEntity id: {}", entity.getId());
                    }

                    String description_ch;
                    if (characteristicEntity != null) {
                        description_ch = characteristicEntity.getDescription();
                    } else {
                        // Обработка null значений
                        description_ch = "-";
                        log.info("-->No characteristic for ExpendEntity id: {}", entity.getId());
                    }

                    AssemblyExpendEntity assemblyExpendEntity = AssemblyExpendEntity.builder()
                            .nomenclature(description_nom)
                            .nomenclatureKey(stocksEntity.getNomenclatureKey())
                            .characteristic(description_ch)
                            .characteristicKey(stocksEntity.getCharacteristicKey())
                            .date(entity.getDate())
                            .quantity(stocksEntity.getQuantity())
                            .price(stocksEntity.getPrice())
                            .expendStocksId(entity.getId())
                            .build();
                    assemblyExpendRepository.save(assemblyExpendEntity);
                }
            }

            pageNumber++;
        } while (pageNumber < page.getTotalPages());

        return assemblyExpendRepository.findAll(PageRequest.of(0, 10));
    }

    @Override
    public Page<AssemblyExpendEntity> costCalculation() {

        log.info("-->AssemblyExpendServiceImpl-->costCalculation--вызов метода");

        int pageSize = 300;
        int pageNumber = 0;
        Page<AssemblyExpendEntity> page;

        do {
            log.info("-->costCalculation-->do-while-->pageNumber:{}--pageSize:{}", pageNumber, pageSize);

            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            page = assemblyExpendRepository.findAll(pageable);

            // Обработка данных текущей страницы
            for (AssemblyExpendEntity entity : page.getContent()) {
                UUID uuid_nom = entity.getNomenclatureKey();
                UUID uuid_ch = entity.getCharacteristicKey();

                List<InvoiceStocksEntity> invoiceStocksEntities = invoiceStocksRepository.findAllByNomenclatureKey(uuid_nom);

                for (InvoiceStocksEntity invoiceStocksEntity : invoiceStocksEntities) {
                    if (invoiceStocksEntity.getCharacteristicKey().compareTo(uuid_ch) == 0) {
                        entity.setCostPrice(invoiceStocksEntity.getPrice());
                        log.info("-->getCharacteristicKey({})-- uuid_ch({})", invoiceStocksEntity.getCharacteristicKey(), uuid_ch);
                    }
                }

                assemblyExpendRepository.save(entity);
            }

            pageNumber++;
        } while (pageNumber < page.getTotalPages());

        return assemblyExpendRepository.findAll(PageRequest.of(0, 10));
    }
}
