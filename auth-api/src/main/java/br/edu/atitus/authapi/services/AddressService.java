package br.edu.atitus.authapi.services;

import br.edu.atitus.authapi.dtos.AddressRequest;
import br.edu.atitus.authapi.entities.AddressEntity;

import java.util.List;

public interface AddressService {

    List<AddressEntity> listMine(Long userId);

    AddressEntity create(Long userId, AddressRequest r);

    AddressEntity update(Long userId, Long id, AddressRequest r);

    void delete(Long userId, Long id);

    AddressEntity getForUser(Long id, Long userId);
}
