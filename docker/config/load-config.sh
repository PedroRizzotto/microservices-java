#!/bin/sh

set -eu

echo "=========================================="
echo " Carregando configurações no Consul"
echo "=========================================="

# Aguarda o Consul responder à API HTTP.
until consul members >/dev/null 2>&1; do
    echo "Aguardando o Consul..."
    sleep 2
done

# Função auxiliar para cadastrar uma configuração.
put() {
    consul kv put "$1" "$2"
}

# ==========================================================
# CONFIGURAÇÕES GERAIS - TODAS AS APLICAÇÕES
# ==========================================================

put "config/application/management.endpoints.web.exposure.include" "*"
put "config/application/management.endpoint.health.show-components" "always"
put "config/application/management.endpoint.health.show-details" "always"
put "config/application/management.health.livenessstate.enabled" "true"
put "config/application/management.health.readinessstate.enabled" "true"
put "config/application/management.health.db.enabled" "true"

# ==========================================================
# OBSERVABILIDADE - MÉTRICAS E TRACING
# ==========================================================

# Métricas no formato do Prometheus
put "config/application/management.prometheus.metrics.export.enabled" "true"
# Histograma das requisições HTTP (usado no p95 do dashboard)
put "config/application/management.metrics.distribution.percentiles-histogram.http.server.requests" "true"

# Tracing
put "config/application/management.tracing.enabled" "true"
put "config/application/management.tracing.sampling.probability" "1.0"

# Exportação dos traces para o Zipkin
put "config/application/management.tracing.export.zipkin.endpoint" "http://zipkin:9411/api/v2/spans"


# ==========================================================
# PRODUCT API
# ==========================================================

put "config/product-api/server.port" "8000"

# Banco de dados
put "config/product-api/spring.datasource.url" \
    "jdbc:postgresql://product-db:5432/db_product"
put "config/product-api/spring.datasource.username" "postgres"
put "config/product-api/spring.datasource.password" "postgres"

# Mensagem de promoção
put "config/product-api/app.promotion.message" \
    "Promoção Semana do Cliente"

# Mensagem em inglês
put "config/product-api,en/app.promotion.message" \
    "Christmas Promotion!!!!"

# Retry - Currency API
put "config/product-api/resilience4j.retry.instances.currency-api.max-attempts" "3"
put "config/product-api/resilience4j.retry.instances.currency-api.wait-duration" "1s"

# Circuit Breaker - Currency API
put "config/product-api/resilience4j.circuitbreaker.instances.currency-api.sliding-window-type" "COUNT_BASED"
put "config/product-api/resilience4j.circuitbreaker.instances.currency-api.sliding-window-size" "5"
put "config/product-api/resilience4j.circuitbreaker.instances.currency-api.minimum-number-of-calls" "5"
put "config/product-api/resilience4j.circuitbreaker.instances.currency-api.failure-rate-threshold" "50"
put "config/product-api/resilience4j.circuitbreaker.instances.currency-api.wait-duration-in-open-state" "100s"

# Exceção ignorada pelo Circuit Breaker
put "config/product-api/resilience4j.circuitbreaker.instances.currency-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException"

# Cache - Currency API
put "config/product-api/spring.cache.type" "caffeine"
put "config/product-api/spring.cache.cache-names" "currency-api"
put "config/product-api/spring.cache.caffeine.spec" "maximumSize=1000,expireAfterWrite=10m"

# Ordem dos aspectos
put "config/product-api/resilience4j.retry.retry-aspect-order" "100"
put "config/product-api/resilience4j.circuitbreaker.circuit-breaker-aspect-order" "101"


# ==========================================================
# CURRENCY API
# ==========================================================

put "config/currency-api/server.port" "8100"

# Banco de dados
put "config/currency-api/spring.datasource.url" \
    "jdbc:postgresql://currency-db:5432/db_currency"
put "config/currency-api/spring.datasource.username" "postgres"
put "config/currency-api/spring.datasource.password" "postgres"

