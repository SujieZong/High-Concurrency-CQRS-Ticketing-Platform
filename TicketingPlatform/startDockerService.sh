#!/usr/bin/env bash
set -euo pipefail

# 容器名称
REDIS_C="dev-redis"
RABBIT_C="dev-rabbitmq"
DYNAMO_C="dev-dynamodb"

docker rm -f $REDIS_C $RABBIT_C $DYNAMO_C >/dev/null 2>&1 || true

echo "Start Redis…"
docker run -d --name $REDIS_C \
  -p 6379:6379 \
  --restart unless-stopped \
  redis:7-alpine

echo "Start RabbitMQ…"
docker run -d --name $RABBIT_C \
  -p 5672:5672 \
  -p 15672:15672 \
  --restart unless-stopped \
  rabbitmq:3-management

echo "Start DynamoDB Local…"
docker run -d --name $DYNAMO_C \
  -p 8000:8000 \
  --restart unless-stopped \
  amazon/dynamodb-local

# 等待 DynamoDB Local 就绪
echo -n " DynamoDB Local prepare on localhost:8000 …"
until curl -s http://localhost:8000/shell >/dev/null 2>&1; do
  echo -n "."
  sleep 1
done
echo " OK"

echo "DynamoDB Local create table Tickets"
aws dynamodb create-table \
  --table-name Tickets \
  --attribute-definitions AttributeName=ticketId,AttributeType=S \
  --key-schema AttributeName=ticketId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000 \
  --region us-west-2 \
|| echo "table exists skipping"

echo "All service started"
