# Petshop marketplace: produtos do prestador, carrinho, pagamento e observabilidade

Data: 2026-11-22

## Intenção

Backend de um app de petshop (front em React Native, feito depois) sobre a arquitetura de microservices que já existe. Dois tipos de usuário:

- **Cliente**: agenda serviços e compra produtos de um pet shop, informando endereço de entrega e pagando.
- **Prestador**: mantém o pet shop (já existe na agenda-api), os serviços, os horários e agora também os produtos que vende; acompanha os pedidos e agendamentos recebidos.

O pagamento é simulado: um microservice novo que aprova qualquer pagamento. No fim, todos os microservices recebem observabilidade conforme o roteiro da disciplina (Zipkin, Prometheus, Loki/Alloy, Grafana).

## Metas

1. Remover o papel de administrador: ficam só Cliente e Prestador.
2. Produtos pertencem a um prestador e ao pet shop dele, com estoque.
3. Endereços de entrega do usuário.
4. Carrinho no servidor, com produtos de **uma loja só** (estilo iFood).
5. Checkout gera um pedido com o endereço copiado, e o pedido é pago pelo payment-api.
6. Prestador vê os pedidos recebidos e avança o status da entrega.
7. Agendamento também pode ser pago pelo payment-api.
8. Observabilidade em todos os microservices, incluindo agenda-api e payment-api.

## Fora do escopo

- Pagamento real, dados de cartão (nada de número de cartão é recebido ou gravado).
- Cálculo de frete, cupons, avaliações, notificações, upload de imagem (a imagem continua sendo uma URL).
- Carrinho com várias lojas.
- Front-end.

## Anti-metas (seria falha mesmo funcionando)

- Fugir da estrutura dos serviços existentes (`controllers / dtos / entities / repositories / services / servicesimpl / clients / configs`, Flyway, Consul, Feign, headers `X-User-*`).
- Um cliente conseguir ver ou mexer em carrinho, pedido, endereço ou pagamento de outro.
- Rotas internas entre serviços acessíveis pelo gateway.

## Restrições

- Java 25, Spring Boot 4, Spring Cloud (Gateway, Consul, OpenFeign, Resilience4j), PostgreSQL com um banco por serviço, Flyway.
- O gateway valida o JWT nas rotas `/ws/**` e repassa `X-User-Id`, `X-User-Type`, `X-User-Email`. Autorização fica em cada serviço.
- Configurações no Consul via `docker/config/load-config.sh` e `start_consul.bat/.sh`.
- Tudo roda também via `docker/docker-compose.yml`.

## Abordagens consideradas

Onde guardar o carrinho:

| Abordagem | Prós | Contras |
|---|---|---|
| **A. Carrinho dentro do order-api** (escolhida) | Carrinho sobrevive à troca de aparelho; regra de uma loja garantida pelo back; carrinho e pedido na mesma transação | order-api fica um pouco maior |
| B. Novo cart-api | Mais um serviço "puro" | Checkout vira transação entre serviços (falha no meio), complexidade sem ganho real |
| C. Carrinho só no front | Menos código no back | Carrinho some ao trocar de aparelho; regra de uma loja fica no front |

Modelo de carrinho: uma loja por carrinho (iFood) foi escolhido em vez de carrinho multi-loja com um pedido por loja (Shopee), porque pet shop é negócio local com entrega própria e o modelo deixa pedido, pagamento e "pedidos recebidos" 1 para 1.

O pedido escolheu também tirar o papel de admin.

## Desenho

### Visão geral

```
App ──> gateway-api (8080) ──┬─ auth-api (8200)      usuários, endereços
                             ├─ product-api (8000)   produtos do prestador, estoque
                             ├─ currency-api (8100)
                             ├─ order-api (8300)     carrinho, checkout, pedidos
                             ├─ agenda-api (8400)    pet shop, serviços, agendamentos
                             └─ payment-api (8700)   NOVO, pagamento simulado

Chamadas internas (Feign + Consul, nunca pelo gateway):
  product-api ─> agenda-api   descobrir o pet shop do prestador
  order-api   ─> product-api  dados do produto, reservar/devolver estoque
  order-api   ─> auth-api     endereço do cliente
  order-api   ─> payment-api  pagar pedido
  agenda-api  ─> payment-api  pagar agendamento
```

**Rotas internas** ficam sob `/internal/**` em cada serviço. O gateway não tem rota para `/internal/**`, então elas não ficam expostas pelo ponto de entrada. (Limitação conhecida: no docker-compose algumas portas dos serviços estão publicadas no host; aceitável para o ambiente didático.) A identidade do usuário nessas chamadas vai como parâmetro explícito (`userId`), não por header.

Porta 8700 para o payment-api: 8500 é a HTTP do Consul e 8600 é a DNS do Consul.

### 1. auth-api: só Cliente e Prestador, e endereços

**Tipos de usuário.** Hoje o tipo é gravado e enviado no JWT pelo `ordinal()` do enum (0 Admin, 1 Common, 2 Provider). Para não mudar os números que agenda-api e o front usam, os códigos ficam fixos:

