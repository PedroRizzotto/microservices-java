package br.edu.atitus.productapi.services;

import br.edu.atitus.productapi.dtos.ReservedItemResponse;
import br.edu.atitus.productapi.dtos.StockItemRequest;

import java.util.List;

public interface StockService {

    List<ReservedItemResponse> reserve(List<StockItemRequest> items);

    void release(List<StockItemRequest> items);
}
