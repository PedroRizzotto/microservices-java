package br.edu.atitus.orderapi.servicesimpl;

import org.springframework.stereotype.Component;

@Component
public class CurrencyConverter {

    private static final String BRL = "BRL";

    private final IntegrationService integrationService;

    public CurrencyConverter(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    public double toBrl(double value, String currency) {
        if (BRL.equalsIgnoreCase(currency))
            return value;
        double converted = value * integrationService.conversionRate(currency.toUpperCase(), BRL);
        return Math.round(converted * 100.0) / 100.0;
    }
}
