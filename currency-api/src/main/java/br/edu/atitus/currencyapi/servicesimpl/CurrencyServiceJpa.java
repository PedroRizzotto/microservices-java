package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private final CurrencyRepository repository;
    private final BcbClientService bcbClientService;

    public CurrencyServiceJpa(
            CurrencyRepository repository,
            BcbClientService bcbClientService) {
        this.repository = repository;
        this.bcbClientService = bcbClientService;
    }

    @Value("${app.bcb.data-cotacao:10-15-2026}")
    private String dataCotacao;

    private Double getCotacaoEmReais(String moeda) {
        if (moeda.equalsIgnoreCase("BRL"))
            return 1.0;
        return bcbClientService.getCotacao(moeda.toUpperCase(), dataCotacao);
    }

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency) throws Exception {
        //Primeiro tenta pegar a cotação no Banco Central
        String fallbackReason;
        try {
            Double sourceRate = getCotacaoEmReais(sourceCurrency);
            Double targetRate = getCotacaoEmReais(targetCurrency);
            if (sourceRate != null && targetRate != null) {
                return new CurrencyResponse(
                        sourceCurrency,
                        targetCurrency,
                        sourceRate / targetRate
                );
            }
            fallbackReason = "sem cotação no BCB";
        } catch (Exception e) {
            fallbackReason = "BCB ERROR: " + e.getMessage();
        }

        //Fallback: cotação cadastrada no banco local
        var currency = repository.findBySourceCurrencyAndTargetCurrency(sourceCurrency, targetCurrency)
                .orElseThrow(() -> new EntityNotFoundException("Cotação não encontrada"));
        log.info("Cotação {}->{} obtida do banco local ({})", sourceCurrency, targetCurrency, fallbackReason);
        return CurrencyResponse.fromEntity(currency);
    }
}
