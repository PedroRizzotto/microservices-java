package br.edu.atitus.paymentapi.servicesimpl;

import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import br.edu.atitus.paymentapi.entities.PaymentEntity;
import br.edu.atitus.paymentapi.entities.PaymentMethod;
import br.edu.atitus.paymentapi.entities.PaymentReferenceType;
import br.edu.atitus.paymentapi.entities.PaymentStatus;
import br.edu.atitus.paymentapi.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceJpaTest {

    private PaymentRepository repository;
    private PaymentServiceJpa service;

    private final PaymentRequest request = new PaymentRequest("ORDER", 7L, 3L, "pix", 120.5);

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PaymentRepository.class);
        service = new PaymentServiceJpa(repository);
    }

    @Test
    void criaPagamentoAprovado() {
        when(repository.findByReferenceTypeAndReferenceId(PaymentReferenceType.ORDER, 7L)).thenReturn(Optional.empty());
        when(repository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentEntity payment = service.pay(request);

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals("BRL", payment.getCurrency());
        assertNotNull(payment.getTransactionCode());
        assertNotNull(payment.getCreatedAt());
        assertEquals(PaymentMethod.PIX, payment.getMethod());
        assertEquals(120.5, payment.getAmount());
        assertEquals(3L, payment.getUserId());
    }

    @Test
    void devolveExistenteParaMesmaReferencia() {
        PaymentEntity existing = new PaymentEntity();
        when(repository.findByReferenceTypeAndReferenceId(PaymentReferenceType.ORDER, 7L)).thenReturn(Optional.of(existing));

        assertSame(existing, service.pay(request));
        verify(repository, never()).save(any());
    }
}
