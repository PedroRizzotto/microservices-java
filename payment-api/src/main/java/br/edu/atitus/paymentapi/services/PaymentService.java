package br.edu.atitus.paymentapi.services;

import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import br.edu.atitus.paymentapi.entities.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    /** Indica se já existe pagamento para a referência informada. */
    boolean exists(PaymentRequest request);

    /** Idempotente: devolve o pagamento existente da referência ou cria um aprovado. */
    PaymentEntity pay(PaymentRequest request);

    Page<PaymentEntity> listMine(Long userId, Pageable pageable);
}
