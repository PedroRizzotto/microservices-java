package br.edu.atitus.orderapi.components;

import br.edu.atitus.orderapi.entities.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static br.edu.atitus.orderapi.entities.OrderStatus.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderStatusRulesTest {

    private static void assertConflito(String mensagem, Runnable acao) {
        var ex = assertThrows(ResponseStatusException.class, acao::run);
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals(mensagem, ex.getReason());
    }

    @Test
    void transicoesValidasDoPrestador() {
        assertDoesNotThrow(() -> OrderStatusRules.requireProviderTransition(PAID, SHIPPED));
        assertDoesNotThrow(() -> OrderStatusRules.requireProviderTransition(SHIPPED, DELIVERED));
    }

    @Test
    void recusaPularStatus() {
        assertConflito("Transição de status inválida", () -> OrderStatusRules.requireProviderTransition(PAID, DELIVERED));
        assertConflito("Transição de status inválida", () -> OrderStatusRules.requireProviderTransition(PENDING_PAYMENT, SHIPPED));
    }

    @Test
    void recusaVoltarStatus() {
        assertConflito("Transição de status inválida", () -> OrderStatusRules.requireProviderTransition(SHIPPED, PAID));
        assertConflito("Transição de status inválida", () -> OrderStatusRules.requireProviderTransition(DELIVERED, SHIPPED));
        assertConflito("Transição de status inválida", () -> OrderStatusRules.requireProviderTransition(CANCELED, PAID));
    }

    @Test
    void cancelaSoPendente() {
        assertDoesNotThrow(() -> OrderStatusRules.requireCancelable(PENDING_PAYMENT));
        for (OrderStatus s : new OrderStatus[]{PAID, SHIPPED, DELIVERED, CANCELED})
            assertConflito("Pedido não pode ser cancelado", () -> OrderStatusRules.requireCancelable(s));
    }

    @Test
    void pagaSoPendente() {
        assertDoesNotThrow(() -> OrderStatusRules.requirePayable(PENDING_PAYMENT));
        for (OrderStatus s : new OrderStatus[]{PAID, SHIPPED, DELIVERED, CANCELED})
            assertConflito("Pedido não está aguardando pagamento", () -> OrderStatusRules.requirePayable(s));
    }
}
