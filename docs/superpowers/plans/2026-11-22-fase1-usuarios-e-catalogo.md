# Fase 1: usuários, endereços e catálogo do prestador. Plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tirar o papel de admin, dar endereços ao usuário e fazer o produto pertencer ao prestador e ao pet shop dele, com estoque e rotas internas de reserva.

**Architecture:** Mudanças em auth-api, agenda-api e product-api, seguindo a estrutura atual (`controllers / dtos / entities / repositories / services / servicesimpl / components / clients / configs`). Regras puras ficam em classes estáticas em `components` com teste JUnit, no estilo do `AgendaRules`. Comunicação entre serviços usa `@HttpExchange` + `RestClient` com `@LoadBalanced`, como o `CurrencyClient` do product-api. Rotas internas ficam em `/internal/**`, que o gateway não roteia.

**Tech Stack:** Java 25, Spring Boot 4.1, Spring Cloud 2025.1 (Consul), Spring Data JPA, Flyway, PostgreSQL, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-11-22-petshop-marketplace-design.md` (seções 1, 2 e a rota interna da seção 5)

Esta é a fase 1 de 5. Próximos planos: payment-api; order-api (carrinho, checkout, pedidos); agenda-api (pagamento e conclusão) com ajustes para o front; observabilidade e fechamento.

## Global Constraints

- Tipos de usuário: `Common` código **1** (Cliente), `Provider` código **2** (Prestador). Nada de código 0.
- Headers vindos do gateway: `X-User-Id` (Long), `X-User-Type` (Integer), `X-User-Email` (String).
- Rotas protegidas começam com `/ws/`; rotas internas com `/internal/`; nenhuma rota nova no gateway nesta fase.
- Mensagens de erro em português, com `ResponseStatusException(HttpStatus.X, "mensagem")`, como já é feito.
- Migrações Flyway novas continuam a numeração de cada serviço (auth: V5; product: V3).
- Testes: `.\mvnw.cmd test` dentro da pasta do serviço (Windows) ou `./mvnw test`.
- Commits em português, no estilo do histórico (ex.: "Adicionado ..."), **com data entre 2026-11-20 19:42 e o momento atual, sempre crescente**: definir `GIT_AUTHOR_DATE` e `GIT_COMMITTER_DATE` no comando de commit.

## Review Focus

1. Cliente acessando endereço de outro usuário por id: deve receber 404, não o endereço. (Task 2, teste no `AddressServiceJpa` via regra `AddressRules.requireOwner`.)
2. Reserva com o mesmo produto repetido na lista (`[{1,2},{1,3}]`): deve somar para 5 e validar o estoque contra 5. (Task 5, teste `reserveSomaProdutoRepetido`.)
3. Reserva com produtos de lojas diferentes: 409 e nenhum estoque baixado. (Task 5, teste `reserveRecusaLojasDiferentes`.)
4. Prestador sem pet shop cadastrado criando produto: 400 com mensagem clara, não 500. (Task 6, verificação manual.)
5. Prestador editando ou desativando produto de outro prestador: 404. (Task 6, teste `requireOwnerRecusaOutroPrestador`.)

---

### Task 1: auth-api: só Cliente e Prestador

**Files:**
- Modify: `auth-api/src/main/java/br/edu/atitus/authapi/entities/UserType.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/components/UserTypeConverter.java`
- Modify: `auth-api/src/main/java/br/edu/atitus/authapi/entities/UserEntity.java` (campo `type`)
- Modify: `auth-api/src/main/java/br/edu/atitus/authapi/components/JwtUtil.java:28`
- Modify: `auth-api/src/main/java/br/edu/atitus/authapi/configs/ConfigSecurity.java`
- Create: `auth-api/src/main/resources/db/migration/V5__RemoveAdmin.sql`
- Test: `auth-api/src/test/java/br/edu/atitus/authapi/components/UserTypeConverterTest.java`

**Interfaces:**
- Produces: `UserType { Common(1), Provider(2); int getCode(); static UserType fromCode(int code) }`; `fromCode` com código desconhecido lança `IllegalArgumentException`.
- Produces: `UserTypeConverter implements AttributeConverter<UserType, Integer>` com `@Converter(autoApply = true)`.

- [ ] **Step 1: Escrever o teste que falha**

```java
class UserTypeConverterTest {
    private final UserTypeConverter converter = new UserTypeConverter();

