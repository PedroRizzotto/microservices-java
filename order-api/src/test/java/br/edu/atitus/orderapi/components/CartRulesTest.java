package br.edu.atitus.orderapi.components;

import br.edu.atitus.orderapi.entities.CartEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartRulesTest {

    @Test
    void adicionaNoCarrinhoVazioAssumeLoja() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        assertEquals(1L, cart.getEstablishmentId());
        assertEquals(1, cart.getItems().size());
        assertEquals(10L, cart.getItems().get(0).getProductId());
        assertEquals(2, cart.getItems().get(0).getQuantity());
    }

    @Test
    void somaProdutoRepetido() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        CartRules.addItem(cart, 10L, 1L, 3, false);
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
    }

    @Test
    void recusaOutraLojaSemReplace() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        var ex = assertThrows(ResponseStatusException.class, () -> CartRules.addItem(cart, 20L, 2L, 1, false));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("O carrinho tem produtos de outra loja", ex.getReason());
        assertEquals(1L, cart.getEstablishmentId());
        assertEquals(1, cart.getItems().size());
        assertEquals(10L, cart.getItems().get(0).getProductId());
    }

    @Test
    void substituiComReplace() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        CartRules.addItem(cart, 20L, 2L, 4, true);
        assertEquals(2L, cart.getEstablishmentId());
        assertEquals(1, cart.getItems().size());
        assertEquals(20L, cart.getItems().get(0).getProductId());
        assertEquals(4, cart.getItems().get(0).getQuantity());
    }

    @Test
    void recusaQuantidadeInvalida() {
        CartEntity cart = new CartEntity();
        for (int q : new int[]{0, -1}) {
            var ex = assertThrows(ResponseStatusException.class, () -> CartRules.addItem(cart, 10L, 1L, q, false));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals("Quantidade inválida", ex.getReason());
        }
        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getEstablishmentId());
    }

    @Test
    void setQuantityAtualizaQuantidade() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        CartRules.setQuantity(cart, 10L, 7);
        assertEquals(7, cart.getItems().get(0).getQuantity());
    }

    @Test
    void setQuantityZeroRemoveELiberaLoja() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        CartRules.setQuantity(cart, 10L, 0);
        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getEstablishmentId());
    }

    @Test
    void setQuantityNegativaRecusa() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        var ex = assertThrows(ResponseStatusException.class, () -> CartRules.setQuantity(cart, 10L, -1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void setQuantityItemAusente404() {
        CartEntity cart = new CartEntity();
        CartRules.addItem(cart, 10L, 1L, 2, false);
        var ex = assertThrows(ResponseStatusException.class, () -> CartRules.setQuantity(cart, 99L, 1));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Item não está no carrinho", ex.getReason());
    }
}
