# Fases 2 a 5: pagamento, carrinho e pedidos, agenda, ajustes para o front e observabilidade. Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Terminar o backend do petshop: payment-api simulado, carrinho de uma loja e pedidos com entrega e pagamento no order-api, pagamento e conclusão de agendamento, erro padronizado e CORS, observabilidade nos 7 serviços, README e coleção do Postman.

**Architecture:** Mesmo padrão da fase 1: regras puras em `components` com teste JUnit, serviços em `servicesimpl`, clientes `@HttpExchange` + `RestClient` `@LoadBalanced` (com `builder.clone()`), rotas internas em `/internal/**` sem rota no gateway, autorização por `X-User-Type` em cada serviço.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Cloud 2025.1 (Consul, Gateway), Resilience4j (`resilience4j-spring-boot4`, como no product-api), Flyway, PostgreSQL, JUnit 5 + Mockito, Docker Compose, Zipkin, Prometheus, Loki, Grafana Alloy, Grafana.

**Spec:** `docs/superpowers/specs/2026-11-22-petshop-marketplace-design.md` (seções 3 a 8 e "Pendências para a fase 3")

## Global Constraints

- Tipos: 1 = Cliente, 2 = Prestador. Carrinho, checkout, pagamento e cancelamento de pedido: só Cliente (senão 403). Rotas de prestador: só Prestador (senão 403).
- payment-api: porta **8700**, banco **db_payment**, nome de serviço `payment-api`.
- Status do pedido: `PENDING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELED`. Transições do prestador: `PAID→SHIPPED→DELIVERED`. Cancelamento só em `PENDING_PAYMENT`.
- Métodos de pagamento: `PIX`, `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO`. Status de pagamento sempre `APPROVED`. Referências: `ORDER`, `APPOINTMENT`.
- Total do pedido gravado em BRL no checkout.
- order-api NÃO faz retry nas chamadas de reserva/devolução de estoque; só timeout e circuit breaker. Pagamento é idempotente por referência, então pode ter retry.
- Pedidos e carrinho não dependem do produto estar ativo: pedido usa dados copiados; carrinho marca item indisponível quando o product-api responder 404.
- Mensagens em português via `ResponseStatusException`. Na Task 8 isso passa a sair no formato `{ "status", "error", "message", "timestamp" }`.
- EOL: auth-api guarda CRLF no índice; os outros módulos e arquivos novos, LF. Conferir com `git ls-files --eol`.
- Commits em português, sem linhas de atribuição. A data dos commits é redistribuída pelo controlador no fim; o implementador comita normalmente.
- Sem Docker no implementador: verificação manual fica para a verificação final do controlador.

## Review Focus

1. Cliente adiciona produto de outra loja ao carrinho: 409 e carrinho intacto; com `?replace=true`, carrinho esvaziado e item novo adicionado. (Task 3, testes em `CartRulesTest`.)
2. Checkout falha depois da reserva (ex.: erro ao gravar): estoque devolvido. (Task 4, teste com Mockito em `CheckoutServiceTest`.)
3. Pagar duas vezes o mesmo pedido: segunda chamada responde 409 e não cria pagamento novo; no payment-api, a mesma referência devolve o pagamento existente. (Tasks 1 e 5.)
4. Prestador mudando status de pedido de outra loja: 404; transição inválida (`PAID→DELIVERED`): 409. (Task 5, `OrderStatusRulesTest`.)
5. Produto desativado depois do pedido: `GET /ws/orders` continua funcionando. (Task 5, teste do mapeamento de leitura sem chamada ao product-api.)

---

### Task 1: payment-api (serviço novo)

