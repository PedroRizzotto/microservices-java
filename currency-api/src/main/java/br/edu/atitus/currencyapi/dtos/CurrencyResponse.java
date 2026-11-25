package br.edu.atitus.currencyapi.dtos;

import br.edu.atitus.currencyapi.entities.CurrencyEntity;

public record CurrencyResponse(
        String sourceCurrency,
        String targetCurrency,
        Double conversionRate
) {
    public static CurrencyResponse fromEntity(CurrencyEntity entity) {
        return new CurrencyResponse(
                entity.getSourceCurrency(),
                entity.getTargetCurrency(),
                entity.getConversionRate()
        );
    }
}