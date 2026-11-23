package br.edu.atitus.productapi.components;

import br.edu.atitus.productapi.dtos.ReservedItemResponse;
import br.edu.atitus.productapi.dtos.StockItemRequest;
import br.edu.atitus.productapi.entities.ProductEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class StockRules {

    private StockRules() {
    }

    public static Map<Long, Integer> merge(List<StockItemRequest> items) {
        if (items == null || items.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lista de itens vazia");
        Map<Long, Integer> wanted = new HashMap<>();
        for (StockItemRequest item : items) {
            if (item == null || item.productId() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item inválido");
            if (item.quantity() <= 0)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade inválida");
            int quantity = wanted.getOrDefault(item.productId(), 0);
            wanted.put(item.productId(), quantity + item.quantity());
        }
        return wanted;
    }

    public static List<ReservedItemResponse> reserve(Map<Long, Integer> wanted, Map<Long, ProductEntity> products) {
        Long establishmentId = null;
        boolean first = true;
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            ProductEntity p = products.get(e.getKey());
            if (p == null || !p.isActive())
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + e.getKey() + " não encontrado");
            if (first) {
                establishmentId = p.getEstablishmentId();
                first = false;
            } else if (!Objects.equals(establishmentId, p.getEstablishmentId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Todos os produtos devem ser da mesma loja");
            }
            if (p.getStock() < e.getValue())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente para o produto " + e.getKey());
        }

        List<ReservedItemResponse> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            ProductEntity p = products.get(e.getKey());
            p.setStock(p.getStock() - e.getValue());
            result.add(new ReservedItemResponse(p.getId(), p.getName(), e.getValue(), p.getPrice(),
                    p.getCurrency(), p.getEstablishmentId(), p.getProviderId()));
        }
        return result;
    }

    public static void release(Map<Long, Integer> wanted, Map<Long, ProductEntity> products) {
        for (Map.Entry<Long, Integer> e : wanted.entrySet()) {
            ProductEntity p = products.get(e.getKey());
            if (p != null)
                p.setStock(p.getStock() + e.getValue());
        }
    }
}