**Files:**
- Create: `payment-api/` copiando o esqueleto do agenda-api (`pom.xml` com artifactId/name `payment-api`, `mvnw`, `mvnw.cmd`, `.mvn/`, `.gitignore`, `.gitattributes`, `.dockerignore`, `Dockerfile`), pacote `br.edu.atitus.paymentapi`
- Create: `PaymentApiApplication`, `entities/PaymentEntity`, `entities/PaymentMethod`, `entities/PaymentReferenceType`, `entities/PaymentStatus`, `dtos/PaymentRequest`, `repositories/PaymentRepository`, `components/PaymentRules`, `services/PaymentService`, `servicesimpl/PaymentServiceJpa`, `controllers/InternalPaymentController`, `controllers/WsPaymentController`
- Create: `src/main/resources/application.properties` (`spring.application.name=payment-api`, `server.port=8700`, datasource `db_payment`, Consul igual aos outros), `db/migration/V1__CreateTableTbPayment.sql`
- Test: `components/PaymentRulesTest`, `servicesimpl/PaymentServiceJpaTest` (Mockito)

**Interfaces:**
- Produces: `tb_payment (id SERIAL PK, reference_type VARCHAR(20) NOT NULL, reference_id BIGINT NOT NULL, user_id BIGINT NOT NULL, method VARCHAR(20) NOT NULL, amount DOUBLE PRECISION NOT NULL CHECK (amount > 0), currency VARCHAR(3) NOT NULL DEFAULT 'BRL', status VARCHAR(20) NOT NULL, transaction_code VARCHAR(36) NOT NULL UNIQUE, created_at TIMESTAMP NOT NULL, UNIQUE (reference_type, reference_id))`
- Produces: `record PaymentRequest(String referenceType, Long referenceId, Long userId, String method, double amount)`
- Produces: `PaymentRules.validate(PaymentRequest r)`: 400 para referenceType/method fora dos enums (aceita minúsculas), `referenceId`/`userId` nulos, `amount <= 0`.
- Produces: `PaymentService.pay(PaymentRequest r) -> PaymentEntity`: se já existe pagamento para `(referenceType, referenceId)`, devolve o existente sem criar outro; senão cria `APPROVED` com `transactionCode = UUID`, `currency = "BRL"`, `createdAt = agora`. `Page<PaymentEntity> listMine(Long userId, Pageable p)`.
- Produces (HTTP): `POST /internal/payments` → 201 com o pagamento (200 quando já existia); `GET /ws/payments` → página do usuário (`X-User-Id`), ordenada por `createdAt` desc, tamanho 10.
- JSON do pagamento: `id, referenceType, referenceId, method, amount, currency, status, transactionCode, createdAt`.

- [ ] **Step 1:** testes `PaymentRulesTest` (`aceitaPagamentoValido`, `aceitaMetodoMinusculo`, `recusaMetodoInvalido` → 400, `recusaValorZero` → 400, `recusaReferenciaInvalida` → 400) e `PaymentServiceJpaTest` (`criaPagamentoAprovado`: status APPROVED, currency BRL, transactionCode não nulo; `devolveExistenteParaMesmaReferencia`: repositório retorna existente → `save` nunca chamado).
- [ ] **Step 2:** rodar e ver falhar (compilação).
- [ ] **Step 3:** implementar.
- [ ] **Step 4:** `./mvnw test` e `./mvnw -q compile` no payment-api passam.
- [ ] **Step 5:** commit `Criado payment-api para pagamento simulado`.

---

### Task 2: payment-api na infraestrutura

**Files:**
- Modify: `gateway-api/src/main/java/br/edu/atitus/gatewayapi/configs/GatewayConfig.java` (rota `payment-api-ws` para `/ws/payments/**` → `lb://payment-api`)
- Modify: `start_consul.bat`, `start_consul.sh` (bloco do payment-api: porta 8700 e datasource `db_payment`, igual ao do agenda-api)
- Modify: `docker/config/load-config.sh` (mesmo bloco, host `payment-db:5432`)
- Modify: `docker/docker-compose.yml` (`payment-db` postgres:17 com volume `payment_data` e healthcheck igual aos outros; `payment-api` igual ao `agenda-api`)
- Modify: `README.md` (serviço payment-api na lista, `CREATE DATABASE db_payment;`, rota `GET /ws/payments`, ordem de subida)

- [ ] **Step 1:** aplicar as mudanças seguindo exatamente o padrão do agenda-api em cada arquivo.
- [ ] **Step 2:** `./mvnw -q compile` no gateway-api passa; `docker compose -f docker/docker-compose.yml config -q` passa se o docker estiver disponível (senão, conferir o YAML lendo).
- [ ] **Step 3:** commit `Adicionado payment-api no gateway, no Consul e no docker`.

