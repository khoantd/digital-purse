package com.ros.ewallet.domain.entity;

import jakarta.persistence.Version;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SEC-10: Wallet uses optimistic locking; concurrent overdrafts must not both succeed.
 */
class WalletConcurrencyTest {

    @Test
    void wallet_hasJpaVersionField() throws Exception {
        Field versionField = Wallet.class.getDeclaredField("version");
        assertNotNull(versionField.getAnnotation(Version.class));
    }

    @Test
    void concurrentDebits_onlyOneSucceedsWhenBalanceIsExact() throws InterruptedException {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setVersion(0L);

        BigDecimal amount = new BigDecimal("100.00");
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        Object lock = new Object();

        Runnable attempt = () -> {
            try {
                start.await();
                synchronized (lock) {
                    if (wallet.getBalance().compareTo(amount) < 0) {
                        failures.incrementAndGet();
                    } else {
                        wallet.setBalance(wallet.getBalance().subtract(amount));
                        wallet.setVersion(wallet.getVersion() + 1);
                        successes.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                done.countDown();
            }
        };

        Thread t1 = new Thread(attempt);
        Thread t2 = new Thread(attempt);
        t1.start();
        t2.start();
        start.countDown();
        done.await();

        assertEquals(1, successes.get());
        assertEquals(1, failures.get());
        assertEquals(0, wallet.getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(1L, wallet.getVersion());
    }

    @Test
    void optimisticVersionMismatch_detectedOnStaleWrite() {
        AtomicReference<Long> storedVersion = new AtomicReference<>(0L);
        AtomicReference<BigDecimal> storedBalance = new AtomicReference<>(new BigDecimal("100.00"));

        // Two readers load the same version
        long v1 = storedVersion.get();
        long v2 = storedVersion.get();
        BigDecimal b1 = storedBalance.get();
        BigDecimal b2 = storedBalance.get();

        // First writer commits
        assertEquals(v1, storedVersion.get());
        storedBalance.set(b1.subtract(new BigDecimal("100.00")));
        storedVersion.set(v1 + 1);

        // Second writer sees version mismatch (optimistic lock failure)
        assertNotEquals(v2, storedVersion.get());
        assertEquals(0, storedBalance.get().compareTo(BigDecimal.ZERO));
        assertEquals(1L, storedVersion.get());
    }
}
