#!/usr/bin/env bash
set -e

echo "Stopping old dev containers…"
docker rm -f dev-redis dev-rabbitmq dev-dynamodb \
           ticketing-platform rabbit-consumer 2>/dev/null || true

# 在 redoTicketImage.sh 中
if docker image inspect "${IMAGE_NAME}:${TAG}" > /dev/null 2>&1; then
  echo "Delete old ${IMAGE_NAME}:${TAG}"
  docker rmi -f "${IMAGE_NAME}:${TAG}"
fi

echo "Starting all services via Docker Compose…"
docker compose up --build -d

echo "Waiting for DynamoDB Local to be ready…"
# 给 DynamoDB Local 启动一点时间
sleep 3

echo "Creating DynamoDB table 'Tickets' (if not exists)…"
aws dynamodb list-tables \
  --endpoint-url http://localhost:8000 \
  --region us-west-2 \
| grep -q '"Tickets"' || \
aws dynamodb create-table \
  --table-name Tickets \
  --attribute-definitions AttributeName=ticketId,AttributeType=S \
  --key-schema AttributeName=ticketId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000 \
  --region us-west-2

echo "All containers are up!"
echo " • Redis            → localhost:6379"
echo " • RabbitMQ         → localhost:5672 (UI:15672)"
echo " • DynamoDB Local   → localhost:8000 (table 'Tickets')"
echo " • API Service      → localhost:8080"
echo " • Consumer Logs    → docker logs -f rabbit-consumer"
