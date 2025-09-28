#!/usr/bin/env bash
set -euo pipefail

#common variables
DynamoDB_ENDPOINT="http://localhost:8000"
AWS_REGION="us-west-2"
TABLE_TICKETS="Tickets"
TABLE_OUTBOX="OutboxEvent"
GSI_NAME="gsi_sent_createdAt"

ROOT="$(cd "$(dirname "$0")" && pwd)"
KAFKA_SCRIPT="$ROOT/scripts/kafka.sh"

# setup for cluster ID
if [[ -z "${CLUSTER_ID:-}" ]]; then
  CLUSTER_ID="$(
    docker run --rm bitnami/kafka:latest \
    /bin/bash -lc '/opt/bitnami/kafka/bin/kafka-storage.sh random-uuid' | tr -d '\r\n'
  )"
  export CLUSTER_ID
  echo "CLUSTER_ID=$CLUSTER_ID"
fi
# make cluster id persistent since Kraft
if [[ ! -f "$ROOT/.env" ]] || ! grep -q '^CLUSTER_ID=' "$ROOT/.env"; then
  echo "CLUSTER_ID=$CLUSTER_ID" >> "$ROOT/.env"
fi

echo "Stopping old dev containers…"
# Note: dev-rabbitmq removed from cleanup as RabbitMQ is deprecated in favor of Kafka
docker rm -f dev-redis dev-dynamodb \
           ticketing-platform rabbit-consumer query-service purchase-service 2>/dev/null || true
           # Removed: dev-rabbitmq

echo "Packaging all modules with Maven…"
mvn clean package -DskipTests

echo "Starting all services via Docker Compose…"
docker compose up --build -d

echo "Waiting for DynamoDB Local to be ready…"
sleep 5

echo "Starting Kafka…"
bash "$KAFKA_SCRIPT" topics ticket.exchange 3 3 # setpartition = 3

echo "Kafka status:"
bash "$KAFKA_SCRIPT" ps

echo "Ensuring DynamoDB table '${TABLE_TICKETS}' exists…"
aws dynamodb list-tables \
  --endpoint-url "${DynamoDB_ENDPOINT}"\
  --region "${AWS_REGION}" 2>/dev/null \
| grep -q "\"${TABLE_TICKETS}\"" || \
aws dynamodb create-table \
  --table-name "${TABLE_TICKETS}" \
  --attribute-definitions AttributeName=ticketId,AttributeType=S \
  --key-schema AttributeName=ticketId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url "${DynamoDB_ENDPOINT}" \
  --region "${AWS_REGION}"

# ==== OutboxEvent ====
echo "Creating DynamoDB table '${TABLE_OUTBOX}' exists…"
aws dynamodb list-tables --endpoint-url "${DynamoDB_ENDPOINT}" --region "${AWS_REGION}" 2>/dev/null \
| grep -q "\"${TABLE_OUTBOX}\"" || \
aws dynamodb create-table \
  --table-name "${TABLE_OUTBOX}" \
  --attribute-definitions \
      AttributeName=id,AttributeType=S \
      AttributeName=sent,AttributeType=N \
      AttributeName=createdAt,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --global-secondary-indexes "[
    {
      \"IndexName\": \"${GSI_NAME}\",
      \"KeySchema\": [
        {\"AttributeName\": \"sent\", \"KeyType\": \"HASH\"},
        {\"AttributeName\": \"createdAt\", \"KeyType\": \"RANGE\"}
      ],
      \"Projection\": {\"ProjectionType\": \"ALL\"}
    }
  ]" \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url "${DynamoDB_ENDPOINT}" \
  --region "${AWS_REGION}"

echo "Verifying table creation…"
aws dynamodb describe-table \
  --table-name "${TABLE_TICKETS}" \
  --endpoint-url http://localhost:8000 \
  --region us-west-2 >/dev/null

aws dynamodb describe-table \
  --table-name OutboxEvent \
  --endpoint-url http://localhost:8000 \
  --region us-west-2 >/dev/null

echo "All containers are up!"