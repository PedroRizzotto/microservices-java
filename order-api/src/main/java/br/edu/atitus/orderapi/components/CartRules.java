package br.edu.atitus.orderapi.components;

import java.time.LocalDateTime;
import java.util.Optional;

import br.edu.atitus.orderapi.entities.CartEntity;
import br.edu.atitus.orderapi.entities.CartItemEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class CartRules {

    private CartRules() {
    }

    public static void addItem(CartEntity cart, Long productId, Long establishmentId, int quantity, boolean replace) {
        if (quantity <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade inválida");

        boolean otherStore = !cart.getItems().isEmpty() && !establishmentId.equals(cart.getEstablishmentId());
        if (otherStore) {
            if (!replace)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O carrinho tem produtos de outra loja");
            cart.getItems().clear();
        }
        cart.setEstablishmentId(establishmentId);

        Optional<CartItemEntity> existing = find(cart, productId);
        if (existing.isPresent()) {
            existing.get().setQuantity(existing.get().getQuantity() + quantity);
        } else {
            CartItemEntity item = new CartItemEntity();
            item.setCart(cart);
            item.setProductId(productId);
            item.setQuantity(quantity);
            cart.getItems().add(item);
        }
        cart.setUpdatedAt(LocalDateTime.now());
    }

    public static void setQuantity(CartEntity cart, Long productId, int quantity) {
        if (quantity < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade inválida");
        CartItemEntity item = find(cart, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não está no carrinho"));
        if (quantity == 0)
            cart.getItems().remove(item);
        else
            item.setQuantity(quantity);
        if (cart.getItems().isEmpty())
            cart.setEstablishmentId(null);
        cart.setUpdatedAt(LocalDateTime.now());
    }

    private static Optional<CartItemEntity> find(CartEntity cart, Long productId) {
        return cart.getItems().stream().filter(i -> i.getProductId().equals(productId)).findFirst();
    }
}
