package br.edu.atitus.paymentapi.servicesimpl;

import br.edu.atitus.paymentapi.components.PaymentRules;
import br.edu.atitus.paymentapi.dtos.PaymentRequest;
import br.edu.atitus.paymentapi.entities.PaymentEntity;
import br.edu.atitus.paymentapi.entities.PaymentReferenceType;
import br.edu.atitus.paymentapi.entities.PaymentStatus;
import br.edu.atitus.paymentapi.repositories.PaymentRepository;
import br.edu.atitus.paymentapi.services.PaymentService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentServiceJpa implements PaymentService {

    private final PaymentRepository repository;

    public PaymentServiceJpa(PaymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean exists(PaymentRequest request) {
        PaymentRules.validate(request);
        return repository.findByReferenceTypeAndReferenceId(
                PaymentRules.parseReferenceType(request.referenceType()), request.referenceId()).isPresent();
    }

    @Override
    public PaymentEntity pay(PaymentRequest request) {
        PaymentRules.validate(request);
        PaymentReferenceType referenceType = PaymentRules.parseReferenceType(request.referenceType());

        var existing = repository.findByReferenceTypeAndReferenceId(referenceType, request.referenceId());
        if (existing.isPresent())
            return existing.get();

        PaymentEntity payment = new PaymentEntity();
        payment.setReferenceType(referenceType);
        payment.setReferenceId(request.referenceId());
        payment.setUserId(request.userId());
        payment.setMethod(PaymentRules.parseMethod(request.method()));
        payment.setAmount(request.amount());
        payment.setCurrency("BRL");
        payment.setStatus(PaymentStatus.APPROVED);
        payment.setTransactionCode(UUID.randomUUID().toString());
        payment.setCreatedAt(PaymentRules.now());
        try {
            return repository.save(payment);
        } catch (DataIntegrityViolationException e) {
            // Requisição concorrente já criou o pagamento desta referência.
            return repository.findByReferenceTypeAndReferenceId(referenceType, request.referenceId())
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public Page<PaymentEntity> listMine(Long userId, Pageable pageable) {
        return repository.findByUserId(userId, pageable);
    }
}