---

### Task 3: order-api: modelo novo, regras do carrinho e do status

**Files:**
- Create: `order-api/src/main/resources/db/migration/V2__CartAndOrderFlow.sql`
- Create: `entities/CartEntity`, `entities/CartItemEntity`, `entities/OrderStatus`
- Modify: `entities/OrderEntity`, `entities/OrderItemEntity`
- Create: `components/CartRules`, `components/OrderStatusRules`
- Create: `repositories/CartRepository`
- Test: `components/CartRulesTest`, `components/OrderStatusRulesTest`
- Modify: `order-api/pom.xml` (adicionar `resilience4j-spring-boot4`, mesma versão e forma do product-api; `spring-boot-starter-aop` se o product-api tiver)

**Interfaces:**
- Produces: migração V2:

```sql
CREATE TABLE tb_cart (
    id SERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL UNIQUE,
    establishment_id BIGINT,
    updated_at TIMESTAMP NOT NULL
);
CREATE TABLE tb_cart_item (
    id SERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL REFERENCES tb_cart(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE (cart_id, product_id)
);
DELETE FROM tb_order;
ALTER TABLE tb_order
    ADD COLUMN establishment_id BIGINT NOT NULL,
    ADD COLUMN provider_id BIGINT NOT NULL,
    ADD COLUMN status VARCHAR(20) NOT NULL,
    ADD COLUMN total_amount DOUBLE PRECISION NOT NULL,
    ADD COLUMN delivery_zip_code VARCHAR(8) NOT NULL,
    ADD COLUMN delivery_street VARCHAR(255) NOT NULL,
    ADD COLUMN delivery_number VARCHAR(20) NOT NULL,
    ADD COLUMN delivery_complement VARCHAR(255),
    ADD COLUMN delivery_district VARCHAR(255) NOT NULL,
    ADD COLUMN delivery_city VARCHAR(255) NOT NULL,
    ADD COLUMN delivery_state CHAR(2) NOT NULL,
    ADD COLUMN payment_id BIGINT,
    ADD COLUMN paid_at TIMESTAMP;
ALTER TABLE tb_order_item ADD COLUMN product_name VARCHAR(255) NOT NULL DEFAULT '';
```

- `OrderItemEntity`: grava `productId, productName, quantity, priceAtPurchase` (BRL) e `currencyAtPurchase` (`"BRL"`). Os campos transientes `product` e `convertedPriceAtPruchase` são removidos; entra `@Transient double convertedPrice`.
- `OrderEntity`: campos novos acima (endereço como `deliveryZipCode` etc.), `status` com `@Enumerated(STRING)`, `@Transient double totalConvertedPrice` mantido; `totalPrice` transiente sai (vira `totalAmount` persistido). `customerId` e `providerId` com `@JsonIgnore`.
- Produces: `enum OrderStatus { PENDING_PAYMENT, PAID, SHIPPED, DELIVERED, CANCELED }`.
- Produces: `CartRules.addItem(CartEntity cart, Long productId, Long establishmentId, int quantity, boolean replace)`:
  - quantidade `<= 0` → 400 "Quantidade inválida";
  - carrinho com itens de outra loja e `replace == false` → 409 "O carrinho tem produtos de outra loja";
  - outra loja com `replace == true` → limpa os itens e troca a loja;
  - produto já no carrinho → soma (`Math.addExact`, estouro → 400);
  - carrinho vazio → assume a loja do produto.
- Produces: `CartRules.setQuantity(CartEntity cart, Long productId, int quantity)`: `0` remove; negativo → 400; produto ausente → 404 "Item não está no carrinho". Ao ficar vazio, `establishmentId = null`.
- Produces: `OrderStatusRules.requireProviderTransition(OrderStatus from, OrderStatus to)`: só `PAID→SHIPPED` e `SHIPPED→DELIVERED`, senão 409 "Transição de status inválida". `OrderStatusRules.requireCancelable(OrderStatus s)`: só `PENDING_PAYMENT`, senão 409 "Pedido não pode ser cancelado". `OrderStatusRules.requirePayable(OrderStatus s)`: só `PENDING_PAYMENT`, senão 409 "Pedido não está aguardando pagamento".

