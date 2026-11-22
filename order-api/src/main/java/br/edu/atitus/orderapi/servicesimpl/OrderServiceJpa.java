package br.edu.atitus.orderapi.servicesimpl;

import br.edu.atitus.orderapi.clients.CurrencyClient;
import br.edu.atitus.orderapi.dtos.CurrencyResponse;
import br.edu.atitus.orderapi.entities.OrderEntity;
import br.edu.atitus.orderapi.entities.OrderItemEntity;
import br.edu.atitus.orderapi.repositories.OrderRepository;
import br.edu.atitus.orderapi.services.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceJpa implements OrderService {

    private final OrderRepository orderRepository;
    private final CurrencyClient currencyClient;

    public OrderServiceJpa(OrderRepository orderRepository, CurrencyClient currencyClient) {
        this.orderRepository = orderRepository;
        this.currencyClient = currencyClient;
    }

    @Override
    public Page<OrderEntity> findOrdersByCustomerId(Long customerId, String targetCurrency, Pageable pageable) {
        Page<OrderEntity> orders = orderRepository.findByCustomerId(customerId, pageable);

        // Usa só os dados gravados no pedido; a conversão é apenas para exibição
        for (OrderEntity order : orders) {
            double totalConvertedPrice = 0.0;

            for (OrderItemEntity item : order.getItems()) {
                CurrencyResponse currencyResponse = currencyClient.getCurrency(item.getCurrencyAtPurchase(), targetCurrency);
                item.setConvertedPrice(item.getPriceAtPurchase() * currencyResponse.conversionRate());
                totalConvertedPrice += item.getConvertedPrice() * item.getQuantity();
            }
            order.setTotalConvertedPrice(totalConvertedPrice);
        }
        return orders;
    }
}
