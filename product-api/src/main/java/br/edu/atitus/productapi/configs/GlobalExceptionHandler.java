package br.edu.atitus.productapi.configs;

import br.edu.atitus.productapi.dtos.ErrorResponse;
import br.edu.atitus.productapi.components.ErrorBodyParser;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

// Único ponto de tratamento de erros do serviço: todo erro sai como { status, error, message, timestamp }
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ErrorResponse> build(HttpStatusCode code, String message) {
        HttpStatus status = HttpStatus.resolve(code.value());
        String error = status != null ? status.getReasonPhrase() : "Error";
        return ResponseEntity.status(code)
                .body(new ErrorResponse(code.value(), error, message, OffsetDateTime.now().toString()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        return build(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({MissingRequestHeaderException.class, MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return build(HttpStatus.BAD_REQUEST, "Requisição inválida");
    }

    @ExceptionHandler({CallNotPermittedException.class, ResourceAccessException.class})
    public ResponseEntity<ErrorResponse> handleUnavailable(Exception e) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Serviço temporariamente indisponível");
    }

    // Erro 4xx de chamada interna: mesmo status, com a mensagem do corpo
    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<ErrorResponse> handleClientError(HttpClientErrorException e) {
        return build(e.getStatusCode(), ErrorBodyParser.extractMessage(e.getResponseBodyAsString()));
    }

    // Erros do próprio Spring MVC (rota inexistente, método não permitido...) mantêm seu status
    @ExceptionHandler(ServletException.class)
    public ResponseEntity<ErrorResponse> handleServlet(ServletException e) {
        if (e instanceof org.springframework.web.ErrorResponse er && er.getStatusCode().is4xxClientError()) {
            return build(er.getStatusCode(), er.getStatusCode().value() == 404
                    ? "Recurso não encontrado" : "Requisição inválida");
        }
        return handleUnknown(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e) {
        log.error("Erro não tratado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
    }
}
