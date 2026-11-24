package br.edu.atitus.authapi.controllers;

import br.edu.atitus.authapi.components.JwtUtil;
import br.edu.atitus.authapi.dtos.MeResponse;
import br.edu.atitus.authapi.dtos.SigninRequestDto;
import br.edu.atitus.authapi.dtos.SigninResponseDto;
import br.edu.atitus.authapi.dtos.SignupRequestDto;
import br.edu.atitus.authapi.entities.UserEntity;
import br.edu.atitus.authapi.entities.UserType;
import br.edu.atitus.authapi.services.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationConfiguration authConfig;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, AuthenticationConfiguration authConfig, JwtUtil jwtUtil) {
        this.userService = userService;
        this.authConfig = authConfig;
        this.jwtUtil = jwtUtil;
    }

    private UserEntity convertDTO2Entity(SignupRequestDto dto) {
        var user = new UserEntity();
        BeanUtils.copyProperties(dto, user);
        return user;
    }

    private MeResponse toResponse(UserEntity user) {
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getType().getCode());
    }

    @PostMapping("/signup")
    public ResponseEntity<MeResponse> signup(@RequestBody SignupRequestDto dto) throws Exception {
        var user = convertDTO2Entity(dto);
        user.setType(UserType.Common);
        userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/signup/provider")
    public ResponseEntity<MeResponse> signupProvider(@RequestBody SignupRequestDto dto) throws Exception {
        var user = convertDTO2Entity(dto);
        user.setType(UserType.Provider);
        userService.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(user));
    }

    @PostMapping("/signin")
    public ResponseEntity<SigninResponseDto> PostSignin(@RequestBody SigninRequestDto signin) throws Exception {
        authConfig.getAuthenticationManager()
                .authenticate(new UsernamePasswordAuthenticationToken(signin.email(), signin.password()));
        UserEntity user = (UserEntity) userService.loadUserByUsername(signin.email());
        SigninResponseDto response = new SigninResponseDto(
                toResponse(user),
                jwtUtil.generateToken(user.getEmail(), user.getId(), user.getType()));
        return ResponseEntity.ok(response);

    }
}
