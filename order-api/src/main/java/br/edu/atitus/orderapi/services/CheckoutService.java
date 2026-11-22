package br.edu.atitus.orderapi.services;

import br.edu.atitus.orderapi.entities.OrderEntity;

public interface CheckoutService {
    OrderEntity checkout(Long customerId, Long addressId);
}
