package br.edu.atitus.productapi.components;

import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.entities.ProductEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ProductRules {

    public static void validate(ProductRequest r) {
        if (r == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requisição inválida");
        if (r.name() == null || r.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome inválido");
        if (r.description() == null || r.description().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição inválida");
        if (r.currency() == null || r.currency().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moeda inválida");
        if (r.price() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preço inválido");
        if (r.stock() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estoque inválido");
    }

    public static void requireOwner(ProductEntity p, Long providerId) {
        if (p == null || p.getProviderId() == null || !p.getProviderId().equals(providerId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
    }
}
