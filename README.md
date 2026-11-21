# microservices-java

Backend do Projeto Integrador feito com Java 25 e Spring Boot 4, usando microservices, Consul, API Gateway e autenticação com JWT.

## Serviços

- gateway-api (porta 8080): ponto de entrada, faz o roteamento e valida o JWT nas rotas protegidas
- auth-api (porta 8200): cadastro, login, geração do token e endereços do usuário. Banco db_user
- product-api (porta 8000): produtos dos pet shops (cada produto pertence a um prestador e ao pet shop dele). Banco db_product
- currency-api (porta 8100): conversão de moedas usando a API do Banco Central. Banco db_currency
- order-api (porta 8300): pedidos do usuário autenticado, consulta o product-api e o currency-api pelo Consul. Banco db_order
- agenda-api (porta 8400): pet shops, serviços, horários de atendimento e agendamentos. Banco db_agenda

O Consul (porta 8500) é usado para o registro dos serviços e para as configurações.

## Autenticação e autorização

As rotas que começam com /ws/ são protegidas. O AuthFilter do gateway exige o header `Authorization: Bearer <token>`, valida o JWT e repassa os dados do usuário para os serviços pelos headers X-User-Id, X-User-Type e X-User-Email. Sem token ou com token inválido retorna 401.

Existem dois tipos de usuário: cliente (type 1) e prestador (type 2). A autorização fica em cada microservice. No product-api, só o prestador cadastra, altera e desativa produtos, senão retorna 403, e cada prestador só mexe nos próprios produtos (o produto de outro prestador retorna 404). Para cadastrar produto o prestador precisa ter o pet shop cadastrado na agenda-api, senão retorna 400. Na agenda-api, as rotas do pet shop também são só para prestador.

Rotas públicas:

- POST /auth/signup
- POST /auth/signup/provider
- POST /auth/signin
- GET /products?targetCurrency=BRL (filtros opcionais: establishmentId, search pelo nome e category; só produtos ativos, ordenados por nome, 10 por página)
- GET /products/{id}?targetCurrency=BRL
- GET /currencies?source=USD&target=BRL
- GET /agenda/establishments
- GET /agenda/establishments/{id}/services
- GET /agenda/establishments/{id}/slots?serviceId=1&date=2026-11-16

Rotas protegidas:

- GET /ws/auth/me
- GET /ws/auth/addresses, POST /ws/auth/addresses
- PUT /ws/auth/addresses/{id}, DELETE /ws/auth/addresses/{id}
- POST /ws/products (prestador)
- PUT /ws/products/{id}, DELETE /ws/products/{id} (prestador, só os próprios produtos; o DELETE desativa o produto)
- GET /ws/products/mine (prestador, paginado)
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

Na primeira execução do auth-api é criado o prestador provider@provider.dev com a senha teste.

## Testes

Todas as requisições são feitas no gateway (http://localhost:8080).

Login do prestador:

```
POST /auth/signin
{ "email": "provider@provider.dev", "password": "teste" }
```

Cadastro de produto (o prestador precisa ter o pet shop cadastrado, veja a seção da agenda abaixo):

```
POST /ws/products
{ "name": "Ração Golden 15kg", "brand": "Golden", "description": "Ração para cães adultos", "category": "racao", "currency": "BRL", "price": 189.9, "stock": 10 }
```

- sem token: 401
- com o token do prestador (Bearer Token): 201, com o establishmentId do pet shop dele
- prestador sem pet shop cadastrado: 400
- com o token de um cliente: 403

Para criar um cliente:

```
POST /auth/signup
{ "name": "Novo Cliente", "email": "user@atitus.edu.br", "password": "Teste123" }
```

Depois é só fazer o login com ele em /auth/signin e usar o token na requisição de cadastro de produto.

Busca pública de produtos e alteração pelo prestador:

```
GET /products?targetCurrency=BRL&search=golden&category=racao&establishmentId=1

PUT /ws/products/1
{ "name": "Ração Golden 15kg", "brand": "Golden", "description": "Ração para cães adultos", "category": "racao", "currency": "BRL", "price": 179.9, "stock": 8 }

DELETE /ws/products/1
```

- outro prestador no PUT ou no DELETE: 404
- depois do DELETE o produto some da busca e o GET /products/1 retorna 404

Endereços do usuário logado:

```
POST /ws/auth/addresses
{ "label": "Casa", "zipCode": "99010-000", "street": "Rua A", "number": "10", "complement": "Ap 2", "district": "Centro", "city": "Passo Fundo", "state": "RS" }

GET /ws/auth/addresses
PUT /ws/auth/addresses/1
DELETE /ws/auth/addresses/1
GET /ws/auth/me
```

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
