# microservices-java

Backend do Projeto Integrador de um pet shop, feito com Java 25 e Spring Boot 4, usando microservices, Consul, API Gateway, autenticação com JWT, pagamento simulado e observabilidade.

## Serviços

| Serviço | Porta | Função | Banco |
| --- | --- | --- | --- |
| gateway-api | 8080 | Ponto de entrada: roteamento, CORS e validação do JWT nas rotas protegidas | |
| auth-api | 8200 | Cadastro, login, geração do token e endereços do usuário | db_user |
| product-api | 8000 | Produtos dos pet shops e controle de estoque | db_product |
| currency-api | 8100 | Conversão de moedas usando a API do Banco Central | db_currency |
| order-api | 8300 | Carrinho, checkout e pedidos | db_order |
| agenda-api | 8400 | Pet shops, serviços, horários de atendimento e agendamentos | db_agenda |
| payment-api | 8700 | Pagamento simulado (sempre aprovado) e histórico de pagamentos | db_payment |

O Consul (porta 8500) é usado para o registro dos serviços e para as configurações. Os serviços se comunicam pelo nome registrado no Consul (`lb://nome-do-servico`).

## Tipos de usuário

- 1 = Cliente: carrinho, checkout, pagamento e cancelamento de pedido, endereços, pets e agendamentos.
- 2 = Prestador: dono de um pet shop. Cadastra produtos, serviços e horários, acompanha e envia os pedidos da loja e conclui agendamentos.

Cada rota exige o tipo certo, senão retorna 403. Não existe usuário administrador. Na primeira execução do auth-api é criado o prestador `provider@provider.dev` com a senha `teste`. Outros prestadores se cadastram em `/auth/signup/provider` e clientes em `/auth/signup`.

## Autenticação e autorização

