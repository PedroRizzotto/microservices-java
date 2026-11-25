# Endpoints da API

Todas as requisições passam pelo API Gateway em `http://localhost:8080`. Os microservices não têm porta publicada no host.

Rotas que começam com `/ws/` são protegidas: é preciso mandar o header `Authorization: Bearer <token>`, com o token retornado por `POST /auth/signin`. Sem token ou com token inválido o gateway responde 401. Tipo de usuário errado retorna 403.

Nos exemplos, `<TOKEN_CLIENTE>` é o token de um usuário cliente (tipo 1) e `<TOKEN_PRESTADOR>` o de um prestador (tipo 2); `<TOKEN>` vale para qualquer um dos dois. Os ids (`1`) são de exemplo, troque pelos ids retornados nas chamadas anteriores. Os cURLs estão no formato exportado pelo Postman (Code > cURL) e rodam em bash (Linux, Mac, Git Bash ou WSL).

A ordem das seções segue o fluxo de uso: cadastro e login, prestador monta o pet shop e os produtos, cliente compra e agenda. A coleção completa do Postman está em [postman/petshop.postman_collection.json](postman/petshop.postman_collection.json).

## Resumo

| Método | Caminho | Acesso | Serviço | Finalidade |
| --- | --- | --- | --- | --- |
| POST | `/auth/signup` | pública | auth-api | Cadastra um cliente (tipo 1). |
| POST | `/auth/signup/provider` | pública | auth-api | Cadastra um prestador, dono de pet shop (tipo 2). |
| POST | `/auth/signin` | pública | auth-api | Login. Retorna os dados do usuário e o token JWT usado nas rotas `/ws/**`. |
| GET | `/ws/auth/me` | qualquer usuário logado | auth-api | Dados do usuário dono do token. |
| PUT | `/ws/auth/me` | cliente | auth-api | Altera o nome do usuário. |
| PUT | `/ws/auth/me/password` | qualquer usuário logado | auth-api | Troca a senha. Senha atual errada retorna 400. |
| POST | `/ws/auth/addresses` | qualquer usuário logado | auth-api | Cadastra um endereço de entrega. |
| GET | `/ws/auth/addresses` | qualquer usuário logado | auth-api | Lista os endereços do usuário. |
| PUT | `/ws/auth/addresses/{addressId}` | cliente | auth-api | Altera um endereço do usuário. |
| DELETE | `/ws/auth/addresses/{addressId}` | qualquer usuário logado | auth-api | Remove um endereço. |
| PUT | `/ws/agenda/establishment` | prestador | agenda-api | Cria ou altera o pet shop do prestador. |
| GET | `/ws/agenda/establishment` | prestador | agenda-api | Retorna o pet shop do prestador. |
| POST | `/ws/agenda/services` | prestador | agenda-api | Cadastra um serviço do pet shop. |
| PUT | `/ws/agenda/services/{serviceId}` | prestador | agenda-api | Altera um serviço. Agendamentos já feitos mantêm o preço da reserva. |
| PUT | `/ws/agenda/availability` | prestador | agenda-api | Substitui a grade de atendimento. dayOfWeek vai de 1 (segunda) a 7 (domingo). |
| GET | `/ws/agenda/availability` | prestador | agenda-api | Retorna a grade de atendimento. |
| DELETE | `/ws/agenda/services/{serviceId}` | prestador | agenda-api | Remove um serviço. |
| GET | `/agenda/establishments` | pública | agenda-api | Lista os pet shops, paginado (`page`, `size`). |
| GET | `/agenda/establishments/{establishmentId}` | pública | agenda-api | Dados de um pet shop. |
| GET | `/agenda/establishments/{establishmentId}/services` | pública | agenda-api | Serviços de um pet shop. |
| GET | `/agenda/establishments/{establishmentId}/slots` | pública | agenda-api | Horários livres do dia para o serviço. |
| POST | `/ws/products` | prestador | product-api | Cadastra um produto no pet shop do prestador. |
| PUT | `/ws/products/{productId}` | prestador | product-api | Altera um produto do prestador. |
| GET | `/ws/products/mine` | prestador | product-api | Produtos do prestador, paginado. |
| GET | `/products` | pública | product-api | Busca produtos ativos com preço convertido. Filtros opcionais: `establishmentId`, `search`, `category`. |
| GET | `/products/categories` | pública | product-api | Categorias dos produtos ativos. |
| GET | `/products/{productId}` | pública | product-api | Um produto com o preço convertido para a moeda pedida (chama o currency-api). |
| DELETE | `/ws/products/{productId}` | prestador | product-api | Desativa o produto, que some da busca. |
| GET | `/currencies` | pública | currency-api | Cotação entre duas moedas (dados do Banco Central). |
| POST | `/ws/orders/cart/items` | cliente | order-api | Adiciona um produto ao carrinho. Produto de outra loja retorna 409, a não ser com `replace=true`. |
| GET | `/ws/orders/cart` | cliente | order-api | Carrinho com os preços convertidos. |
| PATCH | `/ws/orders/cart/items/{productId}` | cliente | order-api | Altera a quantidade de um item. |
| DELETE | `/ws/orders/cart/items/{productId}` | cliente | order-api | Remove um item do carrinho. |
| DELETE | `/ws/orders/cart` | cliente | order-api | Esvazia o carrinho. |
| PATCH | `/ws/orders/{orderId}/cancel` | cliente | order-api | Cancela um pedido que ainda aguarda pagamento e devolve o estoque. |
| POST | `/ws/orders/checkout` | cliente | order-api | Cria o pedido a partir do carrinho: reserva o estoque (product-api), busca o endereço (auth-api) e converte o total (currency-api). |
| POST | `/ws/orders/{orderId}/pay` | cliente | order-api | Paga o pedido (chama o payment-api). Métodos: PIX, CREDIT_CARD, DEBIT_CARD, BOLETO. |
| GET | `/ws/orders` | cliente | order-api | Pedidos do cliente, paginado. |
| GET | `/ws/orders/{orderId}` | cliente | order-api | Detalhe de um pedido do cliente. |
| GET | `/ws/orders/received` | prestador | order-api | Pedidos recebidos pela loja do prestador. Filtro opcional por status. |
| GET | `/ws/orders/received/{orderId}` | prestador | order-api | Detalhe de um pedido recebido, com o e-mail do cliente. |
| PATCH | `/ws/orders/{orderId}/status` | prestador | order-api | Avança o pedido: PAID para SHIPPED e depois DELIVERED. |
| POST | `/ws/agenda/pets` | cliente | agenda-api | Cadastra um pet. Obrigatórios: name e species. |
| GET | `/ws/agenda/pets` | cliente | agenda-api | Lista os pets do cliente. |
| GET | `/ws/agenda/pets/{petId}` | cliente | agenda-api | Detalhe de um pet. |
| PUT | `/ws/agenda/pets/{petId}` | cliente | agenda-api | Altera um pet. |
| DELETE | `/ws/agenda/pets/{petId}` | cliente | agenda-api | Remove um pet. Os agendamentos antigos mantêm os dados dele. |
| PATCH | `/ws/agenda/appointments/{appointmentId}/cancel` | cliente | agenda-api | Cancela o agendamento (cliente ou prestador do pet shop). |
| POST | `/ws/agenda/appointments` | cliente | agenda-api | Agenda um horário. Horário ocupado: 409. Fora da grade: 400. |
| GET | `/ws/agenda/appointments` | cliente | agenda-api | Agendamentos do cliente, paginado. |
| GET | `/ws/agenda/appointments/{appointmentId}` | cliente | agenda-api | Detalhe de um agendamento. |
| POST | `/ws/agenda/appointments/{appointmentId}/pay` | cliente | agenda-api | Paga o agendamento (chama o payment-api). |
| GET | `/ws/agenda/appointments/received` | prestador | agenda-api | Agendamentos do pet shop do prestador, paginado. |
| PATCH | `/ws/agenda/appointments/{appointmentId}/complete` | prestador | agenda-api | Conclui o agendamento. |
| GET | `/ws/payments` | qualquer usuário logado | payment-api | Pagamentos do usuário do token, paginado. |

