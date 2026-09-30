package com.ros.ewallet.dto.mapper;

import com.ros.ewallet.dto.request.TransactionRequest;
import com.ros.ewallet.domain.entity.Transaction;
import com.ros.ewallet.service.TypeService;
import com.ros.ewallet.service.WalletService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

/**
 * Mapper used for mapping TransactionRequest fields.
 * id / status / referenceNumber / createdAt are always server-generated.
 */
@Mapper(componentModel = "spring",
        uses = {WalletService.class, TypeService.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class TransactionRequestMapper {

    private WalletService walletService;
    private TypeService typeService;

    @Autowired
    public void setWalletService(@Lazy WalletService walletService) {
        this.walletService = walletService;
    }

    @Autowired
    public void setTypeService(TypeService typeService) {
        this.typeService = typeService;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", expression = "java(com.ros.ewallet.domain.enums.Status.SUCCESS)")
    @Mapping(target = "referenceNumber", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "fromWallet", ignore = true)
    @Mapping(target = "toWallet", ignore = true)
    @Mapping(target = "type", ignore = true)
    public abstract Transaction toTransaction(TransactionRequest dto);

    @Mapping(target = "fromWalletIban", source = "fromWallet.iban")
    @Mapping(target = "toWalletIban", source = "toWallet.iban")
    @Mapping(target = "typeId", source = "type.id")
    public abstract TransactionRequest toTransactionRequest(Transaction entity);

    @AfterMapping
    void setToEntityFields(@MappingTarget Transaction entity, TransactionRequest dto) {
        entity.setFromWallet(walletService.getByIban(dto.getFromWalletIban()));
        entity.setToWallet(walletService.getByIban(dto.getToWalletIban()));
        entity.setType(typeService.getReferenceById(dto.getTypeId()));
    }
}
