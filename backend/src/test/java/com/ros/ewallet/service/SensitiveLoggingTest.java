package com.ros.ewallet.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ros.ewallet.config.MessageSourceConfig;
import com.ros.ewallet.config.TransactionLimitProperties;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.dto.mapper.TransactionRequestMapper;
import com.ros.ewallet.dto.mapper.TransactionResponseMapper;
import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.repository.TransactionRepository;
import com.ros.ewallet.security.SecurityAccess;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;

import static com.ros.ewallet.common.MessageKeys.INFO_TRANSACTION_CREATED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * SEC-14: INFO logs must not contain IBAN or balance values.
 */
@ExtendWith(MockitoExtension.class)
class SensitiveLoggingTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private MessageSourceConfig messageConfig;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionRequestMapper transactionRequestMapper;
    @Mock
    private TransactionResponseMapper transactionResponseMapper;
    @Mock
    private SecurityAccess securityAccess;
    @Mock
    private TransactionLimitProperties limitProperties;
    @Mock
    private Clock clock;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(TransactionService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void create_logsTransactionIdWithoutIbanOrAmount() {
        TransactionRequest request = new TransactionRequest();
        request.setAmount(new BigDecimal("99.50"));
        request.setFromWalletIban("TR330006100519786457841326");
        request.setToWalletIban("TR320010009999901234567890");

        Wallet from = new Wallet();
        from.setIban("TR330006100519786457841326");
        from.setBalance(new BigDecimal("500.00"));
        Wallet to = new Wallet();
        to.setIban("TR320010009999901234567890");
        to.setBalance(new BigDecimal("100.00"));

        Transaction transaction = new Transaction();
        transaction.setId(42L);
        transaction.setFromWallet(from);
        transaction.setToWallet(to);
        transaction.setAmount(new BigDecimal("99.50"));

        when(transactionRequestMapper.toTransaction(request)).thenReturn(transaction);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(messageConfig.getMessage(INFO_TRANSACTION_CREATED, 42L))
                .thenReturn("Transaction is created (transactionId: 42)");

        transactionService.create(request);

        assertFalse(appender.list.isEmpty());
        String joined = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + " " + b);
        assertTrue(joined.contains("transactionId: 42"));
        assertFalse(joined.contains("TR330006100519786457841326"));
        assertFalse(joined.contains("TR320010009999901234567890"));
        assertFalse(joined.contains("99.50"));
        assertFalse(joined.contains("500.00"));
    }
}
