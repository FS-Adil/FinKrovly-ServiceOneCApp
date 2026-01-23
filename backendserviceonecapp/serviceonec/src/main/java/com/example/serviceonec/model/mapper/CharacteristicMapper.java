package com.example.serviceonec.model.mapper;

import com.example.serviceonec.model.dto.response.CharacteristicItemResponseDto;
import com.example.serviceonec.model.entity.CharacteristicEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface CharacteristicMapper {

    /**
     * Маппинг из DTO в Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    CharacteristicEntity toEntity(CharacteristicItemResponseDto dto);

    /**
     * Маппинг из Entity в DTO
     */
    CharacteristicItemResponseDto toDto(CharacteristicEntity entity);

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
