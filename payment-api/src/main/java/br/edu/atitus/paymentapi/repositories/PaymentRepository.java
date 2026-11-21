package br.edu.atitus.paymentapi.repositories;

import br.edu.atitus.paymentapi.entities.PaymentEntity;
import br.edu.atitus.paymentapi.entities.PaymentReferenceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByReferenceTypeAndReferenceId(PaymentReferenceType referenceType, Long referenceId);

    Page<PaymentEntity> findByUserId(Long userId, Pageable pageable);
}
