package br.edu.atitus.orderapi.servicesimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.edu.atitus.orderapi.components.CartRules;
import br.edu.atitus.orderapi.dtos.CartItemResponse;
import br.edu.atitus.orderapi.dtos.CartResponse;
import br.edu.atitus.orderapi.dtos.ProductResponse;
import br.edu.atitus.orderapi.entities.CartEntity;
import br.edu.atitus.orderapi.entities.CartItemEntity;
import br.edu.atitus.orderapi.repositories.CartRepository;
import br.edu.atitus.orderapi.services.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartServiceJpa implements CartService {

    private final CartRepository cartRepository;
    private final IntegrationService integration;

    public CartServiceJpa(CartRepository cartRepository, IntegrationService integration) {
        this.cartRepository = cartRepository;
        this.integration = integration;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long customerId, String targetCurrency) {
        return cartRepository.findByCustomerId(customerId)
                .map(cart -> toResponse(cart, targetCurrency))
                .orElseGet(() -> new CartResponse(null, List.of(), 0.0, targetCurrency));
    }

    @Override
    @Transactional
    public CartResponse addItem(Long customerId, Long productId, int quantity, boolean replace, String targetCurrency) {
        // O produto só é consultado para saber a loja; o estoque não é reservado aqui
        ProductResponse product = integration.findProduct(productId, targetCurrency)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado"));
        if (product.establishmentId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Produto sem loja");

        CartEntity cart = cartRepository.findByCustomerId(customerId).orElseGet(() -> newCart(customerId));
        CartRules.addItem(cart, productId, product.establishmentId(), quantity, replace);
        cartRepository.save(cart);
        return toResponse(cart, targetCurrency);
    }

    @Override
    @Transactional
    public CartResponse setQuantity(Long customerId, Long productId, int quantity, String targetCurrency) {
        CartEntity cart = findOrFail(customerId);
        CartRules.setQuantity(cart, productId, quantity);
        cartRepository.save(cart);
        return toResponse(cart, targetCurrency);
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long customerId, Long productId, String targetCurrency) {
        return setQuantity(customerId, productId, 0, targetCurrency);
    }

    @Override
    @Transactional
    public void clear(Long customerId) {
        cartRepository.findByCustomerId(customerId).ifPresent(cart -> {
            cart.getItems().clear();
            cart.setEstablishmentId(null);
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepository.save(cart);
        });
    }

    private CartEntity newCart(Long customerId) {
        CartEntity cart = new CartEntity();
        cart.setCustomerId(customerId);
        return cart;
    }

    private CartEntity findOrFail(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não está no carrinho"));
    }

    // Item cujo produto deixou de existir (404) fica indisponível, com preço 0 e fora do total
    private CartResponse toResponse(CartEntity cart, String targetCurrency) {
        List<CartItemResponse> items = new ArrayList<>();
        double total = 0.0;
        for (CartItemEntity item : cart.getItems()) {
            Optional<ProductResponse> product = integration.findProduct(item.getProductId(), targetCurrency);
            if (product.isPresent()) {
                double unitPrice = product.get().convertedPrice();
                double subtotal = unitPrice * item.getQuantity();
                total += subtotal;
                items.add(new CartItemResponse(item.getProductId(), product.get().name(), product.get().image(),
                        item.getQuantity(), unitPrice, subtotal, true));
            } else {
                items.add(new CartItemResponse(item.getProductId(), null, null, item.getQuantity(), 0.0, 0.0, false));
            }
        }
        return new CartResponse(cart.getEstablishmentId(), items, total, targetCurrency);
    }
}
