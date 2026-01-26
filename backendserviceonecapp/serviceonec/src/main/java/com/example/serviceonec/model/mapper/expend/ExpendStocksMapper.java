package com.example.serviceonec.model.mapper.expend;

import com.example.serviceonec.model.dto.response.NomenclatureItemResponseDto;
import com.example.serviceonec.model.dto.response.expend.ExpendStocksItemResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.expend.ExpendStocksEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ExpendStocksMapper {
    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ExpendStocksEntity toEntity(ExpendStocksItemResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    ExpendStocksItemResponseDto toDto(ExpendStocksEntity entity);

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
