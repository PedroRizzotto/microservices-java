package br.edu.atitus.authapi.servicesimpl;

import br.edu.atitus.authapi.components.AddressRules;
import br.edu.atitus.authapi.dtos.AddressRequest;
import br.edu.atitus.authapi.entities.AddressEntity;
import br.edu.atitus.authapi.repositories.AddressRepository;
import br.edu.atitus.authapi.services.AddressService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceJpa implements AddressService {

    private final AddressRepository repository;

    public AddressServiceJpa(AddressRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AddressEntity> listMine(Long userId) {
        return repository.findByUserIdOrderByIdAsc(userId);
    }

    @Override
    public AddressEntity create(Long userId, AddressRequest r) {
        AddressRequest n = AddressRules.normalize(r);
        AddressRules.validate(n);
        AddressEntity a = new AddressEntity();
        a.setUserId(userId);
        copy(n, a);
        return repository.save(a);
    }

    @Override
    public AddressEntity update(Long userId, Long id, AddressRequest r) {
        AddressEntity a = getForUser(id, userId);
        AddressRequest n = AddressRules.normalize(r);
        AddressRules.validate(n);
        copy(n, a);
        return repository.save(a);
    }

    @Override
    public void delete(Long userId, Long id) {
        repository.delete(getForUser(id, userId));
    }

    @Override
    public AddressEntity getForUser(Long id, Long userId) {
        AddressEntity a = repository.findById(id).orElse(null);
        AddressRules.requireOwner(a, userId);
        return a;
    }

    private void copy(AddressRequest n, AddressEntity a) {
        a.setLabel(n.label());
        a.setZipCode(n.zipCode());
        a.setStreet(n.street());
        a.setNumber(n.number());
        a.setComplement(n.complement());
        a.setDistrict(n.district());
        a.setCity(n.city());
        a.setState(n.state());
    }
}
