package com.ros.ewallet.dto.mapper;

import com.ros.ewallet.dto.request.SignupRequest;
import com.ros.ewallet.domain.entity.Role;
import com.ros.ewallet.domain.enums.RoleType;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.service.RoleService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mapper used for mapping SignupRequest fields.
 */
@Mapper(componentModel = "spring",
        uses = {PasswordEncoder.class, RoleService.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public abstract class SignupRequestMapper {

    private PasswordEncoder passwordEncoder;
    private RoleService roleService;

    @Autowired
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Autowired
    public void setRoleService(RoleService roleService) {
        this.roleService = roleService;
    }

    @Mapping(target = "firstName", expression = "java(org.apache.commons.text.WordUtils.capitalizeFully(dto.getFirstName()))")
    @Mapping(target = "lastName", expression = "java(org.apache.commons.text.WordUtils.capitalizeFully(dto.getLastName()))")
    @Mapping(target = "username", expression = "java(dto.getUsername().trim().toLowerCase())")
    @Mapping(target = "email", expression = "java(dto.getEmail().trim().toLowerCase())")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", ignore = true)
    public abstract User toUser(SignupRequest dto);

    @AfterMapping
    void setToEntityFields(@MappingTarget User entity, SignupRequest dto) {
        entity.setPassword(passwordEncoder.encode(dto.getPassword()));
        // SEC-01: never honor client-supplied roles; signup always creates ROLE_USER only
        final List<Role> roles = roleService.getReferenceByTypeIsIn(Set.of(RoleType.ROLE_USER));
        entity.setRoles(new HashSet<>(roles));
    }
}
