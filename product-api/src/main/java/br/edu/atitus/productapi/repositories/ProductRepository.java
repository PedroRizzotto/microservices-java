package br.edu.atitus.productapi.repositories;

import br.edu.atitus.productapi.entities.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    Page<ProductEntity> findAllByProviderIdAndActiveTrue(Long providerId, Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM ProductEntity p WHERE p.active = true AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findActiveCategories();
}