## Autenticação e perfil (auth-api)

### POST /auth/signup

Cadastra um cliente (tipo 1). Acesso: pública.

```bash
curl --location 'http://localhost:8080/auth/signup' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Maria Cliente",
    "email": "cliente@email.com",
    "password": "Senha123"
}'
```

### POST /auth/signup/provider

Cadastra um prestador, dono de pet shop (tipo 2). Acesso: pública.

```bash
curl --location 'http://localhost:8080/auth/signup/provider' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Pet Feliz",
    "email": "petshop@email.com",
    "password": "Senha123"
}'
```

### POST /auth/signin

Login. Retorna os dados do usuário e o token JWT usado nas rotas `/ws/**`. Acesso: pública.

```bash
curl --location 'http://localhost:8080/auth/signin' \
--header 'Content-Type: application/json' \
--data '{
    "email": "cliente@email.com",
    "password": "Senha123"
}'
```

### GET /ws/auth/me

Dados do usuário dono do token. Acesso: qualquer usuário logado.

```bash
curl --location 'http://localhost:8080/ws/auth/me' \
--header 'Authorization: Bearer <TOKEN>'
```

### PUT /ws/auth/me

Altera o nome do usuário. Acesso: cliente.

```bash
curl --location --request PUT 'http://localhost:8080/ws/auth/me' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Maria Souza"
}'
```

