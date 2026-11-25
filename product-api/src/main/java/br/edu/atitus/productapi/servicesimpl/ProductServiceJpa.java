package br.edu.atitus.productapi.servicesimpl;

import br.edu.atitus.productapi.clients.AgendaClient;
import br.edu.atitus.productapi.components.ProductRules;
import br.edu.atitus.productapi.dtos.ProductRequest;
import br.edu.atitus.productapi.dtos.ProductResponse;
import br.edu.atitus.productapi.entities.ProductEntity;
import br.edu.atitus.productapi.repositories.ProductRepository;
import br.edu.atitus.productapi.services.ProductService;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductServiceJpa implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceJpa.class);

    private final ProductRepository repository;
    private final CurrencyClientService currencyClientService;
    private final AgendaClient agendaClient;

    public ProductServiceJpa(
            ProductRepository repository,
            CurrencyClientService currencyClientService,
            AgendaClient agendaClient) {
        this.repository = repository;
        this.currencyClientService = currencyClientService;
        this.agendaClient = agendaClient;
    }

    @Value("${app.promotion.message:Nenhuma Promoção Ativa}")
    private String promotionMessage;

    private ProductResponse getResponse(ProductEntity entity, String targetCurrency){
        double convertedValue = entity.getPrice();
        if (! entity.getCurrency().equalsIgnoreCase(targetCurrency)) {
            //Aqui vai fazer a comunicação com o microservice currency-api
            try {
                var currency = currencyClientService.getCurrency(
                        entity.getCurrency(),
                        targetCurrency
                );
                convertedValue = entity.getPrice() * currency.conversionRate();
            } catch (Exception e) {
                convertedValue = -1;
                log.warn("Falha ao converter o preço do produto {} para {}", entity.getId(), targetCurrency, e);
            }
        }
        return ProductResponse.fromEntity(
                entity,
                promotionMessage,
                targetCurrency,
                convertedValue
        );
    }


    @Override
    public ProductResponse findById(Long id, String targetCurrency) {
        var product = repository.findById(id)
                .filter(ProductEntity::isActive)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado"));
        return getResponse(product, targetCurrency);
    }

    @Override
    public Page<ProductResponse> search(Long establishmentId, String search, String category, Pageable pageable, String targetCurrency) {
        Specification<ProductEntity> spec = (root, query, cb) -> cb.isTrue(root.get("active"));
        if (establishmentId != null)
            spec = spec.and((root, query, cb) -> cb.equal(root.get("establishmentId"), establishmentId));
        if (search != null && !search.isBlank())
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("name")), "%" + search.trim().toLowerCase() + "%"));
        if (category != null && !category.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("category"), category.trim().toLowerCase()));

        return repository.findAll(spec, pageable)
                .map(entity -> getResponse(entity, targetCurrency));
    }

    private static String normalizeCategory(String category) {
        if (category == null || category.isBlank())
            return null;
        return category.trim().toLowerCase();
    }

    private void fill(ProductEntity product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setBrand(request.brand() != null ? request.brand().trim() : null);
        product.setModel(request.model() != null ? request.model().trim() : null);
        product.setDescription(request.description().trim());
        product.setCategory(normalizeCategory(request.category()));
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCurrency(request.currency().trim().toUpperCase());
        product.setImage(request.image() != null ? request.image().trim() : null);
    }

    @Override
    public ProductEntity create(Long providerId, ProductRequest request) {
        ProductRules.validate(request);

        Long establishmentId;
        try {
            establishmentId = agendaClient.getByProvider(providerId).id();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cadastre seu pet shop antes de cadastrar produtos");
        }

        var product = new ProductEntity();
        fill(product, request);
        product.setProviderId(providerId);
        product.setEstablishmentId(establishmentId);
        return repository.save(product);
    }

    @Override
    @Transactional
    public ProductEntity update(Long providerId, Long id, ProductRequest request) {
        var product = repository.findById(id).orElse(null);
        ProductRules.requireOwner(product, providerId);
        ProductRules.validate(request);

        fill(product, request);
        return repository.save(product);
    }

    @Override
    @Transactional
    public void deactivate(Long providerId, Long id) {
        var product = repository.findById(id).orElse(null);
        ProductRules.requireOwner(product, providerId);

        product.setActive(false);
        repository.save(product);
    }

    @Override
    public List<String> listCategories() {
        return repository.findActiveCategories();
    }

    @Override
    public Page<ProductEntity> findMine(Long providerId, Pageable pageable) {
        return repository.findAllByProviderIdAndActiveTrue(providerId, pageable);
    }
}
