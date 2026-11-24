package br.edu.atitus.authapi.servicesimpl;

import br.edu.atitus.authapi.components.Validator;
import br.edu.atitus.authapi.entities.UserEntity;
import br.edu.atitus.authapi.repositories.UserRepository;
import br.edu.atitus.authapi.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserServiceJpa implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    public UserServiceJpa(UserRepository userRepository, PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    private void validate(UserEntity user) throws Exception {
        if (user.getName() == null || user.getName().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome informado inválido");
        if (user.getEmail() == null || user.getEmail().isEmpty() || !Validator.validateEmail(user.getEmail()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail informado inválido");
        if (user.getPassword() == null || user.getPassword().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha informada inválida");

        if (user.getId() != null) {
            if (userRepository.existsByEmailAndIdNot(user.getEmail(), user.getId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe usuário com este e-mail");
        } else {
            if (userRepository.existsByEmail(user.getEmail()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Já existe usuário com este e-mail");
        }
        // TODO validar se usuário tem permissão para o tipo escolhido
    }

    private void format(UserEntity user) throws Exception {
        user.setPassword(encoder.encode(user.getPassword()));
    }

    @Override
    public UserEntity save(UserEntity user) throws Exception {
        if (user == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Objeto nulo");
        validate(user);
        format(user);
        return userRepository.save(user);
    }

    @Override
    public UserEntity updateName(Long userId, String name) {
        if (name == null || name.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome informado inválido");
        UserEntity user = findUser(userId);
        user.setName(name.trim());
        return userRepository.save(user);
    }

    @Override
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nova senha informada inválida");
        UserEntity user = findUser(userId);
        if (currentPassword == null || !encoder.matches(currentPassword, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com este e-mail"));
        return user;
    }
}
