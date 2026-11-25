package br.edu.atitus.agendaapi.dtos;

import br.edu.atitus.agendaapi.entities.PetServiceEntity;

public record PetServiceResponse(
        Long id,
        Long establishmentId,
        String name,
        Integer durationMinutes,
        double price,
        boolean active) {

    public static PetServiceResponse from(PetServiceEntity entity) {
        return new PetServiceResponse(
                entity.getId(),
                entity.getEstablishmentId(),
                entity.getName(),
                entity.getDurationMinutes(),
                entity.getPrice(),
                entity.isActive());
    }
}
