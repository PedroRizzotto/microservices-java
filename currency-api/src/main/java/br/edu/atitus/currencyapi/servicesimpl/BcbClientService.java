package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BcbClientService {

    private final BcbClient bcbClient;

    public BcbClientService(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    //Retorna o valor de 1 unidade da moeda em reais, ou null quando o BCB não tem cotação (null não vai pro cache)
    @Cacheable(
            value = "bcb-api",
            key = "#moeda + '-' + #dataCotacao",
            unless = "#result == null"
    )
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Double getCotacao(String moeda, String dataCotacao) {
        var response = bcbClient.getCotacao(moeda, dataCotacao);
        if (response == null || response.value() == null || response.value().isEmpty())
            return null;
        //O BCB pode devolver mais de um boletim no dia, o último é o de fechamento
        return response.value().getLast().cotacaoVenda();
    }
}
