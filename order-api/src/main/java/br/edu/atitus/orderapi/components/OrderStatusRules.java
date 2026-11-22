package br.edu.atitus.orderapi.components;

import br.edu.atitus.orderapi.entities.OrderStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class OrderStatusRules {

    private OrderStatusRules() {
    }

    // O prestador só avança o pedido: PAID -> SHIPPED -> DELIVERED
    public static void requireProviderTransition(OrderStatus from, OrderStatus to) {
        boolean valid = (from == OrderStatus.PAID && to == OrderStatus.SHIPPED)
                || (from == OrderStatus.SHIPPED && to == OrderStatus.DELIVERED);
        if (!valid)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transição de status inválida");
    }

    public static void requireCancelable(OrderStatus status) {
        if (status != OrderStatus.PENDING_PAYMENT)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido não pode ser cancelado");
    }

    public static void requirePayable(OrderStatus status) {
        if (status != OrderStatus.PENDING_PAYMENT)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedido não está aguardando pagamento");
    }
}
