package com.github.yildizmy.service;

import com.github.yildizmy.config.MessageSourceConfig;
import com.github.yildizmy.domain.entity.Wallet;
import com.github.yildizmy.dto.mapper.WalletRequestMapper;
import com.github.yildizmy.dto.mapper.WalletResponseMapper;
import com.github.yildizmy.dto.mapper.WalletTransactionRequestMapper;
import com.github.yildizmy.dto.request.TransactionRequest;
import com.github.yildizmy.dto.request.WalletRequest;
import com.github.yildizmy.dto.response.CommandResponse;
import com.github.yildizmy.dto.response.WalletResponse;
import com.github.yildizmy.exception.ElementAlreadyExistsException;
import com.github.yildizmy.exception.InsufficientFundsException;
import com.github.yildizmy.exception.NoSuchElementFoundException;
import com.github.yildizmy.repository.WalletRepository;
import com.github.yildizmy.security.SecurityAccess;
import com.github.yildizmy.validator.IbanValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.github.yildizmy.common.MessageKeys.*;

/**
 * Service used for Wallet related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final MessageSourceConfig messageConfig;
    private final WalletRepository walletRepository;
    private final TransactionService transactionService;
    private final WalletRequestMapper walletRequestMapper;
    private final WalletResponseMapper walletResponseMapper;
    private final WalletTransactionRequestMapper walletTransactionRequestMapper;
    private final IbanValidator ibanValidator;
    private final SecurityAccess securityAccess;

    /**
     * Fetches a single wallet by the given id.
     *
     * @param id
     * @return WalletResponse
     */
    @Transactional(readOnly = true)
    public WalletResponse findById(long id) {
        final Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOwnerOrAdmin(wallet);
        return walletResponseMapper.toWalletResponse(wallet);
    }

    /**
     * Fetches a single wallet by the given iban.
     *
     * @param iban
     * @return WalletResponse
     */
    @Transactional(readOnly = true)
    public WalletResponse findByIban(String iban) {
        final Wallet wallet = walletRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOwnerOrAdmin(wallet);
        return walletResponseMapper.toWalletResponse(wallet);
    }

    /**
     * Fetches wallets for the given userId (caller must be that user or admin).
     *
     * @param userId
     * @return WalletResponse
     */
    @Transactional(readOnly = true)
    public List<WalletResponse> findByUserId(long userId) {
        securityAccess.requireSelfOrAdmin(userId);
        return walletRepository.findByUserId(userId).stream()
                .map(walletResponseMapper::toWalletResponse)
                .toList();
    }

    /**
     * Fetches a single wallet reference (entity) by the given iban.
     *
     * @param iban
     * @return Wallet
     */
    public Wallet getByIban(String iban) {
        return walletRepository.findByIban(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
    }

    /**
     * Fetches wallets based on paging. Non-admins only see their own wallets.
     *
     * @param pageable
     * @return List of WalletResponse
     */
    @Transactional(readOnly = true)
    public Page<WalletResponse> findAll(Pageable pageable) {
        final Page<Wallet> wallets = securityAccess.isAdmin()
                ? walletRepository.findAll(pageable)
                : walletRepository.findByUserId(securityAccess.currentUser().getId(), pageable);
        if (wallets.isEmpty())
            throw new NoSuchElementFoundException(messageConfig.getMessage(ERROR_NO_RECORDS));
        return wallets.map(walletResponseMapper::toWalletResponse);
    }

    /**
     * Creates a new wallet using the given request parameters.
     * Owner is always the authenticated user (client userId is ignored).
     *
     * @param request
     * @return id of the created wallet
     */
    @Transactional
    public CommandResponse create(WalletRequest request) {
        final Long ownerId = securityAccess.currentUser().getId();
        request.setUserId(ownerId);

        if (walletRepository.existsByIbanIgnoreCase(request.getIban()))
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_IBAN_EXISTS));
        if (walletRepository.existsByUserIdAndNameIgnoreCase(ownerId, request.getName()))
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_NAME_EXISTS));

        ibanValidator.isValid(request.getIban(), null);

        final Wallet wallet = walletRequestMapper.toWallet(request);
        walletRepository.save(wallet);
        log.info(messageConfig.getMessage(INFO_WALLET_CREATED, wallet.getId()));

        // add this initial amount to the transactions
        transactionService.create(walletTransactionRequestMapper.toTransactionRequest(request));

        return CommandResponse.builder().id(wallet.getId()).build();
    }

    /**
     * Transfer funds between wallets.
     * Debit wallet is locked pessimistically to prevent double-spend races.
     *
     * @param request
     * @return id of the transaction
     */
    @Transactional
    public CommandResponse transferFunds(TransactionRequest request) {
        final Wallet fromWallet = getByIbanForUpdate(request.getFromWalletIban());
        final Wallet toWallet = getByIban(request.getToWalletIban());
        securityAccess.requireWalletOwnerOrAdmin(fromWallet);

        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0)
            throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));

        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));
        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));

        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_BALANCES_UPDATED, fromWallet.getId(), toWallet.getId()));

        final CommandResponse response = transactionService.create(request);
        return CommandResponse.builder().id(response.id()).build();
    }

    /**
     * Adds funds to the given wallet.
     *
     * @param request
     * @return id of the transaction
     */
    @Transactional
    public CommandResponse addFunds(TransactionRequest request) {
        final Wallet toWallet = getByIbanForUpdate(request.getToWalletIban());
        securityAccess.requireWalletOwnerOrAdmin(toWallet);

        toWallet.setBalance(toWallet.getBalance().add(request.getAmount()));

        walletRepository.save(toWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_BALANCE_UPDATED, toWallet.getId()));

        final CommandResponse response = transactionService.create(request);
        return CommandResponse.builder().id(response.id()).build();
    }

    /**
     * Withdraw funds from the given wallet.
     * Debit wallet is locked pessimistically to prevent double-spend races.
     *
     * @param request
     * @return id of the transaction
     */
    @Transactional
    public CommandResponse withdrawFunds(TransactionRequest request) {
        final Wallet fromWallet = getByIbanForUpdate(request.getFromWalletIban());
        securityAccess.requireWalletOwnerOrAdmin(fromWallet);

        if (fromWallet.getBalance().compareTo(request.getAmount()) < 0)
            throw new InsufficientFundsException(messageConfig.getMessage(ERROR_INSUFFICIENT_FUNDS));

        fromWallet.setBalance(fromWallet.getBalance().subtract(request.getAmount()));

        walletRepository.save(fromWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_BALANCE_UPDATED, fromWallet.getId()));

        final CommandResponse response = transactionService.create(request);
        return CommandResponse.builder().id(response.id()).build();
    }

    /**
     * Updates wallet name/iban only. Balance, owner, and id are not client-controllable.
     *
     * @param request
     * @return id of the updated wallet
     */
    @Transactional
    public CommandResponse update(long id, WalletRequest request) {
        final Wallet foundWallet = walletRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOwnerOrAdmin(foundWallet);

        final Long ownerId = foundWallet.getUser().getId();

        if (!request.getIban().equalsIgnoreCase(foundWallet.getIban()) &&
                walletRepository.existsByIbanIgnoreCase(request.getIban()))
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_IBAN_EXISTS));

        if (!request.getName().equalsIgnoreCase(foundWallet.getName()) &&
                walletRepository.existsByUserIdAndNameIgnoreCase(ownerId, request.getName()))
            throw new ElementAlreadyExistsException(messageConfig.getMessage(ERROR_WALLET_NAME_EXISTS));

        ibanValidator.isValid(request.getIban(), null);

        foundWallet.setIban(org.apache.commons.lang3.StringUtils.upperCase(request.getIban()));
        foundWallet.setName(org.apache.commons.text.WordUtils.capitalizeFully(request.getName()));
        walletRepository.save(foundWallet);
        log.info(messageConfig.getMessage(INFO_WALLET_UPDATED, foundWallet.getId()));
        return CommandResponse.builder().id(id).build();
    }

    /**
     * Fetches a wallet by IBAN with a pessimistic write lock (must be called inside a transaction).
     */
    public Wallet getByIbanForUpdate(String iban) {
        return walletRepository.findByIbanForUpdate(iban)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
    }

    /**
     * Deletes wallet by the given id.
     *
     * @param id
     */
    @Transactional
    public void deleteById(long id) {
        final Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementFoundException(messageConfig.getMessage(ERROR_WALLET_NOT_FOUND)));
        securityAccess.requireWalletOwnerOrAdmin(wallet);
        walletRepository.delete(wallet);
        log.info(messageConfig.getMessage(INFO_WALLET_DELETED, wallet.getId()));
    }
}
