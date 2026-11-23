package br.edu.atitus.orderapi.servicesimpl;

import java.util.List;
import java.util.Optional;

import br.edu.atitus.orderapi.clients.AuthClient;
import br.edu.atitus.orderapi.clients.CurrencyClient;
import br.edu.atitus.orderapi.clients.PaymentClient;
import br.edu.atitus.orderapi.clients.ProductClient;
import br.edu.atitus.orderapi.clients.StockClient;
import br.edu.atitus.orderapi.dtos.AddressResponse;
import br.edu.atitus.orderapi.dtos.PaymentRequest;
import br.edu.atitus.orderapi.dtos.PaymentResponse;
import br.edu.atitus.orderapi.dtos.ProductResponse;
import br.edu.atitus.orderapi.dtos.ReservedItemResponse;
import br.edu.atitus.orderapi.dtos.StockItemRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class IntegrationService {

    private final ProductClient productClient;
    private final StockClient stockClient;
    private final AuthClient authClient;
    private final PaymentClient paymentClient;
    private final CurrencyClient currencyClient;

    public IntegrationService(ProductClient productClient, StockClient stockClient, AuthClient authClient,
                              PaymentClient paymentClient, CurrencyClient currencyClient) {
        this.productClient = productClient;
        this.stockClient = stockClient;
        this.authClient = authClient;
        this.paymentClient = paymentClient;
        this.currencyClient = currencyClient;
    }

    @Retry(name = "product-api")
    @CircuitBreaker(name = "product-api")
    public Optional<ProductResponse> findProduct(Long productId, String targetCurrency) {
        try {
            return Optional.ofNullable(productClient.getProductById(productId, targetCurrency));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @CircuitBreaker(name = "stock-api")
    public List<ReservedItemResponse> reserveStock(List<StockItemRequest> items) {
        try {
            return stockClient.reserve(items);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), "Produto indisponível ou sem estoque suficiente");
        }
    }

    @CircuitBreaker(name = "stock-api")
    public void releaseStock(List<StockItemRequest> items) {
        stockClient.release(items);
    }

    @CircuitBreaker(name = "auth-api")
    public Optional<AddressResponse> findAddress(Long addressId, Long userId) {
        try {
            return Optional.ofNullable(authClient.getAddress(addressId, userId));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Retry(name = "payment-api")
    @CircuitBreaker(name = "payment-api")
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            return paymentClient.pay(request);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), "Pagamento recusado");
        }
    }

    @Retry(name = "currency-api")
    @CircuitBreaker(name = "currency-api")
    public double conversionRate(String source, String target) {
        return currencyClient.getCurrency(source, target).conversionRate();
    }
}