### PUT /ws/auth/me/password

Troca a senha. Senha atual errada retorna 400. Acesso: qualquer usuário logado.

```bash
curl --location --request PUT 'http://localhost:8080/ws/auth/me/password' \
--header 'Authorization: Bearer <TOKEN>' \
--header 'Content-Type: application/json' \
--data '{
    "currentPassword": "Senha123",
    "newPassword": "NovaSenha456"
}'
```

## Endereços (auth-api)

### POST /ws/auth/addresses

Cadastra um endereço de entrega. Acesso: qualquer usuário logado.

```bash
curl --location 'http://localhost:8080/ws/auth/addresses' \
--header 'Authorization: Bearer <TOKEN>' \
--header 'Content-Type: application/json' \
--data '{
    "label": "Casa",
    "zipCode": "99010-000",
    "street": "Rua Independência",
    "number": "100",
    "complement": "Ap 201",
    "district": "Centro",
    "city": "Passo Fundo",
    "state": "RS"
}'
```

### GET /ws/auth/addresses

Lista os endereços do usuário. Acesso: qualquer usuário logado.

```bash
curl --location 'http://localhost:8080/ws/auth/addresses' \
--header 'Authorization: Bearer <TOKEN>'
```

### PUT /ws/auth/addresses/{addressId}

Altera um endereço do usuário. Acesso: cliente.

```bash
curl --location --request PUT 'http://localhost:8080/ws/auth/addresses/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "label": "Casa",
    "zipCode": "99010-000",
    "street": "Rua Independência",
    "number": "100",
    "complement": "Ap 202",
    "district": "Centro",
    "city": "Passo Fundo",
    "state": "RS"
}'
```

### DELETE /ws/auth/addresses/{addressId}

Remove um endereço. Acesso: qualquer usuário logado.

```bash
curl --location --request DELETE 'http://localhost:8080/ws/auth/addresses/1' \
--header 'Authorization: Bearer <TOKEN>'
```

## Pet shop, serviços e horários (agenda-api)

### PUT /ws/agenda/establishment

Cria ou altera o pet shop do prestador. Acesso: prestador.

```bash
curl --location --request PUT 'http://localhost:8080/ws/agenda/establishment' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Pet Feliz",
    "address": "Rua Independência, 500 - Passo Fundo",
    "phone": "54 99999-0000"
}'
```

### GET /ws/agenda/establishment

Retorna o pet shop do prestador. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/agenda/establishment' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

### POST /ws/agenda/services

Cadastra um serviço do pet shop. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/agenda/services' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Banho",
    "durationMinutes": 60,
    "price": 50.0
}'
```

### PUT /ws/agenda/services/{serviceId}

Altera um serviço. Agendamentos já feitos mantêm o preço da reserva. Acesso: prestador.

```bash
curl --location --request PUT 'http://localhost:8080/ws/agenda/services/1' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Banho e tosa",
    "durationMinutes": 60,
    "price": 70.0
}'
```

### PUT /ws/agenda/availability

Substitui a grade de atendimento. dayOfWeek vai de 1 (segunda) a 7 (domingo). Acesso: prestador.

```bash
curl --location --request PUT 'http://localhost:8080/ws/agenda/availability' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '[
    {
        "dayOfWeek": 1,
        "startTime": "09:00",
        "endTime": "12:00"
    },
    {
        "dayOfWeek": 2,
        "startTime": "09:00",
        "endTime": "12:00"
    },
    {
        "dayOfWeek": 3,
        "startTime": "09:00",
        "endTime": "12:00"
    },
    {
        "dayOfWeek": 4,
        "startTime": "09:00",
        "endTime": "12:00"
    },
    {
        "dayOfWeek": 5,
        "startTime": "09:00",
        "endTime": "12:00"
    }
]'
```

### GET /ws/agenda/availability

Retorna a grade de atendimento. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/agenda/availability' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

### DELETE /ws/agenda/services/{serviceId}

Remove um serviço. Acesso: prestador.

```bash
curl --location --request DELETE 'http://localhost:8080/ws/agenda/services/1' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

## Consulta de pet shops e horários (agenda-api)

### GET /agenda/establishments

Lista os pet shops, paginado (`page`, `size`). Acesso: pública.

