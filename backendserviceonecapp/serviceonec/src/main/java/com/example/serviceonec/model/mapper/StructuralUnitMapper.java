package com.example.serviceonec.model.mapper;

import com.example.serviceonec.model.dto.response.NomenclatureItemResponseDto;
import com.example.serviceonec.model.dto.response.StructuralUnitItemResponseDto;
import com.example.serviceonec.model.entity.NomenclatureEntity;
import com.example.serviceonec.model.entity.StructuralUnitEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StructuralUnitMapper {

    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    StructuralUnitEntity toEntity(StructuralUnitItemResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    StructuralUnitItemResponseDto toDto(StructuralUnitEntity entity);


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