# Retry - BCB
put "config/currency-api/resilience4j.retry.instances.bcb-api.max-attempts" "3"
put "config/currency-api/resilience4j.retry.instances.bcb-api.wait-duration" "1s"

# Circuit Breaker - BCB
put "config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.sliding-window-type" "COUNT_BASED"
put "config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.sliding-window-size" "5"
put "config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.minimum-number-of-calls" "5"
put "config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.failure-rate-threshold" "50"
put "config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.wait-duration-in-open-state" "100s"

# Exceção ignorada pelo Circuit Breaker
put "config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException"

# Cache - BCB
put "config/currency-api/spring.cache.type" "caffeine"
put "config/currency-api/spring.cache.cache-names" "bcb-api"
put "config/currency-api/spring.cache.caffeine.spec" "maximumSize=1000,expireAfterWrite=10m"

# Ordem dos aspectos
put "config/currency-api/resilience4j.retry.retry-aspect-order" "100"
put "config/currency-api/resilience4j.circuitbreaker.circuit-breaker-aspect-order" "101"


# ==========================================================
# AUTH API
# ==========================================================

put "config/auth-api/server.port" "8200"

# Banco de dados
put "config/auth-api/spring.datasource.url" "jdbc:postgresql://auth-db:5432/db_user"
put "config/auth-api/spring.datasource.username" "postgres"
put "config/auth-api/spring.datasource.password" "postgres"


# ==========================================================
# ORDER API
# ==========================================================

put "config/order-api/server.port" "8300"

# Banco de dados
put "config/order-api/spring.datasource.url" \
    "jdbc:postgresql://order-db:5432/db_order"
put "config/order-api/spring.datasource.username" "postgres"
put "config/order-api/spring.datasource.password" "postgres"

# Resilience4j - chamadas do order-api aos outros serviços (estoque sem retry)
put "config/order-api/resilience4j.retry.instances.product-api.max-attempts" "3"
put "config/order-api/resilience4j.retry.instances.product-api.wait-duration" "1s"
put "config/order-api/resilience4j.retry.instances.product-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException"
put "config/order-api/resilience4j.circuitbreaker.instances.product-api.sliding-window-type" "COUNT_BASED"
put "config/order-api/resilience4j.circuitbreaker.instances.product-api.sliding-window-size" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.product-api.minimum-number-of-calls" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.product-api.failure-rate-threshold" "50"
put "config/order-api/resilience4j.circuitbreaker.instances.product-api.wait-duration-in-open-state" "100s"
put "config/order-api/resilience4j.circuitbreaker.instances.product-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException"
put "config/order-api/resilience4j.circuitbreaker.instances.stock-api.sliding-window-type" "COUNT_BASED"
put "config/order-api/resilience4j.circuitbreaker.instances.stock-api.sliding-window-size" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.stock-api.minimum-number-of-calls" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.stock-api.failure-rate-threshold" "50"
put "config/order-api/resilience4j.circuitbreaker.instances.stock-api.wait-duration-in-open-state" "100s"
put "config/order-api/resilience4j.circuitbreaker.instances.stock-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException,org.springframework.web.server.ResponseStatusException"
put "config/order-api/resilience4j.circuitbreaker.instances.auth-api.sliding-window-type" "COUNT_BASED"
put "config/order-api/resilience4j.circuitbreaker.instances.auth-api.sliding-window-size" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.auth-api.minimum-number-of-calls" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.auth-api.failure-rate-threshold" "50"
put "config/order-api/resilience4j.circuitbreaker.instances.auth-api.wait-duration-in-open-state" "100s"
put "config/order-api/resilience4j.circuitbreaker.instances.auth-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException"
put "config/order-api/resilience4j.retry.instances.payment-api.max-attempts" "3"
put "config/order-api/resilience4j.retry.instances.payment-api.wait-duration" "1s"
put "config/order-api/resilience4j.retry.instances.payment-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException,org.springframework.web.server.ResponseStatusException"
put "config/order-api/resilience4j.circuitbreaker.instances.payment-api.sliding-window-type" "COUNT_BASED"
put "config/order-api/resilience4j.circuitbreaker.instances.payment-api.sliding-window-size" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.payment-api.minimum-number-of-calls" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.payment-api.failure-rate-threshold" "50"
put "config/order-api/resilience4j.circuitbreaker.instances.payment-api.wait-duration-in-open-state" "100s"
put "config/order-api/resilience4j.circuitbreaker.instances.payment-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException,org.springframework.web.server.ResponseStatusException"
put "config/order-api/resilience4j.retry.instances.currency-api.max-attempts" "3"
put "config/order-api/resilience4j.retry.instances.currency-api.wait-duration" "1s"
put "config/order-api/resilience4j.retry.instances.currency-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException"
put "config/order-api/resilience4j.circuitbreaker.instances.currency-api.sliding-window-type" "COUNT_BASED"
put "config/order-api/resilience4j.circuitbreaker.instances.currency-api.sliding-window-size" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.currency-api.minimum-number-of-calls" "5"
put "config/order-api/resilience4j.circuitbreaker.instances.currency-api.failure-rate-threshold" "50"
put "config/order-api/resilience4j.circuitbreaker.instances.currency-api.wait-duration-in-open-state" "100s"
put "config/order-api/resilience4j.circuitbreaker.instances.currency-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException"
put "config/order-api/resilience4j.retry.retry-aspect-order" "100"
put "config/order-api/resilience4j.circuitbreaker.circuit-breaker-aspect-order" "101"