- [ ] **Step 1:** testes. `CartRulesTest`: `adicionaNoCarrinhoVazioAssumeLoja`, `somaProdutoRepetido`, `recusaOutraLojaSemReplace` (409, itens intactos), `substituiComReplace`, `recusaQuantidadeInvalida`, `setQuantityZeroRemoveELiberaLoja`, `setQuantityItemAusente404`. `OrderStatusRulesTest`: `transicoesValidasDoPrestador`, `recusaPularStatus` (PAID→DELIVERED 409), `recusaVoltarStatus`, `cancelaSoPendente`, `pagaSoPendente`.
- [ ] **Step 2:** rodar e ver falhar.
- [ ] **Step 3:** implementar entidades, migração, repositório e regras. O `OrderController`/`OrderServiceJpa` atuais podem ficar quebrados para compilar? Não: ajuste-os o mínimo para compilar (o fluxo novo vem nas Tasks 4 e 5).
- [ ] **Step 4:** `./mvnw test` passa.
- [ ] **Step 5:** commit `Modelo de carrinho e status do pedido no order-api`.

---

### Task 4: order-api: clientes, carrinho e checkout

**Files:**
- Modify: `clients/ProductClient` + `dtos/ProductResponse` (campos atuais do product-api: `id, name, brand, model, description, category, currency, price, stock, image, establishmentId, environment, promotionMessage, targetCurrency, convertedPrice`)
- Create: `clients/StockClient` (`POST /internal/products/stock/reserve` e `/release`), `dtos/StockItemRequest(Long productId, int quantity)`, `dtos/ReservedItemResponse(Long productId, String name, int quantity, double price, String currency, Long establishmentId, Long providerId)`
- Create: `clients/AuthClient` (`GET /internal/addresses/{id}?userId=`), `dtos/AddressResponse(Long id, String label, String zipCode, String street, String number, String complement, String district, String city, String state)`
- Create: `clients/PaymentClient` (`POST /internal/payments`), `dtos/PaymentRequest`, `dtos/PaymentResponse(Long id, String status, String transactionCode)`
- Create: `configs/...ClientConfig` para os novos clientes (base URLs `http://product-api`, `http://auth-api`, `http://payment-api`; timeouts 1s/2s; usar `builder.clone()`)
- Create: `servicesimpl/IntegrationService` (envolve as chamadas com `@CircuitBreaker`; `@Retry` só em product GET, currency e payment; reserve/release sem retry), `servicesimpl/CurrencyConverter` (`toBrl(double value, String currency)`, usa o `CurrencyClient` existente; BRL não chama nada)
- Create: `services/CartService`, `servicesimpl/CartServiceJpa`, `controllers/CartController`, `dtos/CartItemRequest(Long productId, int quantity)`, `dtos/CartQuantityRequest(int quantity)`, `dtos/CartResponse`, `dtos/CartItemResponse`
- Create: `services/CheckoutService`, `servicesimpl/CheckoutServiceJpa`, `dtos/CheckoutRequest(Long addressId)`
- Create: `servicesimpl/AuthorizationService` (`requireCustomer` tipo 1, `requireProvider` tipo 2, 403)
- Modify: `docker/config/load-config.sh`, `start_consul.bat`, `start_consul.sh` (resilience4j das instâncias `product-api`, `stock-api`, `auth-api`, `payment-api` no order-api: mesmos valores das do currency-api no product-api; retry só em `product-api` e `payment-api`)
- Test: `servicesimpl/CheckoutServiceJpaTest` (Mockito)

**Interfaces:**
- `CartResponse(Long establishmentId, List<CartItemResponse> items, double total, String targetCurrency)`; `CartItemResponse(Long productId, String name, String image, int quantity, double unitPrice, double subtotal, boolean available)`. Item com 404 no product-api: `available = false`, preço 0 e fora do total.
- Rotas (todas `requireCustomer`):
  - `GET /ws/orders/cart?targetCurrency=BRL`
  - `POST /ws/orders/cart/items?replace=false` corpo `CartItemRequest`: busca o produto (404 → 404 "Produto não encontrado") para saber a loja; não reserva estoque.
  - `PATCH /ws/orders/cart/items/{productId}` corpo `CartQuantityRequest`
  - `DELETE /ws/orders/cart/items/{productId}`, `DELETE /ws/orders/cart` (204)
  - `POST /ws/orders/checkout` corpo `CheckoutRequest` → 201 com o pedido
