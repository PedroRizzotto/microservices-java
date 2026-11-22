package br.edu.atitus.orderapi.clients;

import br.edu.atitus.orderapi.dtos.AddressResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange("/internal/addresses")
public interface AuthClient {

    @GetExchange("/{id}")
    AddressResponse getAddress(@PathVariable Long id, @RequestParam Long userId);
}