```bash
curl --location 'http://localhost:8080/agenda/establishments'
```

### GET /agenda/establishments/{establishmentId}

Dados de um pet shop. Acesso: pública.

```bash
curl --location 'http://localhost:8080/agenda/establishments/1'
```

### GET /agenda/establishments/{establishmentId}/services

Serviços de um pet shop. Acesso: pública.

```bash
curl --location 'http://localhost:8080/agenda/establishments/1/services'
```

### GET /agenda/establishments/{establishmentId}/slots

Horários livres do dia para o serviço. Acesso: pública.

```bash
curl --location 'http://localhost:8080/agenda/establishments/1/slots?serviceId=1&date=2026-11-30'
```

## Produtos (product-api)

### POST /ws/products

Cadastra um produto no pet shop do prestador. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/products' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Ração Golden 15kg",
    "brand": "Golden",
    "model": "Adulto",
    "description": "Ração para cães adultos",
    "category": "racao",
    "currency": "BRL",
    "price": 189.9,
    "stock": 10,
    "image": "https://exemplo.com/racao.png"
}'
```

### PUT /ws/products/{productId}

Altera um produto do prestador. Acesso: prestador.

```bash
curl --location --request PUT 'http://localhost:8080/ws/products/1' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Ração Golden 15kg",
    "brand": "Golden",
    "model": "Adulto",
    "description": "Ração para cães adultos",
    "category": "racao",
    "currency": "BRL",
    "price": 179.9,
    "stock": 10,
    "image": "https://exemplo.com/racao.png"
}'
```

### GET /ws/products/mine

Produtos do prestador, paginado. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/products/mine' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

### GET /products

Busca produtos ativos com preço convertido. Filtros opcionais: `establishmentId`, `search`, `category`. Acesso: pública.

```bash
curl --location 'http://localhost:8080/products?targetCurrency=BRL&search=golden&category=racao'
```

### GET /products/categories

Categorias dos produtos ativos. Acesso: pública.

```bash
curl --location 'http://localhost:8080/products/categories'
```

### GET /products/{productId}

Um produto com o preço convertido para a moeda pedida (chama o currency-api). Acesso: pública.

```bash
curl --location 'http://localhost:8080/products/1?targetCurrency=USD'
```

### DELETE /ws/products/{productId}

Desativa o produto, que some da busca. Acesso: prestador.

```bash
curl --location --request DELETE 'http://localhost:8080/ws/products/1' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

## Cotação (currency-api)

### GET /currencies

Cotação entre duas moedas (dados do Banco Central). Acesso: pública.

```bash
curl --location 'http://localhost:8080/currencies?source=USD&target=BRL'
```

## Carrinho (order-api)

### POST /ws/orders/cart/items

Adiciona um produto ao carrinho. Produto de outra loja retorna 409, a não ser com `replace=true`. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/orders/cart/items?replace=false' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "productId": 1,
    "quantity": 2
}'
```

### GET /ws/orders/cart

Carrinho com os preços convertidos. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/orders/cart?targetCurrency=BRL' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### PATCH /ws/orders/cart/items/{productId}

Altera a quantidade de um item. Acesso: cliente.

```bash
curl --location --request PATCH 'http://localhost:8080/ws/orders/cart/items/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "quantity": 3
}'
```

### DELETE /ws/orders/cart/items/{productId}

Remove um item do carrinho. Acesso: cliente.

```bash
curl --location --request DELETE 'http://localhost:8080/ws/orders/cart/items/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### DELETE /ws/orders/cart

Esvazia o carrinho. Acesso: cliente.

```bash
curl --location --request DELETE 'http://localhost:8080/ws/orders/cart' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

## Pedidos (order-api)

### PATCH /ws/orders/{orderId}/cancel

Cancela um pedido que ainda aguarda pagamento e devolve o estoque. Acesso: cliente.

```bash
curl --location --request PATCH 'http://localhost:8080/ws/orders/1/cancel' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### POST /ws/orders/checkout

Cria o pedido a partir do carrinho: reserva o estoque (product-api), busca o endereço (auth-api) e converte o total (currency-api). Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/orders/checkout' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "addressId": 1
}'
```

### POST /ws/orders/{orderId}/pay

Paga o pedido (chama o payment-api). Métodos: PIX, CREDIT_CARD, DEBIT_CARD, BOLETO. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/orders/1/pay' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "method": "PIX"
}'
```

### GET /ws/orders

Pedidos do cliente, paginado. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/orders?targetCurrency=BRL' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### GET /ws/orders/{orderId}

Detalhe de um pedido do cliente. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/orders/1?targetCurrency=USD' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### GET /ws/orders/received

