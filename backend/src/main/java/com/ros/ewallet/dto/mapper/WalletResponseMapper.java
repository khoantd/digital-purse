package com.ros.ewallet.dto.mapper;

import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.entity.Wallet;
import com.ros.ewallet.dto.response.UserResponse;
import com.ros.ewallet.dto.response.WalletResponse;
import com.ros.ewallet.service.IbanGenerator;
import com.ros.ewallet.service.VietQrGenerator;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.MessageFormat;

/**
 * Mapper used for mapping WalletResponse fields.
 */
@Mapper(componentModel = "spring")
public abstract class WalletResponseMapper {

    private VietQrGenerator vietQrGenerator;
    private IbanGenerator ibanGenerator;

    @Autowired
    public void setVietQrGenerator(VietQrGenerator vietQrGenerator) {
        this.vietQrGenerator = vietQrGenerator;
    }

    @Autowired
    public void setIbanGenerator(IbanGenerator ibanGenerator) {
        this.ibanGenerator = ibanGenerator;
    }

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "fromTransactions", ignore = true)
    @Mapping(target = "toTransactions", ignore = true)
    public abstract Wallet toWallet(WalletResponse dto);

    @Mapping(target = "vietQrPayload", ignore = true)
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.name")
    public abstract WalletResponse toWalletResponse(Wallet entity);

    @AfterMapping
    void setFullName(@MappingTarget UserResponse dto, User entity) {
        dto.setFullName(MessageFormat.format("{0} {1}", entity.getFirstName(), entity.getLastName()));
    }

    @AfterMapping
    void enrichReceivePayload(@MappingTarget WalletResponse dto, Wallet entity) {
        if (entity.getIban() == null || entity.getIban().length() < 10) {
            return;
        }
        String account = ibanGenerator.accountNumber(entity.getIban());
        dto.setVietQrPayload(vietQrGenerator.buildStatic(
                IbanGenerator.BANK_BIN, account, entity.getName()));
    }
}