- `UserType` passa a ter só `Common(1)` e `Provider(2)`, com um campo `code`.
- Um `AttributeConverter<UserType, Integer>` grava o código no banco; `JwtUtil` coloca `type.getCode()` no token.
- Migração `V5__RemoveAdmin.sql`: apaga o usuário `admin@admin.dev` e troca a constraint para `CHECK (type IN (1, 2))`.

No README, "Common" aparece como **Cliente** e "Provider" como **Prestador**.

**Endereços.** Tabela `tb_address`: `id, user_id, label` (ex.: "Casa"), `zip_code, street, number, complement, district, city, state` (UF com 2 letras). Um usuário pode ter vários.

Rotas (já cobertas pela rota `/ws/auth/**` do gateway):

- `GET /ws/auth/me`: dados do usuário logado (sem senha).
- `GET /ws/auth/addresses`, `POST /ws/auth/addresses`, `PUT /ws/auth/addresses/{id}`, `DELETE /ws/auth/addresses/{id}`: só os próprios; endereço de outro usuário responde 404.
- Interna: `GET /internal/addresses/{id}?userId=` para o order-api.

### 2. product-api: produto do prestador, com estoque

Migração `V3`:

- apaga os produtos de exemplo atuais (celulares, não fazem sentido no petshop);
- adiciona `name, category, stock (>= 0), active (default true), provider_id, establishment_id`.

Os campos atuais (`brand, model, description, currency, price, image`) continuam.

`AuthorizationService.requireAdmin` vira `requireProvider` (tipo 2), igual à agenda-api.

Rotas do prestador:

- `POST /ws/products`: o produto recebe `providerId` do token e `establishmentId` buscado na agenda-api (`GET /internal/establishments/by-provider/{providerId}`, rota nova na agenda-api). Se o prestador ainda não cadastrou o pet shop: 400.
- `PUT /ws/products/{id}`, `DELETE /ws/products/{id}` (desativa, `active = false`), `GET /ws/products/mine`: só produtos do próprio prestador, senão 404.

Rotas públicas:

- `GET /products?targetCurrency=BRL&establishmentId=&search=&category=` paginado, só produtos ativos.
- `GET /products/{id}?targetCurrency=BRL` (já existe).

Rotas internas para o order-api:

- `POST /internal/products/stock/reserve`: corpo `[{productId, quantity}]`. Em uma transação, valida que todos existem, estão ativos, têm estoque e são da mesma loja, e baixa o estoque. Se algum falhar, nada é baixado e responde 409 com o produto que faltou.
- `POST /internal/products/stock/release`: mesmo corpo, devolve o estoque (cancelamento).

### 3. payment-api (novo)

Novo projeto com a mesma estrutura dos outros, Dockerfile, banco `db_payment`, porta 8700, registrado no Consul e no docker-compose.

Tabela `tb_payment`: `id, reference_type` (`ORDER` ou `APPOINTMENT`), `reference_id, user_id, method` (`PIX`, `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO`), `amount, currency` (`BRL`), `status` (sempre `APPROVED`), `transaction_code` (UUID), `created_at`.

Rotas:

- Interna: `POST /internal/payments` com `{referenceType, referenceId, userId, method, amount}`. Sempre aprova e devolve o pagamento criado. Se já existir pagamento aprovado para a mesma referência, devolve o existente (idempotente, evita cobrar duas vezes num retry).
- Protegida: `GET /ws/payments`, histórico do usuário logado, paginado. Nova rota `payment-api-ws` no gateway.

### 4. order-api: carrinho, checkout e pedidos

**Carrinho.** Tabelas `tb_cart` (`id, customer_id` único, `establishment_id, updated_at`) e `tb_cart_item` (`id, cart_id, product_id, quantity > 0`, único por `cart_id + product_id`).

- `GET /ws/orders/cart?targetCurrency=BRL`: itens com dados atuais do produto (product-api), subtotal e total.
- `POST /ws/orders/cart/items` `{productId, quantity}`: se o carrinho já tem itens de outra loja, responde **409**. Com `?replace=true`, esvazia o carrinho e adiciona (o "esvaziar carrinho?" do app). Produto repetido soma a quantidade.
- `PATCH /ws/orders/cart/items/{productId}` `{quantity}`: `0` remove o item.
- `DELETE /ws/orders/cart/items/{productId}`, `DELETE /ws/orders/cart`.

**Pedido.** `tb_order` ganha:

- `establishment_id, provider_id, status, total_amount` (BRL, gravado no checkout);
- o endereço copiado: `delivery_zip_code, delivery_street, delivery_number, delivery_complement, delivery_district, delivery_city, delivery_state`;
- `payment_id, paid_at`.

O endereço é copiado de propósito: se o cliente editar ou apagar o endereço depois, o pedido não muda.

Status: `PENDING_PAYMENT -> PAID -> SHIPPED -> DELIVERED`, e `CANCELED` a partir de `PENDING_PAYMENT`.

Rotas do cliente:

