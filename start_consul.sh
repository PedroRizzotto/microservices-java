#!/usr/bin/env bash
# Equivalente ao start_consul.bat para macOS/Linux
set -e

echo "Iniciando Consul..."
consul agent -dev > consul.log 2>&1 &
CONSUL_PID=$!
trap 'echo; echo "Parando Consul..."; kill $CONSUL_PID' INT TERM

echo "Aguardando Consul iniciar..."
until consul members > /dev/null 2>&1; do sleep 1; done
echo "Carregando configuracoes..."

consul kv put config/product-api/server.port "8000"
consul kv put config/product-api/spring.datasource.url "jdbc:postgresql://localhost/db_product"
consul kv put config/product-api/spring.datasource.username "postgres"
consul kv put config/product-api/spring.datasource.password "postgres"

consul kv put config/product-api/app.promotion.message "Customer Week Promotion"
consul kv put config/product-api,it/app.promotion.message "Promozione Settimana del Cliente"

consul kv put config/currency-api/server.port "8100"
consul kv put config/currency-api/spring.datasource.url "jdbc:postgresql://localhost/db_currency"
consul kv put config/currency-api/spring.datasource.username "postgres"
consul kv put config/currency-api/spring.datasource.password "postgres"

echo
echo "Consul iniciado e configuracoes carregadas! UI: http://localhost:8500"
echo "Logs em consul.log. Ctrl+C para parar."
wait $CONSUL_PID
