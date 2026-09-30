package com.ros.ewallet.repository;

import com.ros.ewallet.domain.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByIban(String iban);

    /**
     * Pessimistic write lock to prevent concurrent overdraft / double-spend on the debit wallet.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.iban = :iban")
    Optional<Wallet> findByIbanForUpdate(@Param("iban") String iban);

    List<Wallet> findByUserId(Long userId);

    Page<Wallet> findByUserId(Long userId, Pageable pageable);

    List<Wallet> findByOrganizationId(Long organizationId);

    Page<Wallet> findByOrganizationId(Long organizationId, Pageable pageable);

    boolean existsByIbanIgnoreCase(String iban);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByOrganizationIdAndNameIgnoreCase(Long organizationId, String name);

    Wallet getReferenceByIban(String iban);
}
