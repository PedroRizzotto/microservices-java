package br.edu.atitus.orderapi.clients;

import br.edu.atitus.orderapi.dtos.PaymentRequest;
import br.edu.atitus.orderapi.dtos.PaymentResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

@HttpExchange("/internal/payments")
public interface PaymentClient {

    @PostExchange
    PaymentResponse pay(@RequestBody PaymentRequest request);
}
