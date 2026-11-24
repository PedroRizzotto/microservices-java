package br.edu.atitus.agendaapi.components;

import br.edu.atitus.agendaapi.dtos.PetRequest;
import br.edu.atitus.agendaapi.entities.PetSpecies;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDate;

public class PetRules {

    public static PetSpecies validate(PetRequest request, LocalDate today) {
        if (request == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do pet não informados");
        if (request.name() == null || request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome do pet informado inválido");
        if (request.birthDate() != null && request.birthDate().isAfter(today))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data de nascimento informada inválida");
        if (request.weight() != null && request.weight() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Peso informado inválido");
        return parseSpecies(request.species());
    }

    public static PetSpecies parseSpecies(String species) {
        if (species == null || species.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Espécie informada inválida");
        try {
            // Aceita com ou sem acento: "Pássaro", "REPTIL", "réptil"
            String plain = Normalizer.normalize(species.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            return PetSpecies.valueOf(plain.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Espécie informada inválida");
        }
    }
}
