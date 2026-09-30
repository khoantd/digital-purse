package com.ros.ewallet.service;

import com.ros.ewallet.domain.entity.IdempotencyRecord;
import com.ros.ewallet.repository.IdempotencyRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Test
    void findResponseId_returnsEmptyWhenKeyBlank() {
        assertTrue(idempotencyService.findResponseId(10L, "TRANSFER", null).isEmpty());
        assertTrue(idempotencyService.findResponseId(10L, "TRANSFER", "  ").isEmpty());
        verifyNoInteractions(idempotencyRecordRepository);
    }

    @Test
    void findResponseId_returnsStoredId() {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setResponseId(99L);
        when(idempotencyRecordRepository.findByOrganizationIdAndOperationAndIdempotencyKey(10L, "TRANSFER", "abc"))
                .thenReturn(Optional.of(record));

        Optional<Long> result = idempotencyService.findResponseId(10L, "TRANSFER", "abc");

        assertTrue(result.isPresent());
        assertEquals(99L, result.get());
    }

    @Test
    void remember_skipsWhenKeyBlank() {
        idempotencyService.remember(10L, 1L, "TRANSFER", null, 5L);
        verify(idempotencyRecordRepository, never()).save(any());
    }

    @Test
    void remember_persistsWhenKeyPresent() {
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        idempotencyService.remember(10L, 1L, "ADD_FUNDS", "key-1", 7L);

        verify(idempotencyRecordRepository).save(argThat(r ->
                r.getOrganizationId().equals(10L)
                        && r.getUserId().equals(1L)
                        && "ADD_FUNDS".equals(r.getOperation())
                        && "key-1".equals(r.getIdempotencyKey())
                        && r.getResponseId().equals(7L)
                        && r.getCreatedAt() != null
        ));
    }
}
