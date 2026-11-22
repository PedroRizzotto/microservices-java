package br.edu.atitus.currencyapi.controllers;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.services.CurrencyService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("currencies")
public class CurrencyController {
    private final CurrencyService service;

    public CurrencyController(@Qualifier("currencyServiceJpa") CurrencyService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<CurrencyResponse> getCurrency(
            @RequestParam String source,
            @RequestParam String target
    ) throws Exception {
        var response = service.findBySourceCurrencyAndTargetCurrency(source, target);
        return ResponseEntity.ok(response);
    }
}
