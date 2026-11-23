package br.edu.atitus.orderapi.servicesimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.edu.atitus.orderapi.dtos.PaymentRequest;
import br.edu.atitus.orderapi.dtos.PaymentResponse;
import br.edu.atitus.orderapi.dtos.StockItemRequest;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderItemEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import br.edu.atitus.orderapi.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceJpaTest {

    private static final Long CUSTOMER = 7L;
    private static final Long PROVIDER = 9L;
    private static final Long ORDER_ID = 100L;

    private OrderRepository orderRepository;
    private IntegrationService integration;
    private OrderServiceJpa service;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        integration = mock(IntegrationService.class);
        service = new OrderServiceJpa(orderRepository, integration);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.saveAndFlush(any(OrderEntity.class))).thenAnswer(i -> i.getArgument(0));
    }

    private OrderEntity order(OrderStatus status) {
        OrderEntity order = new OrderEntity();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER);
        order.setProviderId(PROVIDER);
        order.setStatus(status);
        order.setTotalAmount(110.0);
        List<OrderItemEntity> items = new ArrayList<>();
        items.add(item(10L, 2, 50.0));
        items.add(item(11L, 1, 10.0));
        order.setItems(items);
        return order;
    }

    private OrderItemEntity item(Long productId, int quantity, double price) {
        OrderItemEntity item = new OrderItemEntity();
        item.setProductId(productId);
        item.setProductName("Produto " + productId);
        item.setQuantity(quantity);
        item.setPriceAtPurchase(price);
        item.setCurrencyAtPurchase("BRL");
        return item;
    }

    private void found(OrderEntity order) {
        when(orderRepository.findByIdAndCustomerId(ORDER_ID, CUSTOMER)).thenReturn(Optional.of(order));
        when(orderRepository.findWithLockByIdAndCustomerId(ORDER_ID, CUSTOMER)).thenReturn(Optional.of(order));
    }

    @Test
    void pagarPedidoPendenteViraPago() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT);
        found(order);
        when(integration.createPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(55L, "APPROVED", "TX1"));

        OrderEntity paid = service.pay(CUSTOMER, ORDER_ID, "PIX");

        assertEquals(OrderStatus.PAID, paid.getStatus());
        assertEquals(55L, paid.getPaymentId());
        assertNotNull(paid.getPaidAt());
        verify(integration).createPayment(new PaymentRequest("ORDER", ORDER_ID, CUSTOMER, "PIX", 110.0));
    }

    @Test
    void pagarDuasVezes409() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT);
        found(order);
        when(integration.createPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(55L, "APPROVED", "TX1"));
        service.pay(CUSTOMER, ORDER_ID, "PIX");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(CUSTOMER, ORDER_ID, "PIX"));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(integration, times(1)).createPayment(any(PaymentRequest.class));
    }

    @Test
    void falhaNoPagamentoDa503eMantemPendente() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT);
        found(order);
        when(integration.createPayment(any(PaymentRequest.class))).thenThrow(new IllegalStateException("fora do ar"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(CUSTOMER, ORDER_ID, "PIX"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatusCode());
        assertEquals("Pagamento indisponível, tente novamente", e.getReason());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
    }

    @Test
    void erroDe4xxDoPagamentoPassaDireto() {
        found(order(OrderStatus.PENDING_PAYMENT));
        when(integration.createPayment(any(PaymentRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor inválido"));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(CUSTOMER, ORDER_ID, "PIX"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    @Test
    void pagarMetodoInvalido400SemTocarNoPedido() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.pay(CUSTOMER, ORDER_ID, "DINHEIRO"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("Método de pagamento inválido", e.getReason());
        verify(orderRepository, never()).findWithLockByIdAndCustomerId(anyLong(), anyLong());
        verify(integration, never()).createPayment(any(PaymentRequest.class));
    }

    @Test
    void pedidoDeOutroCliente404() {
        when(orderRepository.findByIdAndCustomerId(ORDER_ID, 99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.getMine(99L, ORDER_ID, "BRL"));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertEquals("Pedido não encontrado", e.getReason());
    }

    @Test
    void cancelarDevolveEstoque() {
        OrderEntity order = order(OrderStatus.PENDING_PAYMENT);
        found(order);

        OrderEntity canceled = service.cancel(CUSTOMER, ORDER_ID);

        assertEquals(OrderStatus.CANCELED, canceled.getStatus());
        verify(integration).releaseStock(List.of(new StockItemRequest(10L, 2), new StockItemRequest(11L, 1)));
    }

    @Test
    void cancelarGravaStatusAntesDeDevolver() {
        found(order(OrderStatus.PENDING_PAYMENT));

        service.cancel(CUSTOMER, ORDER_ID);

        InOrder inOrder = inOrder(orderRepository, integration);
        inOrder.verify(orderRepository).saveAndFlush(any(OrderEntity.class));
        inOrder.verify(integration).releaseStock(any());
    }

    @Test
    void pagarUsaLeituraComLock() {
        found(order(OrderStatus.PENDING_PAYMENT));
        when(integration.createPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(55L, "APPROVED", "TX1"));

        service.pay(CUSTOMER, ORDER_ID, "PIX");

        verify(orderRepository).findWithLockByIdAndCustomerId(ORDER_ID, CUSTOMER);
    }

    @Test
    void aceitaMetodoMinusculo() {
        found(order(OrderStatus.PENDING_PAYMENT));
        when(integration.createPayment(any(PaymentRequest.class)))
                .thenReturn(new PaymentResponse(55L, "APPROVED", "TX1"));

        OrderEntity paid = service.pay(CUSTOMER, ORDER_ID, " pix ");

        assertEquals(OrderStatus.PAID, paid.getStatus());
        verify(integration).createPayment(new PaymentRequest("ORDER", ORDER_ID, CUSTOMER, "PIX", 110.0));
    }

    @Test
    void cancelarPagoRecusa409() {
        found(order(OrderStatus.PAID));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cancel(CUSTOMER, ORDER_ID));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        verify(integration, never()).releaseStock(any());
    }

    @Test
    void prestadorDeOutraLoja404() {
        when(orderRepository.findWithLockByIdAndProviderId(ORDER_ID, 99L)).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(99L, ORDER_ID, OrderStatus.SHIPPED));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void prestadorEnviaPedidoPago() {
        OrderEntity order = order(OrderStatus.PAID);
        when(orderRepository.findWithLockByIdAndProviderId(ORDER_ID, PROVIDER)).thenReturn(Optional.of(order));

        assertEquals(OrderStatus.SHIPPED, service.updateStatus(PROVIDER, ORDER_ID, OrderStatus.SHIPPED).getStatus());
    }

    @Test
    void prestadorNaoPulaEtapa409() {
        when(orderRepository.findWithLockByIdAndProviderId(ORDER_ID, PROVIDER))
                .thenReturn(Optional.of(order(OrderStatus.PAID)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(PROVIDER, ORDER_ID, OrderStatus.DELIVERED));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
    }

    @Test
    void listarNaoChamaProductApi() {
        OrderEntity order = order(OrderStatus.PAID);
        PageRequest pageable = PageRequest.of(0, 5);
        when(orderRepository.findByCustomerId(CUSTOMER, pageable)).thenReturn(new PageImpl<>(List.of(order)));
        when(integration.conversionRate("BRL", "USD")).thenReturn(0.2);

        Page<OrderEntity> page = service.listMine(CUSTOMER, "USD", pageable);

        OrderEntity result = page.getContent().get(0);
        assertEquals(10.0, result.getItems().get(0).getConvertedPrice(), 0.001);
        assertEquals(22.0, result.getTotalConvertedPrice(), 0.001);
        verify(integration, never()).findProduct(anyLong(), anyString());
        verify(integration, times(1)).conversionRate("BRL", "USD");
    }

    @Test
    void listarEmBrlCopiaSemConsultarCambio() {
        OrderEntity order = order(OrderStatus.PAID);
        found(order);

        OrderEntity result = service.getMine(CUSTOMER, ORDER_ID, "BRL");

        assertEquals(50.0, result.getItems().get(0).getConvertedPrice(), 0.001);
        assertEquals(110.0, result.getTotalConvertedPrice(), 0.001);
        verify(integration, never()).conversionRate(anyString(), anyString());
    }
}