- `CheckoutServiceJpa.checkout(Long customerId, Long addressId) -> OrderEntity`, `@Transactional`, nesta ordem:
  1. carrinho vazio → 400 "Carrinho vazio";
  2. endereço pelo auth-api (404 → 404 "Endereço não encontrado");
  3. reserva no product-api (404/409 repassados com a mensagem do product-api);
  4. monta o pedido com os itens reservados: `priceAtPurchase` = preço convertido para BRL, `productName` = nome reservado; `establishmentId`/`providerId` do primeiro item; `totalAmount` = soma; status `PENDING_PAYMENT`; endereço copiado; `orderDate` = agora;
  5. salva o pedido e esvazia o carrinho;
  6. qualquer exceção depois do passo 3 → chama `release` com os mesmos itens e relança.
- `CheckoutServiceJpaTest`: `checkoutCriaPedidoPendenteEEsvaziaCarrinho`, `carrinhoVazio400`, `devolveEstoqueSeFalharDepoisDaReserva` (repositório lança ao salvar → `release` chamado uma vez com os itens), `naoDevolveSeReservaFalhar`.
- O `POST /ws/orders` antigo é removido.

- [ ] **Step 1:** escrever `CheckoutServiceJpaTest`.
- [ ] **Step 2:** rodar e ver falhar.
- [ ] **Step 3:** implementar clientes, configs, serviços e controller; atualizar as configs do Consul.
- [ ] **Step 4:** `./mvnw test` e `./mvnw -q compile` passam.
- [ ] **Step 5:** commit `Carrinho de uma loja e checkout com reserva de estoque no order-api`.

---

### Task 5: order-api: pagar, cancelar, listar e pedidos do prestador

**Files:**
- Modify: `controllers/OrderController`, `services/OrderService`, `servicesimpl/OrderServiceJpa`, `repositories/OrderRepository`
- Test: `servicesimpl/OrderServiceJpaTest` (Mockito)
- Modify: `README.md` (fluxo de compra completo: endereço → carrinho → checkout → pagar → prestador envia; remover o exemplo antigo de `POST /ws/orders`)

**Interfaces:**
- `OrderService`:
  - `Page<OrderEntity> listMine(Long customerId, String targetCurrency, Pageable p)`
  - `OrderEntity getMine(Long customerId, Long id, String targetCurrency)` (de outro cliente → 404 "Pedido não encontrado")
  - `OrderEntity pay(Long customerId, Long id, String method)`: `requirePayable`; chama o payment-api com `referenceType = "ORDER"`, `amount = totalAmount`; grava `paymentId`, `paidAt`, status `PAID`. Falha do payment-api → 503 "Pagamento indisponível, tente novamente", e o pedido continua pendente.
  - `OrderEntity cancel(Long customerId, Long id)`: `requireCancelable`; devolve o estoque; status `CANCELED`.
  - `Page<OrderEntity> listReceived(Long providerId, OrderStatus status, Pageable p)` (status opcional)
  - `OrderEntity updateStatus(Long providerId, Long id, OrderStatus to)`: pedido de outro prestador → 404; `requireProviderTransition`.
- Leitura **sem** chamar o product-api: os itens vêm do banco; se `targetCurrency != BRL`, converte `priceAtPurchase` e `totalAmount` com o currency-api em `convertedPrice`/`totalConvertedPrice`; se BRL, copia.
- Rotas:
  - Cliente: `GET /ws/orders?targetCurrency=BRL`, `GET /ws/orders/{id}?targetCurrency=BRL`, `POST /ws/orders/{id}/pay` corpo `{ "method": "PIX" }`, `PATCH /ws/orders/{id}/cancel`
  - Prestador: `GET /ws/orders/received?status=PAID`, `PATCH /ws/orders/{id}/status` corpo `{ "status": "SHIPPED" }`
