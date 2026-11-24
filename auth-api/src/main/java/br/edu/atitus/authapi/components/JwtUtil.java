package br.edu.atitus.authapi.components;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import br.edu.atitus.authapi.entities.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24; // 1000 milisegundos * 60 segundos * 60 minutos * 24 horas

    // A chave vem de fora do código (variável de ambiente APP_JWT_SECRET ou Consul)
    private final SecretKey signingKey;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32)
            throw new IllegalStateException("app.jwt.secret precisa ter pelo menos 32 caracteres");
        this.signingKey = Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String email, Long id, UserType type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", id);
        claims.put("email", email);
        claims.put("type", type.getCode());
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date()) // Data de emissão
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Expiração
                .signWith(signingKey) // Assina com a chave secreta
                .compact(); // Gera o token JWT
    }

    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey) // Define a chave para verificação
                    .build()
                    .parseSignedClaims(token) // Faz o parsing do token
                    .getPayload(); // Retorna as informações do token
        } catch (Exception e) {
            return null;
        }
    }

    public static String getJwtFromRequest(HttpServletRequest request) {
        String jwt = request.getHeader("Authorization");
        if (jwt == null || jwt.isEmpty())
            jwt = request.getHeader("authorization");
        if (jwt != null && !jwt.isEmpty()) {
            return jwt.substring(7);
        }
        return null;
    }

}