    @Test
    void gravaCodigoFixo() {
        assertEquals(1, converter.convertToDatabaseColumn(UserType.Common));
        assertEquals(2, converter.convertToDatabaseColumn(UserType.Provider));
    }

    @Test
    void leCodigoFixo() {
        assertEquals(UserType.Common, converter.convertToEntityAttribute(1));
        assertEquals(UserType.Provider, converter.convertToEntityAttribute(2));
    }

    @Test
    void recusaCodigoDeAdmin() {
        assertThrows(IllegalArgumentException.class, () -> UserType.fromCode(0));
    }

    @Test
    void nuloViraNulo() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd auth-api; .\mvnw.cmd test -Dtest=UserTypeConverterTest`
Expected: erro de compilação (`UserTypeConverter` não existe).

- [ ] **Step 3: Implementar**

- `UserType` com o campo `code` e `fromCode`.
- `UserTypeConverter`.
- `JwtUtil.generateToken` passa `type.getCode()` em vez de `type.ordinal()`.
- `ConfigSecurity`: trocar a regra para `.anyRequest().permitAll()`. Quem valida o token é o gateway; sem isso as rotas `/ws/auth/**` da Task 2 voltam 403 do Spring Security.
- `V5__RemoveAdmin.sql`:

```sql
DELETE FROM tb_user WHERE type = 0;
ALTER TABLE tb_user DROP CONSTRAINT tb_user_type_check;
ALTER TABLE tb_user ADD CONSTRAINT tb_user_type_check CHECK (type IN (1, 2));
```

- [ ] **Step 4: Rodar e ver passar**

Run: `.\mvnw.cmd test -Dtest=UserTypeConverterTest`
Expected: `Tests run: 4, Failures: 0, Errors: 0`

- [ ] **Step 5: Verificar pelo gateway** (Consul, Postgres, auth-api e gateway rodando)

- `POST /auth/signin` com `provider@provider.dev`/`teste`: 200. Decodificar o token (jwt.io) e ver `"type": 2`.
- `POST /auth/signin` com `admin@admin.dev`: 401.

- [ ] **Step 6: Commit**

```powershell
$env:GIT_AUTHOR_DATE='<data>'; $env:GIT_COMMITTER_DATE='<data>'; git add auth-api; git commit -m 'Removido o papel de admin, ficando so cliente e prestador'
```

---

### Task 2: auth-api: endereços e /me

**Files:**
- Create: `auth-api/src/main/resources/db/migration/V6__CreateTableTbAddress.sql`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/entities/AddressEntity.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/dtos/AddressRequest.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/repositories/AddressRepository.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/components/AddressRules.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/services/AddressService.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/servicesimpl/AddressServiceJpa.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/controllers/WsAuthController.java`
- Create: `auth-api/src/main/java/br/edu/atitus/authapi/controllers/InternalAddressController.java`
- Modify: `auth-api/src/main/java/br/edu/atitus/authapi/repositories/UserRepository.java` (`JpaRepository<UserEntity, Long>`; hoje está `Integer`, mas o id é `Long`)
- Test: `auth-api/src/test/java/br/edu/atitus/authapi/components/AddressRulesTest.java`

**Interfaces:**
- Produces: tabela `tb_address (id SERIAL PK, user_id BIGINT NOT NULL REFERENCES tb_user(id), label VARCHAR(50), zip_code VARCHAR(8) NOT NULL, street VARCHAR(255) NOT NULL, number VARCHAR(20) NOT NULL, complement VARCHAR(255), district VARCHAR(255) NOT NULL, city VARCHAR(255) NOT NULL, state CHAR(2) NOT NULL)`.
- Produces: `record AddressRequest(String label, String zipCode, String street, String number, String complement, String district, String city, String state)`.
- Produces: `AddressEntity` com `userId` marcado `@JsonIgnore`; o JSON de saída tem `id, label, zipCode, street, number, complement, district, city, state`.
- Produces: `AddressRules.normalize(AddressRequest r) -> AddressRequest` (CEP só com dígitos, UF maiúscula, textos com `trim`) e `AddressRules.validate(AddressRequest r)`, que lança `ResponseStatusException(BAD_REQUEST)`. `AddressRules.requireOwner(AddressEntity a, Long userId)` lança `NOT_FOUND "Endereço não encontrado"` se `a == null` ou o dono for outro.
- Produces: `AddressService { List<AddressEntity> listMine(Long userId); AddressEntity create(Long userId, AddressRequest r); AddressEntity update(Long userId, Long id, AddressRequest r); void delete(Long userId, Long id); AddressEntity getForUser(Long id, Long userId); }`
- Produces (HTTP):
  - `GET /ws/auth/me` → `{id, name, email, type}`
  - `GET|POST /ws/auth/addresses`, `PUT|DELETE /ws/auth/addresses/{id}` (POST 201, DELETE 204)
  - `GET /internal/addresses/{id}?userId=` → o `AddressEntity` ou 404 (usado na fase do order-api)

- [ ] **Step 1: Escrever o teste que falha**

```java
class AddressRulesTest {
    private static AddressRequest valido() {
        return new AddressRequest("Casa", "99010-000", " Rua A ", "10", null, "Centro", "Passo Fundo", "rs");
    }

    @Test
    void normalizaCepEUf() {
        AddressRequest r = AddressRules.normalize(valido());
        assertEquals("99010000", r.zipCode());
        assertEquals("RS", r.state());
        assertEquals("Rua A", r.street());
    }

    @Test
    void aceitaEnderecoValido() {
        assertDoesNotThrow(() -> AddressRules.validate(AddressRules.normalize(valido())));
    }

    @Test
    void recusaCepComTamanhoErrado() {
        var r = AddressRules.normalize(new AddressRequest("Casa", "9901", "Rua A", "10", null, "Centro", "Passo Fundo", "RS"));
        var e = assertThrows(ResponseStatusException.class, () -> AddressRules.validate(r));
        assertEquals(400, e.getStatusCode().value());
    }

    @Test
    void recusaUfInvalida() {
        var r = AddressRules.normalize(new AddressRequest("Casa", "99010000", "Rua A", "10", null, "Centro", "Passo Fundo", "RGS"));
        assertThrows(ResponseStatusException.class, () -> AddressRules.validate(r));
    }

    @Test
    void recusaCamposObrigatoriosVazios() {
        var r = AddressRules.normalize(new AddressRequest("Casa", "99010000", " ", "10", null, "Centro", "Passo Fundo", "RS"));
        assertThrows(ResponseStatusException.class, () -> AddressRules.validate(r));
    }

    @Test
    void requireOwnerRecusaOutroUsuario() {
        AddressEntity a = new AddressEntity();
        a.setUserId(7L);
        var e = assertThrows(ResponseStatusException.class, () -> AddressRules.requireOwner(a, 8L));
        assertEquals(404, e.getStatusCode().value());
        assertDoesNotThrow(() -> AddressRules.requireOwner(a, 7L));
        assertThrows(ResponseStatusException.class, () -> AddressRules.requireOwner(null, 7L));
    }
}
```

Obrigatórios: `zipCode` (8 dígitos), `street`, `number`, `district`, `city`, `state` (2 letras). `label` e `complement` são opcionais.

- [ ] **Step 2: Rodar e ver falhar**

Run: `.\mvnw.cmd test -Dtest=AddressRulesTest`
Expected: erro de compilação.

- [ ] **Step 3: Implementar** migração, entidade, repositório (`findByUserIdOrderByIdAsc`, `findById`), regras, serviço e os dois controllers. Os controllers seguem o padrão do agenda-api: `@RequestHeader("X-User-Id") Long userId` e um `@ExceptionHandler(ResponseStatusException.class)` devolvendo `status + reason`. O `/me` busca o usuário pelo id e devolve só `id, name, email, type` (code).

- [ ] **Step 4: Rodar os testes**

Run: `.\mvnw.cmd test`
Expected: todos passam.

- [ ] **Step 5: Verificar pelo gateway** com o token de um cliente (`POST /auth/signup` e depois `/auth/signin`)

- `POST /ws/auth/addresses` com `{"label":"Casa","zipCode":"99010-000","street":"Rua A","number":"10","district":"Centro","city":"Passo Fundo","state":"rs"}`: 201, com `zipCode` `99010000` e `state` `RS`.
- `GET /ws/auth/addresses`: lista com o endereço.
- Com o token de outro usuário, `PUT /ws/auth/addresses/{id}`: 404.
- `GET /ws/auth/me`: dados sem senha.
- `GET http://localhost:8080/internal/addresses/1?userId=1`: 404 (o gateway não tem rota para `/internal`).

- [ ] **Step 6: Commit** com a mensagem `Adicionado cadastro de enderecos e rota /me no auth-api`.

---

### Task 3: agenda-api: rota interna do pet shop por prestador

**Files:**
- Create: `agenda-api/src/main/java/br/edu/atitus/agendaapi/controllers/InternalEstablishmentController.java`

**Interfaces:**
- Consumes: `EstablishmentRepository.findByProviderId(Long)` (já existe).
- Produces: `GET /internal/establishments/by-provider/{providerId}` → `EstablishmentEntity` (`id, name, address, phone`) ou 404 `"Estabelecimento não encontrado"`.

- [ ] **Step 1: Implementar** o controller com `@RequestMapping("/internal/establishments")` e o mesmo `@ExceptionHandler` do `AgendaController`.

- [ ] **Step 2: Verificar** com agenda-api rodando local:
  - `GET http://localhost:8400/internal/establishments/by-provider/<id do provider>`: 200 depois do `PUT /ws/agenda/establishment` do README.
  - Com um id sem pet shop: 404.

- [ ] **Step 3: Commit** com a mensagem `Adicionada rota interna para buscar o estabelecimento do prestador`.

---

### Task 4: product-api: produto do prestador (modelo e migração)

**Files:**
- Create: `product-api/src/main/resources/db/migration/V3__ProductOfProvider.sql`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/entities/ProductEntity.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/dtos/ProductRequest.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/dtos/ProductResponse.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/components/ProductRules.java`
- Delete: `product-api/src/main/java/br/edu/atitus/productapi/servicesimpl/ProductServiceMock.java` (mock com celulares; a interface vai mudar)
- Test: `product-api/src/test/java/br/edu/atitus/productapi/components/ProductRulesTest.java`

**Interfaces:**
- Produces: a migração V3

```sql
DELETE FROM tb_product;
ALTER TABLE tb_product
    ADD COLUMN name VARCHAR(255) NOT NULL,
    ADD COLUMN category VARCHAR(100),
    ADD COLUMN stock INTEGER NOT NULL DEFAULT 0 CHECK (stock >= 0),
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN provider_id BIGINT NOT NULL,
    ADD COLUMN establishment_id BIGINT NOT NULL;
```

- Produces: `ProductEntity` com os campos novos (`name, category, stock, active, providerId, establishmentId`); `providerId` com `@JsonIgnore`.
- Produces: `record ProductRequest(String name, String brand, String model, String description, String category, String currency, double price, int stock, String image)`.
- Produces: `ProductResponse` com `id, name, brand, model, description, category, currency, price, stock, image, establishmentId, environment, promotionMessage, targetCurrency, convertedPrice`. O `fromEntity` atual troca `description` e `brand` de lugar; corrigir junto.
- Produces: `ProductRules.validate(ProductRequest r)`, que lança `BAD_REQUEST`: `name`, `description` e `currency` obrigatórios; `price > 0`; `stock >= 0`. `brand` e `model` passam a ser opcionais.
- Produces: `ProductRules.requireOwner(ProductEntity p, Long providerId)`, que lança `NOT_FOUND "Produto não encontrado"` se `p == null` ou o dono for outro.

- [ ] **Step 1: Escrever o teste que falha**

```java
class ProductRulesTest {
    private static ProductRequest valido() {
        return new ProductRequest("Ração Golden 15kg", "Golden", null, "Ração para cães adultos", "racao", "BRL", 189.9, 10, null);
    }

    @Test
    void aceitaProdutoValido() {
        assertDoesNotThrow(() -> ProductRules.validate(valido()));
    }

    @Test
    void recusaPrecoZero() {
        var r = new ProductRequest("Ração", null, null, "desc", null, "BRL", 0, 1, null);
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> ProductRules.validate(r)).getStatusCode().value());
    }

    @Test
    void recusaEstoqueNegativo() {
        var r = new ProductRequest("Ração", null, null, "desc", null, "BRL", 10, -1, null);
        assertThrows(ResponseStatusException.class, () -> ProductRules.validate(r));
    }

    @Test
    void recusaNomeVazio() {
        var r = new ProductRequest(" ", null, null, "desc", null, "BRL", 10, 1, null);
        assertThrows(ResponseStatusException.class, () -> ProductRules.validate(r));
    }

    @Test
    void requireOwnerRecusaOutroPrestador() {
        ProductEntity p = new ProductEntity();
        p.setProviderId(2L);
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> ProductRules.requireOwner(p, 3L)).getStatusCode().value());
        assertDoesNotThrow(() -> ProductRules.requireOwner(p, 2L));
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `cd product-api; .\mvnw.cmd test -Dtest=ProductRulesTest`
Expected: erro de compilação.

- [ ] **Step 3: Implementar** migração, entidade, DTOs e `ProductRules`; apagar o `ProductServiceMock`. Ajustar o `ProductServiceJpa.save` para compilar com o novo `ProductRequest` (a assinatura final vem na Task 6).

- [ ] **Step 4: Rodar e ver passar**

Run: `.\mvnw.cmd test -Dtest=ProductRulesTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0`

- [ ] **Step 5: Commit** com a mensagem `Produto passa a ter nome, categoria, estoque e dono no product-api`.

---

### Task 5: product-api: reserva e devolução de estoque

**Files:**
- Create: `product-api/src/main/java/br/edu/atitus/productapi/dtos/StockItemRequest.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/dtos/ReservedItemResponse.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/components/StockRules.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/services/StockService.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/servicesimpl/StockServiceJpa.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/controllers/InternalProductController.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/repositories/ProductRepository.java`
- Test: `product-api/src/test/java/br/edu/atitus/productapi/components/StockRulesTest.java`

**Interfaces:**
- Produces: `record StockItemRequest(Long productId, int quantity)`.
- Produces: `record ReservedItemResponse(Long productId, String name, int quantity, double price, String currency, Long establishmentId, Long providerId)`.
- Produces: `StockRules.merge(List<StockItemRequest> items) -> Map<Long, Integer>`: soma repetidos, mantém a ordem e lança `BAD_REQUEST` se a lista for vazia ou alguma quantidade for `<= 0`.
- Produces: `StockRules.reserve(Map<Long, Integer> wanted, Map<Long, ProductEntity> products) -> List<ReservedItemResponse>`: valida tudo antes de alterar qualquer coisa e só então baixa `stock` nas entidades. Erros:
  - produto ausente ou `active = false`: `NOT_FOUND "Produto <id> não encontrado"`
  - lojas diferentes: `CONFLICT "Todos os produtos devem ser da mesma loja"`
  - estoque insuficiente: `CONFLICT "Estoque insuficiente para o produto <id>"`
- Produces: `StockRules.release(Map<Long, Integer> wanted, Map<Long, ProductEntity> products)`: soma ao estoque e ignora produto ausente.
- Produces: `ProductRepository.findAllByIdIn(Collection<Long> ids)` com `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
- Produces (HTTP, consumido pelo order-api na fase 3):
  - `POST /internal/products/stock/reserve` corpo `List<StockItemRequest>` → `List<ReservedItemResponse>`
  - `POST /internal/products/stock/release` corpo `List<StockItemRequest>` → 204

- [ ] **Step 1: Escrever o teste que falha**

```java
class StockRulesTest {
    private static ProductEntity produto(long id, long loja, int estoque, boolean ativo) {
        ProductEntity p = new ProductEntity();
        p.setId(id); p.setName("P" + id); p.setPrice(10); p.setCurrency("BRL");
        p.setEstablishmentId(loja); p.setProviderId(loja + 100); p.setStock(estoque); p.setActive(ativo);
        return p;
    }

    @Test
    void reserveBaixaEstoque() {
        var produtos = Map.of(1L, produto(1, 1, 5, true), 2L, produto(2, 1, 3, true));
        var itens = StockRules.reserve(StockRules.merge(List.of(new StockItemRequest(1L, 2), new StockItemRequest(2L, 3))), produtos);
        assertEquals(3, produtos.get(1L).getStock());
        assertEquals(0, produtos.get(2L).getStock());
        assertEquals(2, itens.size());
        assertEquals(1L, itens.get(0).establishmentId());
    }

    @Test
    void reserveSomaProdutoRepetido() {
        var produtos = Map.of(1L, produto(1, 1, 4, true));
        var wanted = StockRules.merge(List.of(new StockItemRequest(1L, 2), new StockItemRequest(1L, 3)));
        assertEquals(5, wanted.get(1L));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> StockRules.reserve(wanted, produtos)).getStatusCode().value());
        assertEquals(4, produtos.get(1L).getStock());
    }

    @Test
    void reserveRecusaLojasDiferentes() {
        var produtos = Map.of(1L, produto(1, 1, 5, true), 2L, produto(2, 2, 5, true));
        var wanted = StockRules.merge(List.of(new StockItemRequest(1L, 1), new StockItemRequest(2L, 1)));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> StockRules.reserve(wanted, produtos)).getStatusCode().value());
        assertEquals(5, produtos.get(1L).getStock());
        assertEquals(5, produtos.get(2L).getStock());
    }

    @Test
    void reserveRecusaProdutoInativoOuAusente() {
        var produtos = Map.of(1L, produto(1, 1, 5, false));
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> StockRules.reserve(StockRules.merge(List.of(new StockItemRequest(1L, 1))), produtos)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> StockRules.reserve(StockRules.merge(List.of(new StockItemRequest(9L, 1))), produtos)).getStatusCode().value());
    }

    @Test
    void mergeRecusaQuantidadeInvalidaOuListaVazia() {
        assertThrows(ResponseStatusException.class, () -> StockRules.merge(List.of(new StockItemRequest(1L, 0))));
        assertThrows(ResponseStatusException.class, () -> StockRules.merge(List.of()));
    }

    @Test
    void releaseDevolveEstoque() {
        var produtos = Map.of(1L, produto(1, 1, 1, true));
        StockRules.release(Map.of(1L, 4), produtos);
        assertEquals(5, produtos.get(1L).getStock());
    }
}
```

- [ ] **Step 2: Rodar e ver falhar**

Run: `.\mvnw.cmd test -Dtest=StockRulesTest`
Expected: erro de compilação.

- [ ] **Step 3: Implementar** `StockRules`, os DTOs, o `StockServiceJpa`, o repositório e o `InternalProductController`. O `StockServiceJpa` é `@Transactional`: carrega com `findAllByIdIn`, chama as regras e salva. O controller tem um `@ExceptionHandler(ResponseStatusException.class)` igual ao do `WsProductController`.

- [ ] **Step 4: Rodar e ver passar**

Run: `.\mvnw.cmd test`
Expected: todos passam.

- [ ] **Step 5: Commit** com a mensagem `Adicionadas rotas internas de reserva e devolucao de estoque no product-api`.

---

### Task 6: product-api: rotas do prestador, busca pública e cliente da agenda

**Files:**
- Create: `product-api/src/main/java/br/edu/atitus/productapi/clients/AgendaClient.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/dtos/EstablishmentResponse.java`
- Create: `product-api/src/main/java/br/edu/atitus/productapi/configs/AgendaClientConfig.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/servicesimpl/AuthorizationService.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/services/ProductService.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/servicesimpl/ProductServiceJpa.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/repositories/ProductRepository.java` (`extends JpaSpecificationExecutor<ProductEntity>`)
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/controllers/WsProductController.java`
- Modify: `product-api/src/main/java/br/edu/atitus/productapi/controllers/ProductController.java`
- Modify: `README.md` (tipos de usuário, rotas de endereço e de produto, exemplo de cadastro de produto com o prestador)

**Interfaces:**
- Consumes: `GET /internal/establishments/by-provider/{providerId}` (Task 3); `ProductRules` (Task 4).
- Produces: `AgendaClient` com `@GetExchange("/internal/establishments/by-provider/{providerId}") EstablishmentResponse getByProvider(@PathVariable Long providerId)` e `record EstablishmentResponse(Long id, String name)`.
  - `AgendaClientConfig` reaproveita o `RestClient.Builder` `@LoadBalanced` que já existe no `CurrencyClientConfig`, com `baseUrl("http://agenda-api")` e os mesmos timeouts.
  - `HttpClientErrorException.NotFound` vira `BAD_REQUEST "Cadastre seu pet shop antes de cadastrar produtos"`.
- Produces: `AuthorizationService.requireProvider(Integer userType)` (tipo 2, senão `FORBIDDEN`), substituindo `requireAdmin`.
- Produces: `ProductService` com:
  - `ProductResponse findById(Long id, String targetCurrency)`
  - `Page<ProductResponse> search(Long establishmentId, String search, String category, Pageable pageable, String targetCurrency)`
  - `ProductEntity create(Long providerId, ProductRequest r)`
  - `ProductEntity update(Long providerId, Long id, ProductRequest r)`
  - `void deactivate(Long providerId, Long id)`
  - `Page<ProductEntity> findMine(Long providerId, Pageable pageable)`
- Produces (HTTP):
  - `GET /products?targetCurrency=BRL&establishmentId=&search=&category=`: só ativos, ordenado por `name`, página de 10.
  - `GET /products/{id}`: produto inativo também dá 404.
  - `POST /ws/products` (201), `PUT /ws/products/{id}`, `DELETE /ws/products/{id}` (204, desativa) e `GET /ws/products/mine`, todos com `requireProvider`.

- [ ] **Step 1: Implementar** o cliente, o `requireProvider`, o serviço e os controllers. A busca usa `Specification` para os filtros opcionais: com `@Query` e parâmetro nulo, o Postgres reclama do tipo do parâmetro. O `search` compara `lower(name) like %search%`.

- [ ] **Step 2: Rodar os testes**

Run: `.\mvnw.cmd test`
Expected: todos passam.

- [ ] **Step 3: Verificar pelo gateway** (auth, agenda, product, currency e gateway rodando)
  - Prestador sem pet shop, `POST /ws/products`: 400 "Cadastre seu pet shop antes de cadastrar produtos".
  - Prestador com pet shop, `POST /ws/products` com `{"name":"Ração Golden 15kg","brand":"Golden","description":"Ração para cães adultos","category":"racao","currency":"BRL","price":189.9,"stock":10}`: 201 com `establishmentId`.
  - Cliente no mesmo POST: 403.
  - `GET /products?targetCurrency=BRL&search=golden`: encontra. Com `establishmentId` de outra loja: vazio.
  - Outro prestador, `PUT /ws/products/{id}`: 404.
  - `DELETE /ws/products/{id}`: 204. Depois disso, `GET /products/{id}`: 404.

- [ ] **Step 4: Commit** com a mensagem `Cadastro de produtos pelo prestador, busca publica e README atualizado`.

---

## Verificação final da fase

- [ ] `.\mvnw.cmd test` passa em auth-api, agenda-api e product-api.
- [ ] Os passos de verificação manual das Tasks 1, 2, 3 e 6 passam em sequência, com o ambiente subindo do zero (bancos novos), para garantir que as migrações V5, V6 (auth) e V3 (product) rodam limpas.
