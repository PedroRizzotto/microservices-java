package br.edu.atitus.orderapi.clients;

import java.util.List;

import br.edu.atitus.orderapi.dtos.ReservedItemResponse;
import br.edu.atitus.orderapi.dtos.StockItemRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/internal/products/stock")
public interface StockClient {

    @PostExchange("/reserve")
    List<ReservedItemResponse> reserve(@RequestBody List<StockItemRequest> items);

    @PostExchange("/release")
    void release(@RequestBody List<StockItemRequest> items);
}
