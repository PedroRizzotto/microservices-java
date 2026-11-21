package br.edu.atitus.paymentapi.components;

import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentRulesTest {

    private static PaymentRequest request(String referenceType, Long referenceId, String method, double amount) {
        return new PaymentRequest(referenceType, referenceId, 1L, method, amount);
    }

    private static void assertBadRequest(PaymentRequest request) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> PaymentRules.validate(request));
        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    void aceitaPagamentoValido() {
        assertDoesNotThrow(() -> PaymentRules.validate(request("ORDER", 10L, "PIX", 99.9)));
    }

    @Test
    void aceitaMetodoMinusculo() {
        assertDoesNotThrow(() -> PaymentRules.validate(request("appointment", 10L, "credit_card", 50)));
    }

    @Test
    void recusaMetodoInvalido() {
        assertBadRequest(request("ORDER", 10L, "CHEQUE", 50));
        assertBadRequest(request("ORDER", 10L, null, 50));
    }

    @Test
    void recusaValorZero() {
        assertBadRequest(request("ORDER", 10L, "PIX", 0));
        assertBadRequest(request("ORDER", 10L, "PIX", -5));
    }

    @Test
    void recusaReferenciaInvalida() {
        assertBadRequest(request("INVOICE", 10L, "PIX", 50));
        assertBadRequest(request("ORDER", null, "PIX", 50));
        assertBadRequest(new PaymentRequest("ORDER", 10L, null, "PIX", 50));
    }
}
