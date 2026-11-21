package br.edu.atitus.authapi.components;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import br.edu.atitus.authapi.dtos.AddressRequest;
import br.edu.atitus.authapi.entities.AddressEntity;

class AddressRulesTest {
    private static AddressRequest valido() {
        return new AddressRequest("Casa", "99010-000", " Rua A ", "10", null, "Centro", "Passo Fundo", "rs");
    }

    @Test
    void normalizaCepEUf() {
        AddressRequest r = AddressRules.normalize(valido());
        assertEquals("99010000", r.zipCode());
        assertEquals("RS", r.state());
        assertEquals("Rua A", r.street());
    }

    @Test
    void aceitaEnderecoValido() {
        assertDoesNotThrow(() -> AddressRules.validate(AddressRules.normalize(valido())));
    }

    @Test
    void recusaCepComTamanhoErrado() {
        var r = AddressRules.normalize(new AddressRequest("Casa", "9901", "Rua A", "10", null, "Centro", "Passo Fundo", "RS"));
        var e = assertThrows(ResponseStatusException.class, () -> AddressRules.validate(r));
        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    void recusaUfInvalida() {
        var r = AddressRules.normalize(new AddressRequest("Casa", "99010000", "Rua A", "10", null, "Centro", "Passo Fundo", "RGS"));
        assertThrows(ResponseStatusException.class, () -> AddressRules.validate(r));
    }

    @Test
    void recusaCamposObrigatoriosVazios() {
        var r = AddressRules.normalize(new AddressRequest("Casa", "99010000", " ", "10", null, "Centro", "Passo Fundo", "RS"));
        assertThrows(ResponseStatusException.class, () -> AddressRules.validate(r));
    }

    @Test
    void recusaCamposLongos() {
        var label = AddressRules.normalize(new AddressRequest("x".repeat(51), "99010000", "Rua A", "10", null, "Centro", "Passo Fundo", "RS"));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> AddressRules.validate(label)).getStatusCode().value());
        var numero = AddressRules.normalize(new AddressRequest("Casa", "99010000", "Rua A", "1".repeat(21), null, "Centro", "Passo Fundo", "RS"));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> AddressRules.validate(numero)).getStatusCode().value());
    }

    @Test
    void requireOwnerRecusaOutroUsuario() {
        AddressEntity a = new AddressEntity();
        a.setUserId(7L);
        var e = assertThrows(ResponseStatusException.class, () -> AddressRules.requireOwner(a, 8L));
        assertEquals(404, e.getStatusCode().value());
        assertDoesNotThrow(() -> AddressRules.requireOwner(a, 7L));
        assertThrows(ResponseStatusException.class, () -> AddressRules.requireOwner(null, 7L));
    }
}
