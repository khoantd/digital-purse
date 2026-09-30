package com.ros.ewallet.dto.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ros.ewallet.domain.entity.Role;
import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.domain.enums.RoleType;
import com.ros.ewallet.dto.request.SignupRequest;
import com.ros.ewallet.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SignupRequestMapperTest {

    private SignupRequestMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @BeforeEach
    void setUp() {
        mapper = new SignupRequestMapperImpl();
        mapper.setPasswordEncoder(passwordEncoder);
        mapper.setRoleService(roleService);
    }

    @Test
    void toUser_shouldAssignOnlyRoleUserWhenClientSendsAdminInPayload() throws Exception {
        // Simulate attack: client POSTs roles including ROLE_ADMIN (unknown field ignored by DTO)
        String maliciousJson = """
                {
                  "firstName": "Alice",
                  "lastName": "Admin",
                  "username": "alice",
                  "email": "alice@example.com",
                  "password": "Str0ng!Passw0rd",
                  "roles": ["ROLE_ADMIN"]
                }
                """;
        SignupRequest request = objectMapper.readValue(maliciousJson, SignupRequest.class);
        var userRole = createRole(2L, RoleType.ROLE_USER);

        when(passwordEncoder.encode("Str0ng!Passw0rd")).thenReturn("encoded");
        when(roleService.getReferenceByTypeIsIn(any())).thenReturn(List.of(userRole));

        User user = mapper.toUser(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<RoleType>> typesCaptor = ArgumentCaptor.forClass(Set.class);
        verify(roleService).getReferenceByTypeIsIn(typesCaptor.capture());

        assertEquals(Set.of(RoleType.ROLE_USER), typesCaptor.getValue());
        assertEquals(1, user.getRoles().size());
        assertTrue(user.getRoles().stream().allMatch(r -> r.getType() == RoleType.ROLE_USER));
        assertFalse(user.getRoles().stream().anyMatch(r -> r.getType() == RoleType.ROLE_ADMIN));
    }

    private Role createRole(Long id, RoleType type) {
        var role = new Role();
        role.setId(id);
        role.setType(type);
        return role;
    }
}
