package br.edu.atitus.agendaapi.components;

import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import br.edu.atitus.agendaapi.entities.AppointmentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class AppointmentRules {

    public static void requirePayable(AppointmentEntity appointment, Long customerId) {
        if (!appointment.getCustomerId().equals(customerId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado");
        if (appointment.getStatus() == AppointmentStatus.CANCELADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agendamento cancelado");
        if (appointment.getPaymentId() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agendamento já pago");
    }

    public static void requireCompletable(AppointmentEntity appointment, Long establishmentId) {
        if (!appointment.getEstablishmentId().equals(establishmentId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agendamento não encontrado");
        if (appointment.getStatus() != AppointmentStatus.AGENDADO)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Agendamento não pode ser concluído");
    }
}
