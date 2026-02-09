package com.example.serviceonec.model.mapper.expend;

import com.example.serviceonec.model.dto.response.expend.ExpendItemResponseDto;
import com.example.serviceonec.model.dto.response.invoice.InvoiceItemResponseDto;
import com.example.serviceonec.model.entity.expend.ExpendEntity;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface ExpendMapper {

    // Форматтер для даты
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    ExpendEntity toEntity(ExpendItemResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    ExpendItemResponseDto toDto(ExpendEntity entity);

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
