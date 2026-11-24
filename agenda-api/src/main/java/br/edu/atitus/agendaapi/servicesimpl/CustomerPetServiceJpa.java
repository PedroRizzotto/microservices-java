package br.edu.atitus.agendaapi.servicesimpl;

import br.edu.atitus.agendaapi.components.AgendaRules;
import br.edu.atitus.agendaapi.components.PetRules;
import br.edu.atitus.agendaapi.dtos.PetRequest;
import br.edu.atitus.agendaapi.entities.PetEntity;
import br.edu.atitus.agendaapi.repositories.PetRepository;
import br.edu.atitus.agendaapi.services.CustomerPetService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerPetServiceJpa implements CustomerPetService {

    private final PetRepository petRepository;

    public CustomerPetServiceJpa(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    public List<PetEntity> listMine(Long customerId) {
        return petRepository.findByCustomerIdAndActiveTrueOrderByNameAsc(customerId);
    }

    @Override
    public PetEntity getMine(Long customerId, Long petId) {
        if (petId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pet não informado");
        return petRepository.findByIdAndCustomerIdAndActiveTrue(petId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado"));
    }

    @Override
    public PetEntity create(Long customerId, PetRequest request) {
        PetEntity pet = new PetEntity();
        pet.setCustomerId(customerId);
        return save(pet, request);
    }

    @Override
    public PetEntity update(Long customerId, Long petId, PetRequest request) {
        return save(getMine(customerId, petId), request);
    }

    @Override
    public void delete(Long customerId, Long petId) {
        PetEntity pet = getMine(customerId, petId);
        // Exclusão lógica: os agendamentos antigos continuam apontando para o pet
        pet.setActive(false);
        petRepository.save(pet);
    }

    private PetEntity save(PetEntity pet, PetRequest request) {
        pet.setSpecies(PetRules.validate(request, AgendaRules.now().toLocalDate()));
        pet.setName(request.name().trim());
        pet.setBreed(request.breed());
        pet.setBirthDate(request.birthDate());
        pet.setWeight(request.weight());
        pet.setNotes(request.notes());
        return petRepository.save(pet);
    }
}
