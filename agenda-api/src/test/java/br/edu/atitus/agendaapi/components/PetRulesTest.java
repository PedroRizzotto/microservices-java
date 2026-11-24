package br.edu.atitus.agendaapi.components;

import br.edu.atitus.agendaapi.dtos.PetRequest;
import br.edu.atitus.agendaapi.entities.PetSpecies;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PetRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

    private static PetRequest pet(String name, String species, LocalDate birthDate, Double weight) {
        return new PetRequest(name, species, "SRD", birthDate, weight, null);
    }

    private static void assertBadRequest(PetRequest request, String message) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> PetRules.validate(request, TODAY));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals(message, e.getReason());
    }

    @Test
    void aceitaPetValidoEIgnoraMaiusculas() {
        assertEquals(PetSpecies.GATO, PetRules.validate(pet("Mingau", " gato ", LocalDate.of(2020, 1, 1), 4.5), TODAY));
    }

    @Test
    void aceitaEspecieComAcento() {
        assertEquals(PetSpecies.PASSARO, PetRules.validate(pet("Paçoca", "Pássaro", null, null), TODAY));
        assertEquals(PetSpecies.REPTIL, PetRules.validate(pet("Spike", "réptil", null, null), TODAY));
    }

    @Test
    void aceitaCamposOpcionaisVazios() {
        assertEquals(PetSpecies.CACHORRO, PetRules.validate(pet("Rex", "CACHORRO", null, null), TODAY));
    }

    @Test
    void recusaSemNome() {
        assertBadRequest(pet(" ", "GATO", null, null), "Nome do pet informado inválido");
    }

    @Test
    void recusaEspecieDesconhecida() {
        assertBadRequest(pet("Rex", "DRAGAO", null, null), "Espécie informada inválida");
        assertBadRequest(pet("Rex", null, null, null), "Espécie informada inválida");
    }

    @Test
    void recusaNascimentoNoFuturo() {
        assertBadRequest(pet("Rex", "GATO", TODAY.plusDays(1), null), "Data de nascimento informada inválida");
    }

    @Test
    void recusaPesoInvalido() {
        assertBadRequest(pet("Rex", "GATO", null, 0.0), "Peso informado inválido");
    }
}
