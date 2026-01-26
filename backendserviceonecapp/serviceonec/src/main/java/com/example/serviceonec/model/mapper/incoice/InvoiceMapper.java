package com.example.serviceonec.model.mapper.incoice;

import com.example.serviceonec.model.dto.response.invoice.InvoiceItemResponseDto;
import com.example.serviceonec.model.entity.invoice.InvoiceEntity;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    // Форматтер для даты
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    InvoiceEntity toEntity(InvoiceItemResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    InvoiceItemResponseDto toDto(InvoiceEntity entity);

    /**
     * Маппинг списка Entity в список DTO
     */
    List<InvoiceItemResponseDto> toDtoList(List<InvoiceEntity> entities);

    /**
     * Обновление Entity из DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(InvoiceItemResponseDto dto, @MappingTarget InvoiceEntity entity);

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