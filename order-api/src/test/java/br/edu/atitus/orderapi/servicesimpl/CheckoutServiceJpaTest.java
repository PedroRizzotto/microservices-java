package br.edu.atitus.orderapi.servicesimpl;

import java.util.List;
import java.util.Optional;

import br.edu.atitus.orderapi.dtos.AddressResponse;
import br.edu.atitus.orderapi.dtos.ReservedItemResponse;
import br.edu.atitus.orderapi.dtos.StockItemRequest;
import br.edu.atitus.orderapi.entities.CartEntity;
import br.edu.atitus.orderapi.entities.CartItemEntity;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import br.edu.atitus.orderapi.repositories.CartRepository;
import br.edu.atitus.orderapi.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutServiceJpaTest {

    private static final Long CUSTOMER = 7L;
    private static final Long ADDRESS = 3L;

    private CartRepository cartRepository;
    private OrderRepository orderRepository;
    private IntegrationService integration;
    private CurrencyConverter converter;
    private CheckoutServiceJpa service;

    @BeforeEach
    void setUp() {
        cartRepository = mock(CartRepository.class);
        orderRepository = mock(OrderRepository.class);
        integration = mock(IntegrationService.class);
        converter = mock(CurrencyConverter.class);
        service = new CheckoutServiceJpa(cartRepository, orderRepository, integration, converter);

        when(converter.toBrl(Mockito.anyDouble(), Mockito.anyString()))
                .thenAnswer(i -> (double) i.getArgument(0) * ("USD".equals(i.getArgument(1)) ? 5.0 : 1.0));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private CartEntity cartWithItems() {
        CartEntity cart = new CartEntity();
        cart.setCustomerId(CUSTOMER);
        cart.setEstablishmentId(1L);
        cart.getItems().add(item(cart, 10L, 2));
        cart.getItems().add(item(cart, 11L, 1));
        return cart;
    }

    private CartItemEntity item(CartEntity cart, Long productId, int quantity) {
        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private AddressResponse address() {
        return new AddressResponse(ADDRESS, "Casa", "99000-000", "Rua A", "10", "Ap 2", "Centro", "Passo Fundo", "RS");
    }

    private List<StockItemRequest> stockRequest() {
        return List.of(new StockItemRequest(10L, 2), new StockItemRequest(11L, 1));
    }

    private List<ReservedItemResponse> reserved() {
        return List.of(
                new ReservedItemResponse(10L, "Ração", 2, 20.0, "BRL", 1L, 99L),
                new ReservedItemResponse(11L, "Coleira", 1, 10.0, "USD", 1L, 99L));
    }

    @Test
    void checkoutCriaPedidoPendenteEEsvaziaCarrinho() {
        CartEntity cart = cartWithItems();
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cart));
        when(integration.findAddress(ADDRESS, CUSTOMER)).thenReturn(Optional.of(address()));
        when(integration.reserveStock(stockRequest())).thenReturn(reserved());

        OrderEntity order = service.checkout(CUSTOMER, ADDRESS);

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(CUSTOMER, order.getCustomerId());
        assertEquals(1L, order.getEstablishmentId());
        assertEquals(99L, order.getProviderId());
        assertNotNull(order.getOrderDate());
        assertEquals(2, order.getItems().size());
        assertEquals("Ração", order.getItems().get(0).getProductName());
        assertEquals(20.0, order.getItems().get(0).getPriceAtPurchase());
        assertEquals(50.0, order.getItems().get(1).getPriceAtPurchase());
        assertEquals("BRL", order.getItems().get(1).getCurrencyAtPurchase());
        assertSame(order, order.getItems().get(0).getOrder());
        assertEquals(90.0, order.getTotalAmount());
        assertEquals("99000-000", order.getDeliveryZipCode());
        assertEquals("Rua A", order.getDeliveryStreet());
        assertEquals("10", order.getDeliveryNumber());
        assertEquals("Ap 2", order.getDeliveryComplement());
        assertEquals("Centro", order.getDeliveryDistrict());
        assertEquals("Passo Fundo", order.getDeliveryCity());
        assertEquals("RS", order.getDeliveryState());
        assertTrue(cart.getItems().isEmpty());
        assertEquals(null, cart.getEstablishmentId());
        verify(cartRepository).saveAndFlush(cart);
        verify(integration, never()).releaseStock(any());
    }

    @Test
    void devolveEstoqueSeFalharAoGravarOCarrinho() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWithItems()));
        when(integration.findAddress(ADDRESS, CUSTOMER)).thenReturn(Optional.of(address()));
        when(integration.reserveStock(stockRequest())).thenReturn(reserved());
        when(cartRepository.saveAndFlush(any(CartEntity.class))).thenThrow(new IllegalStateException("falha no flush"));

        assertThrows(IllegalStateException.class, () -> service.checkout(CUSTOMER, ADDRESS));

        verify(integration, Mockito.times(1)).releaseStock(stockRequest());
    }

    @Test
    void carrinhoVazio400() {
        CartEntity cart = new CartEntity();
        cart.setCustomerId(CUSTOMER);
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cart));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.checkout(CUSTOMER, ADDRESS));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("Carrinho vazio", e.getReason());
        verify(integration, never()).reserveStock(any());
    }

    @Test
    void devolveEstoqueSeFalharDepoisDaReserva() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWithItems()));
        when(integration.findAddress(ADDRESS, CUSTOMER)).thenReturn(Optional.of(address()));
        when(integration.reserveStock(stockRequest())).thenReturn(reserved());
        when(orderRepository.save(any(OrderEntity.class))).thenThrow(new IllegalStateException("falha no banco"));

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.checkout(CUSTOMER, ADDRESS));

        assertEquals("falha no banco", e.getMessage());
        verify(integration, Mockito.times(1)).releaseStock(stockRequest());
    }

    @Test
    void naoDevolveSeReservaFalhar() {
        when(cartRepository.findByCustomerId(CUSTOMER)).thenReturn(Optional.of(cartWithItems()));
        when(integration.findAddress(ADDRESS, CUSTOMER)).thenReturn(Optional.of(address()));
        when(integration.reserveStock(stockRequest()))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.checkout(CUSTOMER, ADDRESS));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals("Estoque insuficiente", e.getReason());
        verify(integration, never()).releaseStock(any());
        verify(orderRepository, never()).save(any());
    }
}