Todas as requisições passam pelo gateway (http://localhost:8080). As rotas que começam com `/ws/` são protegidas: o AuthFilter do gateway exige o header `Authorization: Bearer <token>`, valida o JWT e repassa os dados do usuário aos serviços pelos headers X-User-Id, X-User-Type e X-User-Email (os mesmos headers enviados pelo cliente são descartados). Sem token ou com token inválido retorna 401. O gateway também responde o preflight de CORS (OPTIONS) e libera qualquer origem.

A autorização por tipo e por dono do recurso fica em cada microservice. Recurso de outro usuário retorna 404 (a exceção é cancelar o agendamento de outro usuário, que responde 403).

As rotas `/internal/**` são só para a comunicação entre os serviços e não existem no gateway, então não são acessíveis de fora.

## Formato de erro

Todos os serviços devolvem os erros no mesmo formato, com a mensagem em português:

```json
{ "status": 409, "error": "Conflict", "message": "Horário já ocupado", "timestamp": "2026-11-16T10:00:00" }
```

Códigos usados: 400 (dados inválidos), 401 (sem token ou token inválido), 403 (tipo de usuário sem permissão), 404 (não encontrado ou de outro usuário), 409 (conflito de regra), 503 (serviço dependente fora do ar), 500 (erro interno).

## Rotas

### Públicas

| Método e rota | Descrição |
| --- | --- |
| POST /auth/signup | Cadastra cliente. Corpo: name, email, password. Retorna 201 |
| POST /auth/signup/provider | Cadastra prestador. Mesmo corpo. Retorna 201 |
| POST /auth/signin | Corpo: email, password. Retorna `{ "user": { id, name, email, type }, "token": "..." }`, com type 1 (cliente) ou 2 (prestador) |
| GET /products?targetCurrency=BRL | Busca de produtos ativos, ordenados por nome, 10 por página. Filtros opcionais: establishmentId, search (nome) e category |
| GET /products/{id}?targetCurrency=BRL | Produto com o preço convertido |
| GET /products/categories | Categorias dos produtos ativos, para o filtro da busca |
| GET /currencies?source=USD&target=BRL | Cotação entre duas moedas |
| GET /agenda/establishments | Pet shops, paginado |
| GET /agenda/establishments/{id} | Dados de um pet shop |
| GET /agenda/establishments/{id}/services | Serviços do pet shop |
| GET /agenda/establishments/{id}/slots?serviceId=1&date=2026-11-16 | Horários livres do dia para o serviço |

### Protegidas (Bearer token)

Auth (auth-api):

| Método e rota | Quem | Descrição |
| --- | --- | --- |
| GET /ws/auth/me | qualquer | Dados do usuário do token |
| PUT /ws/auth/me | qualquer | Altera o nome. Corpo: name |
| PUT /ws/auth/me/password | qualquer | Troca a senha. Corpo: currentPassword, newPassword. Senha atual errada: 400 |
| GET /ws/auth/addresses | qualquer | Lista os endereços |
| POST /ws/auth/addresses | qualquer | Cria endereço. Corpo: label, zipCode, street, number, complement, district, city, state |
| PUT /ws/auth/addresses/{id} | qualquer | Altera o endereço |
| DELETE /ws/auth/addresses/{id} | qualquer | Remove o endereço |

Produtos (product-api):

| Método e rota | Quem | Descrição |
| --- | --- | --- |
| POST /ws/products | prestador | Cadastra produto no pet shop dele. Corpo: name, brand, model, description, category, currency, price, stock, image. Sem pet shop cadastrado: 400 |
| PUT /ws/products/{id} | prestador | Altera produto próprio (de outro prestador: 404) |
| DELETE /ws/products/{id} | prestador | Desativa o produto, que some da busca |
| GET /ws/products/mine | prestador | Produtos do prestador, paginado |

Carrinho e pedidos (order-api):

| Método e rota | Quem | Descrição |
| --- | --- | --- |
| GET /ws/orders/cart?targetCurrency=BRL | cliente | Carrinho com preços convertidos |
| POST /ws/orders/cart/items?replace=false | cliente | Adiciona item. Corpo: productId, quantity. Item de outra loja: 409, ou troca o carrinho com `replace=true` |
| PATCH /ws/orders/cart/items/{productId} | cliente | Altera a quantidade. Corpo: quantity |
| DELETE /ws/orders/cart/items/{productId} | cliente | Remove o item |
| DELETE /ws/orders/cart | cliente | Esvazia o carrinho |
| POST /ws/orders/checkout | cliente | Cria o pedido a partir do carrinho. Corpo: addressId. Retorna 201 |
| GET /ws/orders?targetCurrency=BRL | cliente | Pedidos do cliente, paginado, mais recentes primeiro |
| GET /ws/orders/{id}?targetCurrency=BRL | cliente | Detalhe do pedido |
| POST /ws/orders/{id}/pay | cliente | Paga o pedido. Corpo: method |
| PATCH /ws/orders/{id}/cancel | cliente | Cancela pedido aguardando pagamento |
| GET /ws/orders/received?status=PAID | prestador | Pedidos da loja dele, paginado, filtro opcional por status |
| GET /ws/orders/received/{id} | prestador | Detalhe de um pedido da loja, com o e-mail do cliente |
| PATCH /ws/orders/{id}/status | prestador | Avança o pedido. Corpo: status (SHIPPED ou DELIVERED) |

Agenda (agenda-api):

| Método e rota | Quem | Descrição |
| --- | --- | --- |
| PUT /ws/agenda/establishment | prestador | Cria ou altera o pet shop. Corpo: name, address, phone |
| GET /ws/agenda/establishment | prestador | Pet shop do prestador |
| POST /ws/agenda/services | prestador | Cadastra serviço. Corpo: name, durationMinutes, price |
| PUT /ws/agenda/services/{id} | prestador | Altera o serviço (agendamentos já feitos mantêm o preço da reserva) |
| DELETE /ws/agenda/services/{id} | prestador | Remove o serviço |
| PUT /ws/agenda/availability | prestador | Substitui a grade de atendimento. Corpo: lista de dayOfWeek, startTime, endTime |
| GET /ws/agenda/availability | prestador | Grade de atendimento |
| GET /ws/agenda/appointments/received | prestador | Agendamentos do pet shop, paginado |
| PATCH /ws/agenda/appointments/{id}/complete | prestador | Conclui o agendamento |
| GET /ws/agenda/pets | cliente | Pets do cliente |
| GET /ws/agenda/pets/{id} | cliente | Detalhe do pet |
| POST /ws/agenda/pets | cliente | Cadastra pet. Corpo: name, species, breed, birthDate, weight, notes. Obrigatórios: name e species |
| PUT /ws/agenda/pets/{id} | cliente | Altera o pet |
| DELETE /ws/agenda/pets/{id} | cliente | Remove o pet (os agendamentos antigos continuam com os dados dele) |
| POST /ws/agenda/appointments | cliente | Agenda um horário. Corpo: establishmentId, serviceId, startAt, petId |
| GET /ws/agenda/appointments | cliente | Agendamentos do cliente, paginado |
| GET /ws/agenda/appointments/{id} | cliente ou prestador do pet shop | Detalhe do agendamento |
| PATCH /ws/agenda/appointments/{id}/cancel | cliente ou prestador do pet shop | Cancela o agendamento |
| POST /ws/agenda/appointments/{id}/pay | cliente | Paga o agendamento. Corpo: method |

Pagamentos (payment-api):

| Método e rota | Quem | Descrição |
| --- | --- | --- |
| GET /ws/payments | qualquer | Pagamentos do usuário do token, paginado |

### Internas (só entre os serviços)

| Rota | Serviço | Uso |
| --- | --- | --- |
| GET /internal/addresses/{id}?userId= | auth-api | O order-api busca o endereço de entrega no checkout |
| POST /internal/products/stock/reserve | product-api | O order-api reserva o estoque no checkout |
| POST /internal/products/stock/release | product-api | O order-api devolve o estoque ao cancelar |
| GET /internal/establishments/by-provider/{providerId} | agenda-api | O product-api e o order-api descobrem o pet shop do prestador |
| POST /internal/payments | payment-api | O order-api e a agenda-api registram o pagamento |

## Fluxo de compra

1. O prestador cadastra o pet shop (`PUT /ws/agenda/establishment`) e o produto (`POST /ws/products`).
2. O cliente cadastra um endereço (`POST /ws/auth/addresses`) e adiciona produtos ao carrinho (`POST /ws/orders/cart/items`). O carrinho é de uma loja só: item de outra loja retorna 409. Se um produto sair do ar, o item aparece como indisponível no carrinho.
3. `POST /ws/orders/checkout` com o addressId cria o pedido como `PENDING_PAYMENT`, reserva o estoque, grava os dados do produto e do endereço no pedido e o total em BRL, e esvazia o carrinho.
4. `POST /ws/orders/{id}/pay` com `{ "method": "PIX" }` chama o payment-api e o pedido vira `PAID`. Métodos aceitos: PIX, CREDIT_CARD, DEBIT_CARD e BOLETO. O pagamento é sempre aprovado e é idempotente por referência. Pagar de novo retorna 409. Se o payment-api estiver fora, retorna 503 e o pedido continua pendente.
5. O prestador acompanha em `GET /ws/orders/received` e avança com `PATCH /ws/orders/{id}/status`: `PAID` para `SHIPPED` e depois `DELIVERED`. Qualquer outra transição retorna 409.
6. O cliente só cancela (`PATCH /ws/orders/{id}/cancel`) com o pedido em `PENDING_PAYMENT`, senão 409. O cancelamento devolve o estoque.

Status do pedido: `PENDING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELED`. Os pedidos usam os dados copiados no checkout, então continuam consultáveis mesmo se o produto for desativado. O order-api não faz retry nas chamadas de estoque, só timeout e circuit breaker.

## Fluxo de agendamento

1. O prestador cadastra o pet shop, um serviço (`POST /ws/agenda/services`) e a grade de atendimento (`PUT /ws/agenda/availability`). O dayOfWeek vai de 1 (segunda) a 7 (domingo).
2. O cliente cadastra o pet (`POST /ws/agenda/pets`). Espécies: CACHORRO, GATO, PASSARO, ROEDOR, PEIXE, REPTIL e OUTRO (aceita minúscula e acento).
3. O cliente consulta os horários livres em `GET /agenda/establishments/{id}/slots?serviceId=1&date=2026-11-16`.
4. `POST /ws/agenda/appointments` com o petId cria o agendamento com status `AGENDADO` e copia o nome e a espécie do pet. Pet de outro cliente: 404. Horário já ocupado: 409. Fora do horário de atendimento: 400.
5. `POST /ws/agenda/appointments/{id}/pay` com `{ "method": "PIX" }` registra o pagamento no payment-api. Agendamento cancelado ou já pago: 409.
6. O prestador conclui com `PATCH /ws/agenda/appointments/{id}/complete` (status `CONCLUIDO`). Agendamento que não está `AGENDADO`: 409.
7. O cancelamento (`CANCELADO`) pode ser feito pelo cliente ou pelo prestador do pet shop. Agendamento já concluído não pode ser cancelado (409).

## Como rodar

### Local

Precisa ter Java 25, PostgreSQL e Consul instalados.

1. Criar os bancos no PostgreSQL:

```sql
CREATE DATABASE db_user;
CREATE DATABASE db_product;
CREATE DATABASE db_currency;
CREATE DATABASE db_order;
CREATE DATABASE db_agenda;
CREATE DATABASE db_payment;
```

2. Definir as variáveis de ambiente com o usuário e a senha do PostgreSQL e a chave do JWT (mínimo de 32 caracteres). Nada disso fica no código, os scripts do Consul leem essas variáveis e param se alguma estiver faltando.

Windows (PowerShell):

```
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="sua-senha"
$env:JWT_SECRET="uma-chave-com-pelo-menos-32-caracteres"
```

Mac/Linux:

```
export DB_USERNAME=postgres DB_PASSWORD=sua-senha JWT_SECRET=uma-chave-com-pelo-menos-32-caracteres
```

3. No mesmo terminal, rodar o `start_consul.bat` (Windows) ou o `start_consul.sh` (Mac/Linux).

4. Subir os serviços: auth-api, currency-api, product-api, order-api, agenda-api, payment-api e gateway-api (pela IDE ou com `./mvnw spring-boot:run` dentro de cada pasta).

### Com Docker

Dentro da pasta `docker`, copiar o `.env.example` para `.env` e preencher os valores (senha dos bancos, chave do JWT com pelo menos 32 caracteres e senha do Grafana). O `.env` não vai para o git:

```
cp .env.example .env
docker compose up --build -d
```

Sobem o Consul, os bancos PostgreSQL, os 7 serviços e a stack de observabilidade. Se faltar alguma variável no `.env`, o compose avisa qual é e não sobe.

As credenciais dos bancos e a chave do JWT chegam aos containers por variável de ambiente (`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` e `APP_JWT_SECRET`). O restante das configurações fica no Consul.

Só o gateway (http://localhost:8080) é publicado para fora. Os microservices não têm porta aberta no host e só são acessados pela rede interna do Docker, então as rotas `/internal/**` e os headers X-User-* não podem ser chamados direto por quem está fora.

## Observabilidade

Os 7 serviços exportam métricas em `/actuator/prometheus` e enviam traces ao Zipkin; as configurações ficam no Consul em `config/application`.

| Ferramenta | URL | Função |
| --- | --- | --- |
| Zipkin | http://localhost:9411 | Distributed tracing |
| Prometheus | http://localhost:9090 | Métricas (coleta a cada 15s) |
| Grafana | http://localhost:3000 | Dashboards (usuário e senha definidos no `.env`) |
| Loki | http://localhost:3100 | Logs centralizados (`/ready`, `/loki/api/v1/labels`) |
| Grafana Alloy | http://localhost:12345/graph | Coleta os logs dos containers e envia ao Loki |

No Grafana as fontes de dados (Prometheus, Loki e Zipkin) e o dashboard "Microservices Overview" (disponibilidade, requisições por segundo, taxa de erro 5xx, latência p95 e memória heap da JVM) já vêm provisionados. Em Explore dá para consultar os logs com `{job="docker"}` ou `{container_name=~".*order-api.*"}`.

Rodando sem Docker, o `start_consul.bat`/`start_consul.sh` apontam o tracing para `http://localhost:9411/api/v2/spans`; se não houver Zipkin local os serviços funcionam normalmente, só sem os traces.

## Testes

Em cada módulo: `./mvnw test`.

Para testar a API na mão, importe no Postman a coleção `docs/postman/petshop.postman_collection.json`. Ela usa o gateway (variável `baseUrl`, padrão http://localhost:8080), cadastra um cliente e um prestador novos a cada execução e, rodando as requisições de cima para baixo, percorre o fluxo completo: perfil e senha, pet shop, serviço, horários, produto, endereço, pets, carrinho, checkout, pagamento, envio e entrega do pedido, e agendamento, pagamento e conclusão. Os tokens e os ids são guardados nas variáveis da coleção pelos scripts de teste.

Resumo do que esperar:

- sem token nas rotas `/ws/`: 401
- cliente nas rotas de prestador, ou prestador nas rotas de cliente: 403
- recurso de outro usuário: 404 (cancelar agendamento de outro usuário: 403)
- item de outra loja no carrinho, pagar duas vezes, transição de status inválida, horário já ocupado: 409
