package com.ros.ewallet.service;

import com.ros.ewallet.domain.entity.IdempotencyRecord;
import com.ros.ewallet.repository.IdempotencyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    public static final String OP_TRANSFER = "TRANSFER";
    public static final String OP_ADD_FUNDS = "ADD_FUNDS";
    public static final String OP_WITHDRAW = "WITHDRAW";
    public static final String OP_REVERSE = "REVERSE";

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public Optional<Long> findResponseId(Long organizationId, String operation, String idempotencyKey) {
        if (isBlank(idempotencyKey) || organizationId == null) {
            return Optional.empty();
        }
        return idempotencyRecordRepository
                .findByOrganizationIdAndOperationAndIdempotencyKey(organizationId, operation, idempotencyKey.trim())
                .map(IdempotencyRecord::getResponseId);
    }

    public void remember(Long organizationId, Long userId, String operation, String idempotencyKey, Long responseId) {
        if (isBlank(idempotencyKey) || organizationId == null) {
            return;
        }
        IdempotencyRecord record = new IdempotencyRecord();
        record.setOrganizationId(organizationId);
        record.setUserId(userId);
        record.setOperation(operation);
        record.setIdempotencyKey(idempotencyKey.trim());
        record.setResponseId(responseId);
        record.setCreatedAt(Instant.now());
        idempotencyRecordRepository.save(record);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
