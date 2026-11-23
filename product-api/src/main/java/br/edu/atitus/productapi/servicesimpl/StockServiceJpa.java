package br.edu.atitus.productapi.servicesimpl;

import br.edu.atitus.productapi.components.StockRules;
import br.edu.atitus.productapi.dtos.ReservedItemResponse;
import br.edu.atitus.productapi.dtos.StockItemRequest;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.repositories.ProductRepository;
import br.edu.atitus.productapi.services.StockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockServiceJpa implements StockService {
    private final ProductRepository repository;

    public StockServiceJpa(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<ReservedItemResponse> reserve(List<StockItemRequest> items) {
        Map<Long, Integer> wanted = StockRules.merge(items);
        Map<Long, ProductEntity> products = load(wanted);
        List<ReservedItemResponse> result = StockRules.reserve(wanted, products);
        repository.saveAll(products.values());
        return result;
    }

    @Override
    @Transactional
    public void release(List<StockItemRequest> items) {
        Map<Long, Integer> wanted = StockRules.merge(items);
        Map<Long, ProductEntity> products = load(wanted);
        StockRules.release(wanted, products);
        repository.saveAll(products.values());
    }

    private Map<Long, ProductEntity> load(Map<Long, Integer> wanted) {
        Map<Long, ProductEntity> products = new HashMap<>();
        for (ProductEntity product : repository.findAllById(wanted.keySet())) {
            products.put(product.getId(), product);
        }
        return products;
    }
}