# ==========================================================
# AGENDA API
# ==========================================================

put "config/agenda-api/server.port" "8400"

# Banco de dados
put "config/agenda-api/spring.datasource.url" \
    "jdbc:postgresql://agenda-db:5432/db_agenda"
put "config/agenda-api/spring.datasource.username" "postgres"
put "config/agenda-api/spring.datasource.password" "postgres"

put "config/agenda-api/resilience4j.retry.instances.payment-api.max-attempts" "3"
put "config/agenda-api/resilience4j.retry.instances.payment-api.wait-duration" "1s"
put "config/agenda-api/resilience4j.retry.instances.payment-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException,org.springframework.web.server.ResponseStatusException"
put "config/agenda-api/resilience4j.circuitbreaker.instances.payment-api.sliding-window-type" "COUNT_BASED"
put "config/agenda-api/resilience4j.circuitbreaker.instances.payment-api.sliding-window-size" "5"
put "config/agenda-api/resilience4j.circuitbreaker.instances.payment-api.minimum-number-of-calls" "5"
put "config/agenda-api/resilience4j.circuitbreaker.instances.payment-api.failure-rate-threshold" "50"
put "config/agenda-api/resilience4j.circuitbreaker.instances.payment-api.wait-duration-in-open-state" "100s"
put "config/agenda-api/resilience4j.circuitbreaker.instances.payment-api.ignore-exceptions" "io.github.resilience4j.circuitbreaker.CallNotPermittedException,org.springframework.web.client.HttpClientErrorException,org.springframework.web.server.ResponseStatusException"
put "config/agenda-api/resilience4j.retry.retry-aspect-order" "100"
put "config/agenda-api/resilience4j.circuitbreaker.circuit-breaker-aspect-order" "101"


# ==========================================================
# PAYMENT API
# ==========================================================

put "config/payment-api/server.port" "8700"

# Banco de dados
put "config/payment-api/spring.datasource.url"     "jdbc:postgresql://payment-db:5432/db_payment"
put "config/payment-api/spring.datasource.username" "postgres"
put "config/payment-api/spring.datasource.password" "postgres"


# ==========================================================
# FINALIZAÇÃO
# ==========================================================

echo "=========================================="
echo " Configurações carregadas com sucesso!"
echo "=========================================="