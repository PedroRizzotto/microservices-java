package br.edu.atitus.productapi.clients;

import br.edu.atitus.productapi.dtos.EstablishmentResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange
public interface AgendaClient {

    @GetExchange("/internal/establishments/by-provider/{providerId}")
    EstablishmentResponse getByProvider(@PathVariable Long providerId);
}
