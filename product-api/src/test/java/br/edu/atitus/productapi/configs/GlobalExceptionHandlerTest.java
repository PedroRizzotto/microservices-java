package br.edu.atitus.productapi.configs;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatusVira409() {
        var response = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.CONFLICT, "Estoque insuficiente"));

        assertEquals(409, response.getStatusCode().value());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Estoque insuficiente", response.getBody().message());
        assertDoesNotThrow(() -> java.time.OffsetDateTime.parse(response.getBody().timestamp()));
    }

    @Test
    void corpoInvalidoVira400() {
        var response = handler.handleBadRequest(new HttpMessageNotReadableException("JSON quebrado", null, null));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Bad Request", response.getBody().error());
        assertEquals("Requisição inválida", response.getBody().message());
    }

    @Test
    void erroDesconhecidoNaoVazaMensagem() {
        var response = handler.handleUnknown(new IllegalStateException("senha do banco: 123"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Erro interno", response.getBody().message());
        assertFalse(response.getBody().message().contains("senha"));
    }
}
