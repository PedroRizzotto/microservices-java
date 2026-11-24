package br.edu.atitus.agendaapi.services;

import br.edu.atitus.agendaapi.dtos.PetRequest;
import br.edu.atitus.agendaapi.entities.PetEntity;

import java.util.List;

public interface CustomerPetService {
    List<PetEntity> listMine(Long customerId);

    PetEntity getMine(Long customerId, Long petId);

    PetEntity create(Long customerId, PetRequest request);

    PetEntity update(Long customerId, Long petId, PetRequest request);

    void delete(Long customerId, Long petId);
}
