package br.edu.atitus.orderapi.servicesimpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import br.edu.atitus.orderapi.components.OrderStatusRules;
import br.edu.atitus.orderapi.dtos.PaymentRequest;
import br.edu.atitus.orderapi.dtos.PaymentResponse;
import br.edu.atitus.orderapi.dtos.StockItemRequest;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderItemEntity;
import br.edu.atitus.orderapi.entities.OrderStatus;
import br.edu.atitus.orderapi.repositories.OrderRepository;
import br.edu.atitus.orderapi.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderServiceJpa implements OrderService {

    private static final String BRL = "BRL";
    private static final Set<String> METHODS = Set.of("PIX", "CREDIT_CARD", "DEBIT_CARD", "BOLETO");

    private final OrderRepository orderRepository;
    private final IntegrationService integration;

    public OrderServiceJpa(OrderRepository orderRepository, IntegrationService integration) {
        this.orderRepository = orderRepository;
        this.integration = integration;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderEntity> listMine(Long customerId, String targetCurrency, Pageable pageable) {
        Page<OrderEntity> orders = orderRepository.findByCustomerId(customerId, pageable);
        applyConversion(orders.getContent(), targetCurrency);
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderEntity getMine(Long customerId, Long id, String targetCurrency) {
        OrderEntity order = orderRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        applyConversion(List.of(order), targetCurrency);
        return order;
    }

    @Override
    @Transactional
    public OrderEntity pay(Long customerId, Long id, String method) {
        if (method == null || !METHODS.contains(method))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Método de pagamento inválido");
        OrderEntity order = orderRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        OrderStatusRules.requirePayable(order.getStatus());

        PaymentResponse payment;
        try {
            payment = integration.createPayment(
                    new PaymentRequest("ORDER", order.getId(), customerId, method, order.getTotalAmount()));
        } catch (ResponseStatusException e) {
            throw e; // 4xx do payment-api segue como veio
        } catch (RuntimeException e) {
            // rede, circuit breaker aberto ou 5xx: o pedido continua pendente
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Pagamento indisponível, tente novamente");
        }

        order.setPaymentId(payment.id());
        order.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public OrderEntity cancel(Long customerId, Long id) {
        OrderEntity order = orderRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        OrderStatusRules.requireCancelable(order.getStatus());

        List<StockItemRequest> toRelease = order.getItems().stream()
                .map(i -> new StockItemRequest(i.getProductId(), i.getQuantity()))
                .toList();
        integration.releaseStock(toRelease);

        order.setStatus(OrderStatus.CANCELED);
        return orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderEntity> listReceived(Long providerId, OrderStatus status, Pageable pageable) {
        return status == null
                ? orderRepository.findByProviderId(providerId, pageable)
                : orderRepository.findByProviderIdAndStatus(providerId, status, pageable);
    }

    @Override
    @Transactional
    public OrderEntity updateStatus(Long providerId, Long id, OrderStatus to) {
        OrderEntity order = orderRepository.findByIdAndProviderId(id, providerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        OrderStatusRules.requireProviderTransition(order.getStatus(), to);
        order.setStatus(to);
        return orderRepository.save(order);
    }

    // Os preços gravados são sempre BRL; uma consulta de câmbio por chamada basta
    private void applyConversion(List<OrderEntity> orders, String targetCurrency) {
        double rate = BRL.equals(targetCurrency) || orders.isEmpty()
                ? 1.0
                : integration.conversionRate(BRL, targetCurrency);
        for (OrderEntity order : orders) {
            double total = 0.0;
            for (OrderItemEntity item : order.getItems()) {
                double converted = BRL.equals(targetCurrency)
                        ? item.getPriceAtPurchase()
                        : Math.round(item.getPriceAtPurchase() * rate * 100.0) / 100.0;
                item.setConvertedPrice(converted);
                total += converted * item.getQuantity();
            }
            order.setTotalConvertedPrice(BRL.equals(targetCurrency)
                    ? order.getTotalAmount()
                    : Math.round(total * 100.0) / 100.0);
        }
    }
}
