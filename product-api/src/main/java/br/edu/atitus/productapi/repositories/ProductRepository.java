package br.edu.atitus.productapi.repositories;

import br.edu.atitus.productapi.entities.ProductEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {

    // Trava as linhas até o fim da transação para evitar venda acima do estoque
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Ordenado por id para todas as transações travarem as linhas na mesma ordem (evita deadlock)
    List<ProductEntity> findAllByIdInOrderByIdAsc(Collection<Long> ids);

    // Evita que update/deactivate sobrescrevam um decremento de estoque concorrente
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ProductEntity> findWithLockById(Long id);

    Page<ProductEntity> findAllByProviderIdAndActiveTrue(Long providerId, Pageable pageable);
}