- `POST /ws/orders/checkout` `{addressId}`: carrinho vazio, 400. Busca o endereço no auth-api (não encontrado: 404). Reserva o estoque no product-api (falta: 409). Cria o pedido `PENDING_PAYMENT` com preços em BRL da hora da compra, esvazia o carrinho e responde 201 com o pedido.
- `POST /ws/orders/{id}/pay` `{method}`: só pedido próprio em `PENDING_PAYMENT`, senão 409. Chama o payment-api, grava `payment_id` e `paid_at`, status `PAID`.
- `PATCH /ws/orders/{id}/cancel`: só em `PENDING_PAYMENT`; devolve o estoque.
- `GET /ws/orders?targetCurrency=BRL` (já existe) e `GET /ws/orders/{id}`.

Se o checkout falhar depois de reservar o estoque (ex.: erro ao gravar o pedido), o order-api chama o `stock/release` para devolver (compensação).

Carrinho, checkout, pagamento e cancelamento são só para Cliente (tipo 1); Prestador recebe 403.

O `POST /ws/orders` atual (pedido direto, sem carrinho) é removido: o checkout substitui.

Rotas do prestador:

- `GET /ws/orders/received?status=`: pedidos com `provider_id` = usuário logado, paginado.
- `PATCH /ws/orders/{id}/status` `{status}`: só o dono da loja, só `PAID -> SHIPPED -> DELIVERED`; outra transição, 409.

As chamadas Feign novas (auth-api, product-api, payment-api) seguem o padrão já usado com o currency-api: timeout, retry e circuit breaker. Se o payment-api estiver fora, o pedido continua `PENDING_PAYMENT` e a resposta é 503.

### 5. agenda-api: pagar e concluir agendamento

- Rota interna nova: `GET /internal/establishments/by-provider/{providerId}` (usada pelo product-api).
- `tb_appointment` ganha `payment_id, paid_at`.
- `POST /ws/agenda/appointments/{id}/pay` `{method}`: só o cliente dono, agendamento ativo e ainda não pago; chama o payment-api com `referenceType = APPOINTMENT`.
- Prestador: `PATCH /ws/agenda/appointments/{id}/complete` marca como concluído (novo valor em `AppointmentStatus`). Cancelar agendamento pago não estorna (fora do escopo).

O pagamento do agendamento é opcional: o agendamento continua válido sem ele (pagar no local).

### 6. Ajustes para o front

- CORS liberado no gateway (React Native em desenvolvimento roda via Expo/web).
- Erro padronizado em todos os serviços com um `@ControllerAdvice`: `{ "status": 409, "error": "...", "message": "...", "timestamp": "..." }`.
- Listagens paginadas no mesmo formato `Page` que o order-api já usa.

### 7. Observabilidade (roteiro da disciplina)

Feita depois das partes 1 a 5, para não instrumentar serviço que ainda vai mudar. Vale para os **7 microservices** (gateway, auth, product, currency, order, agenda, payment):

1. `micrometer-registry-prometheus` e `spring-boot-starter-zipkin` em cada `pom.xml`; configurações de métricas e tracing em `config/application` no `load-config.sh`.
2. Zipkin no docker-compose; testar um trace passando por gateway, order, product e payment.
3. Prometheus com `docker/prometheus/prometheus.yml` listando os 7 serviços.
4. Loki e Grafana Alloy para centralizar os logs dos containers, com o Trace ID nos logs (Correlation ID).
5. Grafana com as fontes Prometheus, Loki e Zipkin.
6. Dashboard com requisições, erros, latência e JVM por serviço.

### 8. Fechamento

- Testes unitários das regras novas, no estilo do `AgendaRulesTest`: carrinho de uma loja, transições de status do pedido, reserva de estoque.
- README atualizado: serviços, rotas, tipos de usuário, exemplos.
- Coleção do Postman/Insomnia com o fluxo completo.

## Ordem de implementação

1. auth-api (tipos e endereços)
2. agenda-api: rota interna by-provider
3. product-api
4. payment-api
5. order-api
6. agenda-api: pagamento e conclusão
7. Ajustes para o front (CORS, erros)
8. Observabilidade
9. Testes, README e coleção

Cada item é testável sozinho pelo gateway antes de seguir.

## Decisões tomadas por mim (para revisar)

- Códigos de tipo fixos (1 Cliente, 2 Prestador) com conversor, em vez de renumerar.
- Rotas internas em `/internal/**`, sem rota no gateway.
- Estoque baixado no checkout e devolvido no cancelamento.
- Total do pedido gravado em BRL; produto continua podendo ter outra moeda, convertida no checkout.
- Porta 8700 e banco `db_payment` para o payment-api.
- Remoção dos produtos de exemplo (celulares) e do `POST /ws/orders` direto.
- Pagamento idempotente por referência.
- Só Cliente compra; Prestador não usa carrinho.
- Compensação devolve o estoque se o checkout falhar no meio.

## Fica a critério de quem implementar

- Nomes exatos de classes, DTOs e métodos, seguindo o padrão do projeto.
- Dados de exemplo (seed) de pet shop e produtos para o prestador de teste.
- Tamanho padrão das páginas e textos das mensagens de erro.
