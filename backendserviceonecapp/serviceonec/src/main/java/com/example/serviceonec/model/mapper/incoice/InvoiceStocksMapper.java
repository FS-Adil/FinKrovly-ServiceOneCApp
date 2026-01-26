package com.example.serviceonec.model.mapper.incoice;

import com.example.serviceonec.model.dto.response.invoice.InvoiceStocksItemResponseDto;
import com.example.serviceonec.model.entity.invoice.InvoiceStocksEntity;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface InvoiceStocksMapper {
    // Форматтер для даты
    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * Преобразование из Entity в ResponseDto
     */
    InvoiceStocksItemResponseDto toDto(InvoiceStocksEntity entity);

    /**
     * Преобразование из ResponseDto в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    InvoiceStocksEntity toEntity(InvoiceStocksItemResponseDto dto);

    /**
     * Преобразование списка Entity в список ResponseDto
     */
    public abstract List<InvoiceStocksItemResponseDto> toDtoList(List<InvoiceStocksEntity> entities);

    /**
     * Преобразование списка ResponseDto в список Entity
     */
    public abstract List<InvoiceStocksEntity> toEntityList(List<InvoiceStocksItemResponseDto> dtos);

    /**
     * Обновление Entity из Dto
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    public abstract void updateEntityFromDto(InvoiceStocksItemResponseDto dto, @MappingTarget InvoiceStocksEntity entity);

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
