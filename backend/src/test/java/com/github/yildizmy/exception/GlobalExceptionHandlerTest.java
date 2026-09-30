package com.github.yildizmy.exception;

import com.github.yildizmy.config.MessageSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.WebRequest;

import static com.github.yildizmy.common.MessageKeys.ERROR_UNKNOWN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * SEC-11: when exception.trace is false, responses never include stack traces or 5xx internals
 * even if the client passes {@code ?trace=true}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSourceConfig messageConfig;

    @Mock
    private WebRequest request;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageConfig);
        ReflectionTestUtils.setField(handler, "printStackTrace", false);
    }

    @Test
    void uncaughtException_doesNotExposeStackOrInternalMessage_evenWithTraceParam() {
        when(messageConfig.getMessage(anyString(), any())).thenReturn("ignored");
        when(messageConfig.getMessage(ERROR_UNKNOWN)).thenReturn("Unknown error occurred");
        when(request.getParameterValues("trace")).thenReturn(new String[]{"true"});

        ResponseEntity<Object> response = handler.handleAllUncaughtException(
                new RuntimeException("secret JDBC connection string leak"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals("Unknown error occurred", body.getMessage());
        assertNull(body.getStackTrace());
        assertNotNull(body.getCorrelationId());
        assertFalse(body.getCorrelationId().isBlank());
    }

    @Test
    void operationalException_keepsSafeMessage_withoutStack() {
        ResponseEntity<Object> response = handler.handleNoSuchElementFoundException(
                new NoSuchElementFoundException("Requested wallet is not found"), request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertEquals("Requested wallet is not found", body.getMessage());
        assertNull(body.getStackTrace());
        assertNotNull(body.getCorrelationId());
    }

    @Test
    void whenTraceEnabled_stackIsIncludedOnlyWithTraceParam() {
        ReflectionTestUtils.setField(handler, "printStackTrace", true);
        when(request.getParameterValues("trace")).thenReturn(new String[]{"true"});

        ResponseEntity<Object> response = handler.handleNoSuchElementFoundException(
                new NoSuchElementFoundException("not found"), request);

        ErrorResponse body = (ErrorResponse) response.getBody();
        assertNotNull(body);
        assertNotNull(body.getStackTrace());
        assertTrue(body.getStackTrace().contains("NoSuchElementFoundException"));
    }
}
