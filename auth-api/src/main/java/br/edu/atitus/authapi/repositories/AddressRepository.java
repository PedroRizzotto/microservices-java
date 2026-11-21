package br.edu.atitus.authapi.repositories;

import br.edu.atitus.authapi.entities.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<AddressEntity, Long> {

    List<AddressEntity> findByUserIdOrderByIdAsc(Long userId);
}
