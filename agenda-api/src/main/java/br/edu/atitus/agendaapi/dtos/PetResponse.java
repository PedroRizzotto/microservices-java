package br.edu.atitus.agendaapi.dtos;

import br.edu.atitus.agendaapi.entities.PetEntity;
import br.edu.atitus.agendaapi.entities.PetSpecies;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record PetResponse(
        Long id,
        String name,
        PetSpecies species,
        String breed,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate birthDate,
        Double weight,
        String notes) {

    public static PetResponse from(PetEntity entity) {
        return new PetResponse(
                entity.getId(),
                entity.getName(),
                entity.getSpecies(),
                entity.getBreed(),
                entity.getBirthDate(),
                entity.getWeight(),
                entity.getNotes());
    }
}
