package br.edu.atitus.orderapi.services;

import br.edu.atitus.orderapi.dtos.CartResponse;

public interface CartService {
    CartResponse getCart(Long customerId, String targetCurrency);

    CartResponse addItem(Long customerId, Long productId, int quantity, boolean replace, String targetCurrency);

    CartResponse setQuantity(Long customerId, Long productId, int quantity, String targetCurrency);

    CartResponse removeItem(Long customerId, Long productId, String targetCurrency);

    void clear(Long customerId);
}