- `OrderServiceJpaTest`: `pagarPedidoPendenteViraPago`, `pagarDuasVezes409` (segunda chamada não chama o payment-api), `cancelarDevolveEstoque`, `cancelarPagoRecusa409`, `prestadorDeOutraLoja404`, `listarNaoChamaProductApi` (ProductClient mock nunca chamado).

- [ ] **Step 1:** testes.
- [ ] **Step 2:** ver falhar.
- [ ] **Step 3:** implementar e atualizar o README.
- [ ] **Step 4:** `./mvnw test` passa.
- [ ] **Step 5:** commit `Pagamento, cancelamento e pedidos recebidos pelo prestador no order-api`.

---

### Task 6: agenda-api: pagar e concluir agendamento

**Files:**
- Create: `agenda-api/src/main/resources/db/migration/V2__AppointmentPayment.sql` (`ALTER TABLE tb_appointment ADD COLUMN payment_id BIGINT, ADD COLUMN paid_at TIMESTAMP;`)
- Modify: `entities/AppointmentStatus` (novo valor `CONCLUIDO`), `entities/AppointmentEntity`, `services/AppointmentService`, `servicesimpl/AppointmentServiceJpa`, `controllers/WsAppointmentController`, `controllers/WsProviderController`
- Create: `clients/PaymentClient`, `configs/PaymentClientConfig` (com o builder `@LoadBalanced` como no product-api), `dtos/PaymentRequest`, `dtos/PaymentResponse`, `dtos/PayRequest(String method)`, `components/AppointmentRules`
- Modify: `agenda-api/pom.xml` (resilience4j como no product-api), configs do Consul (`load-config.sh`, `start_consul.*`) com retry e circuit breaker da instância `payment-api` na agenda-api
- Modify: `README.md` (rotas novas da agenda)
- Test: `components/AppointmentRulesTest`

**Interfaces:**
- `AppointmentRules.requirePayable(AppointmentEntity a, Long customerId)`: de outro cliente → 404 "Agendamento não encontrado"; `CANCELADO` → 409 "Agendamento cancelado"; já pago → 409 "Agendamento já pago".
- `AppointmentRules.requireCompletable(AppointmentEntity a, Long establishmentId)`: de outra loja → 404; só `AGENDADO` → senão 409 "Agendamento não pode ser concluído".
- Rotas: `POST /ws/agenda/appointments/{id}/pay` corpo `{ "method": "PIX" }` (só Cliente) → agendamento com `paymentId`/`paidAt`; `PATCH /ws/agenda/appointments/{id}/complete` (só Prestador) → status `CONCLUIDO`.
- Falha do payment-api → 503 "Pagamento indisponível, tente novamente".
- `AppointmentRulesTest`: `pagaAgendado`, `recusaPagarCancelado`, `recusaPagarDuasVezes`, `recusaPagarDeOutroCliente404`, `concluiAgendado`, `recusaConcluirDeOutraLoja404`, `recusaConcluirCancelado`.

- [ ] **Step 1:** testes. **Step 2:** ver falhar. **Step 3:** implementar. **Step 4:** `./mvnw test` passa. **Step 5:** commit `Pagamento e conclusao de agendamento na agenda-api`.

---

### Task 7: erro padronizado e CORS

**Files:**
- Create em cada um dos 6 serviços de negócio (auth, product, currency, order, agenda, payment): `configs/GlobalExceptionHandler` (`@RestControllerAdvice`) e `dtos/ErrorResponse(int status, String error, String message, String timestamp)`
- Modify: remover os `@ExceptionHandler` dos controllers desses serviços (o AuthController mantém o comportamento: credencial inválida → **401** "Usuário inexistente ou senha inválida")
- Modify: `gateway-api` (CORS global: origens `*`, métodos GET/POST/PUT/PATCH/DELETE/OPTIONS, headers `*`; requisição `OPTIONS` não passa pelo AuthFilter)
- Test: `GlobalExceptionHandlerTest` em product-api (unit, chamando os métodos do handler)

