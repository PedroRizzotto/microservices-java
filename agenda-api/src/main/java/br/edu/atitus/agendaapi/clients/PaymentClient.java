package br.edu.atitus.agendaapi.clients;

import br.edu.atitus.agendaapi.dtos.PaymentRequest;
import br.edu.atitus.agendaapi.dtos.PaymentResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/internal/payments")
public interface PaymentClient {

    @PostExchange
    PaymentResponse pay(@RequestBody PaymentRequest request);
}
