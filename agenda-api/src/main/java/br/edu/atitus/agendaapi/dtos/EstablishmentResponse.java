package br.edu.atitus.agendaapi.dtos;

import br.edu.atitus.agendaapi.entities.EstablishmentEntity;

public record EstablishmentResponse(Long id, String name, String address, String phone) {

    public static EstablishmentResponse from(EstablishmentEntity entity) {
        return new EstablishmentResponse(entity.getId(), entity.getName(), entity.getAddress(), entity.getPhone());
    }
}
