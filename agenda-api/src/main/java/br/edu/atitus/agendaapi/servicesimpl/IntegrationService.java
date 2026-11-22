package br.edu.atitus.agendaapi.servicesimpl;

import br.edu.atitus.agendaapi.components.ErrorBodyParser;
import br.edu.atitus.agendaapi.clients.PaymentClient;
import br.edu.atitus.agendaapi.dtos.PaymentRequest;
import br.edu.atitus.agendaapi.dtos.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

// Chamadas a outros serviços com retry e circuit breaker.
// O pagamento é idempotente por referência, então repetir a chamada é seguro.
@Service
public class IntegrationService {

    private final PaymentClient paymentClient;

    public IntegrationService(PaymentClient paymentClient) {
        this.paymentClient = paymentClient;
    }

    @Retry(name = "payment-api")
    @CircuitBreaker(name = "payment-api")
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            return paymentClient.pay(request);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), ErrorBodyParser.extractMessage(e.getResponseBodyAsString()));
        }
    }
}