**Interfaces:**
- Mapeamento do handler:
  - `ResponseStatusException` → status e `reason`
  - `EntityNotFoundException` → 404
  - `MissingRequestHeaderException`, `MissingServletRequestParameterException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException` → 400 "Requisição inválida"
  - `CallNotPermittedException` e `ResourceAccessException` → 503 "Serviço temporariamente indisponível"
  - `HttpClientErrorException` de chamada interna → mesmo status, com a mensagem do corpo
  - qualquer outra → 500 "Erro interno", sem detalhes da exceção
- `timestamp` em ISO-8601; `error` = nome do status (`"Conflict"`).
- Teste: `responseStatusVira409`, `corpoInvalidoVira400`, `erroDesconhecidoNaoVazaMensagem`.

- [ ] **Step 1:** teste. **Step 2:** ver falhar. **Step 3:** implementar nos 6 serviços e no gateway. **Step 4:** `./mvnw test` em todos os módulos alterados passa. **Step 5:** commit `Erro padronizado nos servicos e CORS no gateway`.

---

### Task 8: observabilidade (roteiro da disciplina)

Roteiro: `C:\Users\Pedro\Downloads\observabilidade.html` (etapas 1 a 6). Aplicar em **7** serviços: gateway, auth, product, currency, order, agenda, payment.

**Files:**
- Modify: `pom.xml` dos 7 serviços (`micrometer-registry-prometheus` runtime e `spring-boot-starter-zipkin`)
- Modify: `docker/config/load-config.sh` (bloco "OBSERVABILIDADE" do roteiro em `config/application`) e `start_consul.*` (mesmas chaves com endpoint `http://localhost:9411/api/v2/spans`)
- Create: `docker/prometheus/prometheus.yml` (7 jobs, `/actuator/prometheus`, alvos `nome:porta`)
- Create: configs do Loki, do Alloy e do provisionamento do Grafana conforme as etapas 4 e 5 do roteiro (`docker/loki/`, `docker/alloy/`, `docker/grafana/provisioning/...`)
- Create: dashboard do Grafana (etapa 6) com requisições por serviço, taxa de erro, latência p95 e memória JVM
- Modify: `docker/docker-compose.yml` (zipkin, prometheus, loki, alloy, grafana, volumes)
- Modify: logs com Trace ID (Correlation ID) conforme o roteiro (padrão de log com `traceId`/`spanId` em `config/application`)
- Modify: `README.md` (seção Observabilidade: portas e como usar)

- [ ] **Step 1:** aplicar o roteiro, adaptado aos 7 serviços.
- [ ] **Step 2:** `./mvnw -q compile` em cada serviço; `docker compose -f docker/docker-compose.yml config -q` passa.
- [ ] **Step 3:** commit `Observabilidade com Zipkin, Prometheus, Loki, Alloy e Grafana`.

---

### Task 9: README final e coleção do Postman

**Files:**
- Modify: `README.md` (revisão completa: serviços e portas, tipos de usuário, todas as rotas públicas/protegidas/internas, fluxo de compra e de agendamento, formato de erro, como rodar local e com docker, observabilidade)
- Create: `docs/postman/petshop.postman_collection.json` (variáveis `baseUrl`, `tokenCliente`, `tokenPrestador`, `productId`, `orderId`, `addressId`; pastas Auth, Endereços, Pet shop, Produtos, Carrinho e pedidos, Agendamentos, Pagamentos; scripts de teste que salvam token e ids nas variáveis)

- [ ] **Step 1:** escrever. **Step 2:** validar que o JSON é válido (`python -m json.tool`). **Step 3:** commit `README final e colecao do Postman com o fluxo completo`.

---

## Verificação final (controlador)

- [ ] `./mvnw test` em todos os módulos.
- [ ] `docker compose up --build -d` em `docker/` e, pelo gateway, o fluxo: cadastro de cliente e prestador → pet shop → produto → endereço → carrinho → item de outra loja (409) → checkout → pagar → pagar de novo (409) → prestador SHIPPED e DELIVERED → agendamento → pagar → concluir.
- [ ] Zipkin mostra um trace gateway → order → product/payment; Prometheus com os 7 alvos UP; Grafana com as fontes e o dashboard.
