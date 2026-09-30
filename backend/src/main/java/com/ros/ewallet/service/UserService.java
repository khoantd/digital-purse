package com.ros.ewallet.service;

import com.ros.ewallet.domain.entity.User;
import com.ros.ewallet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service used for User related operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Fetches a single user reference (entity) by the given id.
     *
     * @param id
     * @return User
     */
    public User getReferenceById(long id) {
        return userRepository.getReferenceById(id);
    }
}
