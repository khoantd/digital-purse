package com.ros.ewallet.dto.mapper;

import com.ros.ewallet.dto.response.TransactionResponse;
import com.ros.ewallet.domain.entity.Transaction;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static com.ros.ewallet.common.Constants.DATE_TIME_FORMAT;

/**
 * Mapper used for mapping TransactionResponse fields.
 */
@Mapper(componentModel = "spring")
public interface TransactionResponseMapper {

    @Mapping(target = "reversesTransaction", ignore = true)
    Transaction toTransaction(TransactionResponse dto);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "reversesTransactionId", ignore = true)
    @Mapping(target = "reversedByTransactionId", ignore = true)
    @Mapping(target = "reversible", ignore = true)
    TransactionResponse toTransactionResponse(Transaction entity);

    @AfterMapping
    default void formatCreatedAt(@MappingTarget TransactionResponse dto, Transaction entity) {
        LocalDateTime datetime = LocalDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC);
        dto.setCreatedAt(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT).format(datetime));
    }
}
