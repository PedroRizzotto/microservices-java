package br.edu.atitus.orderapi.servicesimpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.atitus.orderapi.dtos.AddressResponse;
import br.edu.atitus.orderapi.dtos.ReservedItemResponse;
import br.edu.atitus.orderapi.dtos.StockItemRequest;
import br.edu.atitus.orderapi.entities.CartEntity;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderItemEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import br.edu.atitus.orderapi.repositories.CartRepository;
import br.edu.atitus.orderapi.repositories.OrderRepository;
import br.edu.atitus.orderapi.services.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CheckoutServiceJpa implements CheckoutService {

    private static final String BRL = "BRL";

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final IntegrationService integration;
    private final CurrencyConverter converter;

    public CheckoutServiceJpa(CartRepository cartRepository, OrderRepository orderRepository,
                              IntegrationService integration, CurrencyConverter converter) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.integration = integration;
        this.converter = converter;
    }

    @Override
    @Transactional
    public OrderEntity checkout(Long customerId, Long addressId) {
        CartEntity cart = cartRepository.findByCustomerId(customerId)
                .filter(c -> !c.getItems().isEmpty())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Carrinho vazio"));

        AddressResponse address = integration.findAddress(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado"));

        List<StockItemRequest> requested = cart.getItems().stream()
                .map(i -> new StockItemRequest(i.getProductId(), i.getQuantity()))
                .toList();

        // Se a reserva falhar nada ficou reservado; daqui em diante, qualquer falha devolve o estoque
        List<ReservedItemResponse> reserved = integration.reserveStock(requested);
        try {
            OrderEntity order = orderRepository.save(buildOrder(customerId, address, reserved));
            cart.getItems().clear();
            cart.setEstablishmentId(null);
            cart.setUpdatedAt(LocalDateTime.now());
            // flush dentro do try: deletes/updates do carrinho também ficam cobertos pela devolução do estoque
            cartRepository.saveAndFlush(cart);
            return order;
        } catch (RuntimeException e) {
            try {
                integration.releaseStock(requested);
            } catch (RuntimeException releaseError) {
                e.addSuppressed(releaseError);
            }
            throw e;
        }
    }

    private OrderEntity buildOrder(Long customerId, AddressResponse address, List<ReservedItemResponse> reserved) {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(customerId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setOrderDate(LocalDateTime.now());
        order.setEstablishmentId(reserved.get(0).establishmentId());
        order.setProviderId(reserved.get(0).providerId());
        order.setDeliveryZipCode(address.zipCode());
        order.setDeliveryStreet(address.street());
        order.setDeliveryNumber(address.number());
        order.setDeliveryComplement(address.complement());
        order.setDeliveryDistrict(address.district());
        order.setDeliveryCity(address.city());
        order.setDeliveryState(address.state());

        List<OrderItemEntity> items = new ArrayList<>();
        double total = 0.0;
        for (ReservedItemResponse r : reserved) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductId(r.productId());
            item.setProductName(r.name());
            item.setQuantity(r.quantity());
            item.setPriceAtPurchase(converter.toBrl(r.price(), r.currency()));
            item.setCurrencyAtPurchase(BRL);
            total += item.getPriceAtPurchase() * r.quantity();
            items.add(item);
        }
        order.setItems(items);
        order.setTotalAmount(total);
        return order;
    }
}
