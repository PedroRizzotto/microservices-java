package br.edu.atitus.gatewayapi.components;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    // Mesma chave do auth-api, vinda de fora do código (APP_JWT_SECRET ou Consul)
    private final SecretKey signingKey;

    public JwtUtil(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32)
            throw new IllegalStateException("app.jwt.secret precisa ter pelo menos 32 caracteres");
        this.signingKey = Keys.hmacShaKeyFor(bytes);
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
}
