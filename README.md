# microservices-java

Backend do Projeto Integrador feito com Java 25 e Spring Boot 4, usando microservices, Consul, API Gateway e autenticação com JWT.

## Serviços

- gateway-api (porta 8080): ponto de entrada, faz o roteamento e valida o JWT nas rotas protegidas
- auth-api (porta 8200): cadastro, login e geração do token. Banco db_user
- product-api (porta 8000): produtos. Banco db_product
- currency-api (porta 8100): conversão de moedas usando a API do Banco Central. Banco db_currency
- order-api (porta 8300): pedidos do usuário autenticado, consulta o product-api e o currency-api pelo Consul. Banco db_order
- agenda-api (porta 8400): pet shops, serviços, horários de atendimento e agendamentos. Banco db_agenda

O Consul (porta 8500) é usado para o registro dos serviços e para as configurações.

## Autenticação e autorização

As rotas que começam com /ws/ são protegidas. O AuthFilter do gateway exige o header `Authorization: Bearer <token>`, valida o JWT e repassa os dados do usuário para os serviços pelos headers X-User-Id, X-User-Type e X-User-Email. Sem token ou com token inválido retorna 401.

A autorização fica em cada microservice. No product-api, só administrador (type 0) pode cadastrar produto, senão retorna 403. Na agenda-api, as rotas do pet shop são só para prestador (type 2).

Rotas públicas:

- POST /auth/signup
- POST /auth/signup/provider
- POST /auth/signin
- GET /products?targetCurrency=BRL
- GET /products/{id}?targetCurrency=BRL
- GET /currencies?source=USD&target=BRL
- GET /agenda/establishments
- GET /agenda/establishments/{id}/services
- GET /agenda/establishments/{id}/slots?serviceId=1&date=2026-11-16

Rotas protegidas:

- POST /ws/products (somente admin)
- POST /ws/orders
- GET /ws/orders?targetCurrency=BRL (paginado, só os pedidos do usuário do token)
- PUT /ws/agenda/establishment, GET /ws/agenda/establishment (prestador)
- POST /ws/agenda/services, DELETE /ws/agenda/services/{id} (prestador)
- PUT /ws/agenda/availability, GET /ws/agenda/availability (prestador)
- GET /ws/agenda/appointments/received (prestador)
- POST /ws/agenda/appointments
- GET /ws/agenda/appointments
- PATCH /ws/agenda/appointments/{id}/cancel

## Como rodar

Precisa ter Java 25, PostgreSQL e Consul instalados.

1. Criar os bancos no PostgreSQL:

```sql
CREATE DATABASE db_user;
CREATE DATABASE db_product;
CREATE DATABASE db_currency;
CREATE DATABASE db_order;
CREATE DATABASE db_agenda;
```

O usuário e a senha do banco estão como postgres/postgres no start_consul.bat e no start_consul.sh, se for diferente é só alterar lá.

2. Rodar o `start_consul.bat` (Windows) ou o `start_consul.sh` (Mac/Linux).

3. Subir os serviços: auth-api, currency-api, product-api, order-api, agenda-api e gateway-api (pela IDE ou com `./mvnw spring-boot:run` dentro de cada pasta).

Na primeira execução do auth-api é criado o usuário admin@admin.dev com a senha teste e o prestador provider@provider.dev com a senha teste.

## Testes

Todas as requisições são feitas no gateway (http://localhost:8080).

Login do admin:

```
POST /auth/signin
{ "email": "admin@admin.dev", "password": "teste" }
```

Cadastro de produto:

```
POST /ws/products
{ "description": "POCO X8 Pro Global", "brand": "POCO", "model": "POCO X8 Pro", "currency": "USD", "price": 395.5 }
```

- sem token: 401
- com o token do admin (Bearer Token): 201
- com o token de um usuário comum: 403

Para criar um usuário comum:

```
POST /auth/signup
{ "name": "Novo Usuário NÃO ADMIN", "email": "user@atitus.edu.br", "password": "Teste123" }
```

Depois é só fazer o login com ele em /auth/signin e usar o token na requisição de cadastro de produto.

Cadastro de pedido (o cliente vem do token, não vai no corpo):

```
POST /ws/orders
{ "items": [ { "productId": 2, "quantity": 2 }, { "productId": 4, "quantity": 3 } ] }
```

Consulta dos pedidos do usuário logado, com os valores convertidos:

```
GET /ws/orders?targetCurrency=BRL
```

- sem token: 401
- com token: 201 no cadastro e 200 na consulta, cada usuário vê só os próprios pedidos

Agenda, logado com o provider@provider.dev:

```
PUT /ws/agenda/establishment
{ "name": "Pet Feliz", "address": "Rua A, 10", "phone": "54 99999-0000" }

POST /ws/agenda/services
{ "name": "Banho", "durationMinutes": 60, "price": 50.0 }

PUT /ws/agenda/availability
[ { "dayOfWeek": 1, "startTime": "09:00", "endTime": "12:00" }, { "dayOfWeek": 1, "startTime": "13:00", "endTime": "18:00" } ]
```

O dayOfWeek vai de 1 (segunda) a 7 (domingo).

Depois, logado com um usuário comum:

```
GET /agenda/establishments/1/slots?serviceId=1&date=2026-11-16

POST /ws/agenda/appointments
{ "establishmentId": 1, "serviceId": 1, "startAt": "2026-11-16T10:00:00", "petName": "Rex" }
```

- horário já ocupado: 409
- fora do horário de atendimento: 400
- usuário comum nas rotas do prestador: 403