Pedidos recebidos pela loja do prestador. Filtro opcional por status. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/orders/received?status=PAID' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

### GET /ws/orders/received/{orderId}

Detalhe de um pedido recebido, com o e-mail do cliente. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/orders/received/1' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

### PATCH /ws/orders/{orderId}/status

Avança o pedido: PAID para SHIPPED e depois DELIVERED. Acesso: prestador.

```bash
curl --location --request PATCH 'http://localhost:8080/ws/orders/1/status' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>' \
--header 'Content-Type: application/json' \
--data '{
    "status": "SHIPPED"
}'
```

## Pets (agenda-api)

### POST /ws/agenda/pets

Cadastra um pet. Obrigatórios: name e species. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/pets' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Mingau",
    "species": "GATO",
    "breed": "Siamês",
    "birthDate": "2021-03-10",
    "weight": 4.2,
    "notes": "Não gosta de secador"
}'
```

### GET /ws/agenda/pets

Lista os pets do cliente. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/pets' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### GET /ws/agenda/pets/{petId}

Detalhe de um pet. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/pets/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### PUT /ws/agenda/pets/{petId}

Altera um pet. Acesso: cliente.

```bash
curl --location --request PUT 'http://localhost:8080/ws/agenda/pets/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "name": "Mingau",
    "species": "GATO",
    "breed": "Siamês",
    "birthDate": "2021-03-10",
    "weight": 4.8,
    "notes": "Não gosta de secador"
}'
```

### DELETE /ws/agenda/pets/{petId}

Remove um pet. Os agendamentos antigos mantêm os dados dele. Acesso: cliente.

```bash
curl --location --request DELETE 'http://localhost:8080/ws/agenda/pets/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

## Agendamentos (agenda-api)

### PATCH /ws/agenda/appointments/{appointmentId}/cancel

Cancela o agendamento (cliente ou prestador do pet shop). Acesso: cliente.

```bash
curl --location --request PATCH 'http://localhost:8080/ws/agenda/appointments/1/cancel' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### POST /ws/agenda/appointments

Agenda um horário. Horário ocupado: 409. Fora da grade: 400. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/appointments' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "establishmentId": 1,
    "serviceId": 1,
    "startAt": "2026-11-30T09:00:00",
    "petId": 1
}'
```

### GET /ws/agenda/appointments

Agendamentos do cliente, paginado. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/appointments' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### GET /ws/agenda/appointments/{appointmentId}

Detalhe de um agendamento. Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/appointments/1' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>'
```

### POST /ws/agenda/appointments/{appointmentId}/pay

Paga o agendamento (chama o payment-api). Acesso: cliente.

```bash
curl --location 'http://localhost:8080/ws/agenda/appointments/1/pay' \
--header 'Authorization: Bearer <TOKEN_CLIENTE>' \
--header 'Content-Type: application/json' \
--data '{
    "method": "PIX"
}'
```

### GET /ws/agenda/appointments/received

Agendamentos do pet shop do prestador, paginado. Acesso: prestador.

```bash
curl --location 'http://localhost:8080/ws/agenda/appointments/received' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

### PATCH /ws/agenda/appointments/{appointmentId}/complete

Conclui o agendamento. Acesso: prestador.

```bash
curl --location --request PATCH 'http://localhost:8080/ws/agenda/appointments/1/complete' \
--header 'Authorization: Bearer <TOKEN_PRESTADOR>'
```

## Pagamentos (payment-api)

### GET /ws/payments

Pagamentos do usuário do token, paginado. Acesso: qualquer usuário logado.

```bash
curl --location 'http://localhost:8080/ws/payments' \
--header 'Authorization: Bearer <TOKEN>'
```

## Rotas internas

Usadas só na comunicação entre os microservices, pelo nome registrado no Consul (client-side discovery). Não existem no gateway, então não têm cURL externo: de fora, `http://localhost:8080/internal/...` retorna 404.

| Método | Caminho | Serviço | Quem chama | Finalidade |
| --- | --- | --- | --- | --- |
| GET | `/internal/addresses/{id}?userId=` | auth-api | order-api | Endereço de entrega no checkout |
| POST | `/internal/products/stock/reserve` | product-api | order-api | Reserva o estoque no checkout |
| POST | `/internal/products/stock/release` | product-api | order-api | Devolve o estoque ao cancelar |
| GET | `/internal/establishments/by-provider/{providerId}` | agenda-api | product-api, order-api | Pet shop do prestador |
| POST | `/internal/payments` | payment-api | order-api, agenda-api | Registra o pagamento |
