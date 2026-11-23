package br.edu.atitus.agendaapi.components;

import br.edu.atitus.agendaapi.entities.AppointmentEntity;
import br.edu.atitus.agendaapi.entities.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppointmentRulesTest {

    private static final Long CUSTOMER = 1L;
    private static final Long ESTABLISHMENT = 10L;

    private static AppointmentEntity appointment(AppointmentStatus status) {
        AppointmentEntity a = new AppointmentEntity();
        a.setCustomerId(CUSTOMER);
        a.setEstablishmentId(ESTABLISHMENT);
        a.setStatus(status);
        return a;
    }

    @Test
    void pagaAgendado() {
        assertDoesNotThrow(() -> AppointmentRules.requirePayable(appointment(AppointmentStatus.AGENDADO), CUSTOMER));
    }

    @Test
    void recusaPagarCancelado() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> AppointmentRules.requirePayable(appointment(AppointmentStatus.CANCELADO), CUSTOMER));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals("Agendamento cancelado", e.getReason());
    }

    @Test
    void recusaPagarDuasVezes() {
        AppointmentEntity a = appointment(AppointmentStatus.AGENDADO);
        a.setPaymentId(5L);
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> AppointmentRules.requirePayable(a, CUSTOMER));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals("Agendamento já pago", e.getReason());
    }

    @Test
    void recusaPagarDeOutroCliente404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> AppointmentRules.requirePayable(appointment(AppointmentStatus.AGENDADO), 2L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertEquals("Agendamento não encontrado", e.getReason());
    }

    @Test
    void concluiAgendado() {
        assertDoesNotThrow(() -> AppointmentRules.requireCompletable(appointment(AppointmentStatus.AGENDADO), ESTABLISHMENT));
    }

    @Test
    void recusaConcluirDeOutraLoja404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> AppointmentRules.requireCompletable(appointment(AppointmentStatus.AGENDADO), 99L));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        assertEquals("Agendamento não encontrado", e.getReason());
    }

    @Test
    void recusaConcluirCancelado() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> AppointmentRules.requireCompletable(appointment(AppointmentStatus.CANCELADO), ESTABLISHMENT));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals("Agendamento não pode ser concluído", e.getReason());
    }

    @Test
    void cancelaAgendado() {
        assertDoesNotThrow(() -> AppointmentRules.requireCancelable(appointment(AppointmentStatus.AGENDADO)));
    }

    @Test
    void recusaCancelarConcluido() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> AppointmentRules.requireCancelable(appointment(AppointmentStatus.CONCLUIDO)));
        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertEquals("Agendamento já concluído", e.getReason());
    }
}
