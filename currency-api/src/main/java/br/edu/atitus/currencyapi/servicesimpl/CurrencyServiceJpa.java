package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    public CurrencyServiceJpa(
            CurrencyRepository repository,
            BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Value("${server.port:8100}")
    private String serverPort;

    @Value("${app.bcb.data-cotacao:10-15-2026}")
    private String dataCotacao;

    private Double getCotacaoEmReais(String moeda) {
        if (moeda.equalsIgnoreCase("BRL"))
            return 1.0;
        return bcbClientService.getCotacao(moeda.toUpperCase(), dataCotacao);
    }

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency) throws Exception {
        String environment = "Currency API running in Port: " + serverPort;

        //Primeiro tenta pegar a cotação no Banco Central
        String fallbackReason;
        try {
            Double sourceRate = getCotacaoEmReais(sourceCurrency);
            Double targetRate = getCotacaoEmReais(targetCurrency);
            if (sourceRate != null && targetRate != null) {
                return new CurrencyResponse(
                        sourceCurrency,
                        targetCurrency,
                        sourceRate / targetRate,
                        environment + " - BCB"
                );
            }
            fallbackReason = "sem cotação no BCB";
        } catch (Exception e) {
            fallbackReason = "BCB ERROR: " + e.getMessage();
        }

        //Fallback: cotação cadastrada no banco local
        var currency = repository.findBySourceCurrencyAndTargetCurrency(sourceCurrency, targetCurrency)
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));
        return CurrencyResponse.fromEntity(currency, environment + " - Local DB (" + fallbackReason + ")");
    }
}
