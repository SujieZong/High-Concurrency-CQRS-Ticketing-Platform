#!/usr/bin/env bash
set -euo pipefail

echo "Stopping old dev containers…"
docker rm -f dev-redis dev-rabbitmq dev-dynamodb \
           ticketing-platform rabbit-consumer 2>/dev/null || true

echo "Starting all services via Docker Compose…"
docker compose build --no-cache
docker compose up --build -d

echo "Waiting for DynamoDB Local to be ready…"
sleep 5

echo "Creating DynamoDB table 'Tickets' (if not exists)…"
aws dynamodb list-tables \
  --endpoint-url http://localhost:8000 \
  --region us-west-2 2>/dev/null \
| grep -q '"Tickets"' || \
aws dynamodb create-table \
  --table-name Tickets \
  --attribute-definitions AttributeName=ticketId,AttributeType=S \
  --key-schema AttributeName=ticketId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000 \
  --region us-west-2

echo "Creating DynamoDB table 'OutboxEvent' (if not exists)…"
aws dynamodb list-tables \
  --endpoint-url http://localhost:8000 \
  --region us-west-2 2>/dev/null \
| grep -q '"OutboxEvent"' || \
aws dynamodb create-table \
  --table-name OutboxEvent \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000 \
  --region us-west-2


echo "Verifying table creation…"
aws dynamodb describe-table \
  --table-name Tickets \
  --endpoint-url http://localhost:8000 \
  --region us-west-2

echo "All containers are up!"