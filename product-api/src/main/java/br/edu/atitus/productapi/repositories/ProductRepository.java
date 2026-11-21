package br.edu.atitus.productapi.repositories;

import br.edu.atitus.productapi.entities.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // Trava as linhas até o fim da transação para evitar venda acima do estoque
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ProductEntity> findAllByIdIn(Collection<Long> ids);
}
