package com.ros.ewallet.dto.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.enums.Status;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.service.TypeService;
import com.ros.ewallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * SEC-06: server-controlled transaction fields must ignore client-supplied values.
 */
@ExtendWith(MockitoExtension.class)
class TransactionRequestMapperTest {

    private TransactionRequestMapper mapper;
    private ObjectMapper objectMapper;

    @Mock
    private WalletService walletService;

    @Mock
    private TypeService typeService;

    @BeforeEach
    void setUp() {
        mapper = new TransactionRequestMapperImpl();
        mapper.setWalletService(walletService);
        mapper.setTypeService(typeService);
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void toTransaction_ignoresClientIdStatusReferenceAndCreatedAt() throws Exception {
        String maliciousJson = """
                {
                  "id": 999,
                  "amount": 50.00,
                  "description": "hack",
                  "createdAt": "2020-01-01T00:00:00Z",
                  "referenceNumber": "00000000-0000-0000-0000-000000000001",
                  "status": "PENDING",
                  "fromWalletIban": "TR330006100519786457841326",
                  "toWalletIban": "TR320010009999901234567890",
                  "typeId": 1
                }
                """;
        TransactionRequest request = objectMapper.readValue(maliciousJson, TransactionRequest.class);

        when(walletService.getByIban(anyString())).thenReturn(null);
        when(typeService.getReferenceById(anyLong())).thenReturn(null);

        Instant before = Instant.now().minusSeconds(1);
        Transaction entity = mapper.toTransaction(request);
        Instant after = Instant.now().plusSeconds(1);

        assertNull(entity.getId());
        assertEquals(Status.SUCCESS, entity.getStatus());
        assertNotNull(entity.getReferenceNumber());
        assertNotEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), entity.getReferenceNumber());
        assertTrue(!entity.getCreatedAt().isBefore(before) && !entity.getCreatedAt().isAfter(after));
        assertEquals(0, new BigDecimal("50.00").compareTo(entity.getAmount()));
        assertEquals("hack", entity.getDescription());
    }
}
