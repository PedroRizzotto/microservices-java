package br.edu.atitus.paymentapi.services;

import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import br.edu.atitus.paymentapi.entities.PaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    boolean exists(PaymentRequest request);

    PaymentEntity pay(PaymentRequest request);

    Page<PaymentEntity> listMine(Long userId, Pageable pageable);
}
