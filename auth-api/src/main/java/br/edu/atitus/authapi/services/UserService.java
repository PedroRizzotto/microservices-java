package br.edu.atitus.authapi.services;

import br.edu.atitus.authapi.entities.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface UserService extends UserDetailsService {

    UserEntity save(UserEntity user) throws Exception;

    UserEntity updateName(Long userId, String name);

    void changePassword(Long userId, String currentPassword, String newPassword);
}
