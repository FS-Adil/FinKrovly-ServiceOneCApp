package com.example.serviceonec.model.mapper.production;

import com.example.serviceonec.model.dto.response.production.ProductionItemResponseDto;
import com.example.serviceonec.model.entity.production.ProductionEntity;
import com.example.serviceonec.model.entity.production.ProductionStocksEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ProductionStocksMapper {

    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProductionStocksEntity toEntity(ProductionItemResponseDto.ProductionStocksDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    ProductionItemResponseDto.ProductionStocksDto toDto(ProductionStocksEntity entity);

    /**
     * Кастомный маппинг для UUID (если приходит как String)
     */
    @Named("stringToUUID")
    default UUID stringToUUID(String uuidString) {
        if (uuidString == null || uuidString.isEmpty()) {
            return null;
        }
        return UUID.fromString(uuidString);
    }
}
