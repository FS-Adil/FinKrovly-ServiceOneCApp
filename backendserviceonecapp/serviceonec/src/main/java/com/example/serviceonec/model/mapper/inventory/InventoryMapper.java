package com.example.serviceonec.model.mapper.inventory;

import com.example.serviceonec.model.dto.response.inventory.InventoryItemResponseDto;
import com.example.serviceonec.model.dto.response.inventory.InventoryResponseDto;
import com.example.serviceonec.model.entity.inventory.InventoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    // Форматтер для даты
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    InventoryEntity toEntity(InventoryItemResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
//    InventoryItemResponseDto toDto(InventoryEntity entity);

    /**
     * Кастомный маппинг для строки даты (если приходит как String)
     */
    @Named("stringToLocalDateTime")
    default LocalDateTime stringToLocalDateTime(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateString, DATE_FORMATTER);
        } catch (Exception e) {
            // Обработка разных форматов даты
            return LocalDateTime.parse(dateString);
        }
    }
}
