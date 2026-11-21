package br.edu.atitus.productapi.components;

import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.entities.ProductEntity;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductRulesTest {

    private static ProductRequest valido() {
        return new ProductRequest("Ração Golden 15kg", "Golden", null, "Ração para cães adultos", "racao", "BRL", 189.9, 10, null);
    }

    @Test
    void aceitaProdutoValido() {
        assertDoesNotThrow(() -> ProductRules.validate(valido()));
    }

    @Test
    void recusaPrecoZero() {
        var r = new ProductRequest("Ração", null, null, "desc", null, "BRL", 0, 1, null);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> ProductRules.validate(r)).getStatusCode().value());
    }

    @Test
    void recusaEstoqueNegativo() {
        var r = new ProductRequest("Ração", null, null, "desc", null, "BRL", 10, -1, null);
        assertThrows(ResponseStatusException.class, () -> ProductRules.validate(r));
    }

    @Test
    void recusaNomeVazio() {
        var r = new ProductRequest(" ", null, null, "desc", null, "BRL", 10, 1, null);
        assertThrows(ResponseStatusException.class, () -> ProductRules.validate(r));
    }

    @Test
    void requireOwnerRecusaOutroPrestador() {
        ProductEntity p = new ProductEntity();
        p.setProviderId(2L);
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> ProductRules.requireOwner(p, 3L)).getStatusCode().value());
        assertDoesNotThrow(() -> ProductRules.requireOwner(p, 2L));
    }
}
