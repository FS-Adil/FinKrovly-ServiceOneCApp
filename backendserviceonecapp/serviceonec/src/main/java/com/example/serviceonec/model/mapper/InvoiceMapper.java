package com.example.serviceonec.model.mapper;

import com.example.serviceonec.model.dto.response.InvoiceValueResponseDto;
import com.example.serviceonec.model.entity.InvoiceEntity;
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
    InvoiceEntity toEntity(InvoiceValueResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    InvoiceValueResponseDto toDto(InvoiceEntity entity);

    /**
     * Маппинг списка Entity в список DTO
     */
    List<InvoiceValueResponseDto> toDtoList(List<InvoiceEntity> entities);

    /**
     * Обновление Entity из DTO
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(InvoiceValueResponseDto dto, @MappingTarget InvoiceEntity entity);

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