package br.edu.atitus.orderapi.services;

import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    Page<OrderEntity> listMine(Long customerId, String targetCurrency, Pageable pageable);

    OrderEntity getMine(Long customerId, Long id, String targetCurrency);

    OrderEntity pay(Long customerId, Long id, String method);

    OrderEntity cancel(Long customerId, Long id);

    OrderEntity getReceived(Long providerId, Long id, String targetCurrency);

    Page<OrderEntity> listReceived(Long providerId, OrderStatus status, Pageable pageable);

    OrderEntity updateStatus(Long providerId, Long id, OrderStatus to);
}
