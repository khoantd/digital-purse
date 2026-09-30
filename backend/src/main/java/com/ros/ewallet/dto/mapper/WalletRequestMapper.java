package com.ros.ewallet.dto.mapper;

import com.ros.ewallet.dto.request.WalletRequest;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.service.UserService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Mapper used for mapping WalletRequest fields.
 * Entity id is never taken from the client request.
 */
@Mapper(componentModel = "spring",
        uses = {UserService.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class WalletRequestMapper {

    private UserService userService;

    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currency", ignore = true)
    @Mapping(target = "name", expression = "java(org.apache.commons.text.WordUtils.capitalizeFully(dto.getName()))")
    @Mapping(target = "iban", expression = "java(org.apache.commons.lang3.StringUtils.upperCase(dto.getIban()))")
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "ownerType", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "fromTransactions", ignore = true)
    @Mapping(target = "toTransactions", ignore = true)
    public abstract Wallet toWallet(WalletRequest dto);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "customerId", source = "customer.id")
    public abstract WalletRequest toWalletRequest(Wallet entity);

    @AfterMapping
    void setToEntityFields(@MappingTarget Wallet entity, WalletRequest dto) {
        entity.setUser(userService.getReferenceById(dto.getUserId()));
        entity.setCurrency(com.ros.ewallet.common.Constants.CURRENCY_VND);
    }
}
