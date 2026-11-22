package br.edu.atitus.orderapi.repositories;

import java.util.Optional;

import br.edu.atitus.orderapi.entities.CartEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findByCustomerId(Long customerId);
}
