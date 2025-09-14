#!/usr/bin/env bash
set -euo pipefail

# Location of the compose file
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/../../.. && pwd)"
COMPOSE="docker compose -f \"$ROOT/docker-compose.yml\""

# Wait for health check
wait_ready() {
  echo "Waiting for Kafka to be healthy via Docker healthcheck…"
  for i in {1..60}; do  # 最长约 120s
    # 拿到容器 ID，再用 docker inspect 读健康状态
    cid="$(eval "$COMPOSE" ps -q kafka-1)"
    status="$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo "unknown")"
    if [[ "$status" == "healthy" ]]; then
      echo "Kafka (kafka-1) is healthy."
      return 0
    fi
    sleep 2
  done
  echo "Kafka did not become healthy in time." >&2
  return 1
}

case "${1:-}" in
  topics)
    topic="${2:-ticket.exchange}"
    parts="${3:-3}" #3 partiions
    rf="${4:-3}"
    wait_ready
    eval "$COMPOSE" exec -T kafka-1 /opt/bitnami/kafka/bin/kafka-topics.sh \
      --bootstrap-server kafka-1:9092 --create --if-not-exists \
      --topic "$topic" --partitions "$parts" --replication-factor "$rf"
    echo "Ensured topic: $topic (p=$parts, rf=$rf)"
    ;;

  ps)
    eval "$COMPOSE" ps
    ;;

  *)
    cat <<EOF
Usage: $0 {topics [name [partitions [rf]]]|ps|up}
  topics          Ensure topic exists (default: ticket.exchange, 3 partitions, RF=3) #partition
  ps              Show kafka containers status
  up              (Optional) Start only kafka-1/2/3 via compose
EOF
    exit 1
    ;;
esac