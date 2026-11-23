package br.edu.atitus.orderapi.repositories;

import java.util.Optional;

import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Page<OrderEntity> findByCustomerId(Long customerId, Pageable pageable);

    Optional<OrderEntity> findByIdAndCustomerId(Long id, Long customerId);

    Page<OrderEntity> findByProviderId(Long providerId, Pageable pageable);

    Page<OrderEntity> findByProviderIdAndStatus(Long providerId, OrderStatus status, Pageable pageable);

    Optional<OrderEntity> findByIdAndProviderId(Long id, Long providerId);
}
