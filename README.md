# microservices-java

Backend do Projeto Integrador feito com Java 25 e Spring Boot 4, usando microservices, Consul, API Gateway e autenticação com JWT.

## Serviços

- gateway-api (porta 8080): ponto de entrada, faz o roteamento e valida o JWT nas rotas protegidas
- auth-api (porta 8200): cadastro, login e geração do token. Banco db_user
- product-api (porta 8000): produtos. Banco db_product
- currency-api (porta 8100): conversão de moedas usando a API do Banco Central. Banco db_currency

O Consul (porta 8500) é usado para o registro dos serviços e para as configurações.

## Autenticação e autorização

As rotas que começam com /ws/ são protegidas. O AuthFilter do gateway exige o header `Authorization: Bearer <token>`, valida o JWT e repassa os dados do usuário para os serviços pelos headers X-User-Id, X-User-Type e X-User-Email. Sem token ou com token inválido retorna 401.

A autorização fica em cada microservice. No product-api, só administrador (type 0) pode cadastrar produto, senão retorna 403.

Rotas públicas:

- POST /auth/signup
- POST /auth/signin
- GET /products?targetCurrency=BRL
- GET /products/{id}?targetCurrency=BRL
- GET /currencies?source=USD&target=BRL

Rotas protegidas:

- POST /ws/products (somente admin)

## Como rodar

Precisa ter Java 25, PostgreSQL e Consul instalados.

1. Criar os bancos no PostgreSQL:

```sql
CREATE DATABASE db_user;
CREATE DATABASE db_product;
CREATE DATABASE db_currency;
```

O usuário e a senha do banco estão como postgres/postgres no start_consul.bat e no start_consul.sh, se for diferente é só alterar lá.

2. Rodar o `start_consul.bat` (Windows) ou o `start_consul.sh` (Mac/Linux).

3. Subir os serviços: auth-api, currency-api, product-api e gateway-api (pela IDE ou com `./mvnw spring-boot:run` dentro de cada pasta).

Na primeira execução do auth-api é criado o usuário admin@admin.dev com a senha teste.

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
