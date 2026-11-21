package br.edu.atitus.productapi.services;

import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    ProductResponse findById(Long id, String targetCurrency);
    Page<ProductResponse> search(Long establishmentId, String search, String category, Pageable pageable, String targetCurrency);
    ProductEntity create(Long providerId, ProductRequest r);
    ProductEntity update(Long providerId, Long id, ProductRequest r);
    void deactivate(Long providerId, Long id);
    Page<ProductEntity> findMine(Long providerId, Pageable pageable);
}
