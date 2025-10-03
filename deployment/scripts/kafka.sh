#!/usr/bin/env bash

# Kafka management script for Docker-based Kafka cluster
# Usage: ./kafka.sh [command] [args...]

set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Set log prefix for this script
LOG_PREFIX="KAFKA"

# Load environment variables from .env file
ROOT="$(get_project_root)"
ENV_FILE="$ROOT/deployment/.env"
if [[ -f "$ENV_FILE" ]]; then
    source "$ENV_FILE"
fi

# Check for verbose mode
VERBOSE_MODE=false
if [[ "${1:-}" == "--verbose" ]]; then
    VERBOSE_MODE=true
    shift
fi

# Kafka broker configuration - read from environment variables
KAFKA_CONTAINER="${KAFKA_CONTAINER:-kafka-1}"
KAFKA_BOOTSTRAP_SERVERS="${KAFKA_BROKERS_LOCAL:-kafka-1:9092,kafka-2:9092,kafka-3:9092}"

# Function to wait for Kafka to be ready
wait_for_kafka() {
    log_info "Waiting for Kafka cluster to be ready..."
    local max_attempts=15
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if docker ps --format "{{.Names}}" | grep -q "^${KAFKA_CONTAINER}$"; then
            if kafka_cmd kafka-topics.sh --list --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" >/dev/null 2>&1; then
                log_info "Kafka cluster is ready!"
                return 0
            fi
        fi
        
        log_info "Attempt $attempt/$max_attempts: Kafka not ready yet..."
        sleep 2
        ((attempt++))
    done
    
    log_warning "Kafka cluster failed to start after $max_attempts attempts"
    return 1
}

# Function to run Kafka commands in Docker container
kafka_cmd() {
    # Check if Docker is available
    if ! command -v docker >/dev/null 2>&1; then
        log_failed "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Check if Kafka container is running
    if ! docker ps --format "{{.Names}}" | grep -q "^${KAFKA_CONTAINER}$"; then
        log_failed "Kafka container '$KAFKA_CONTAINER' is not running"
        exit 1
    fi
    
    docker exec -it "$KAFKA_CONTAINER" /opt/bitnami/kafka/bin/"$@"
}

case "${1:-help}" in
    "setup")
        # Setup Kafka topic: ./kafka.sh setup <topic-name> <partitions> <replication-factor>
        if [[ $# -eq 4 ]]; then
            TOPIC_NAME="$2"
            PARTITIONS="$3"
            REPLICATION_FACTOR="$4"
            
            log_step "=== Kafka Setup Started ==="
            
            # Wait for Kafka to be ready
            if ! wait_for_kafka; then
                exit 1
            fi
            
            # Delete existing topic first to avoid conflicts (ignore errors)
            kafka_cmd kafka-topics.sh \
                --delete \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" >/dev/null 2>&1 || true
            
            # Create topic
            log_info "Creating Kafka topic: $TOPIC_NAME with $PARTITIONS partitions and replication factor $REPLICATION_FACTOR"
            
            if kafka_cmd kafka-topics.sh \
                --create \
                --topic "$TOPIC_NAME" \
                --partitions "$PARTITIONS" \
                --replication-factor "$REPLICATION_FACTOR" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
                --if-not-exists >/dev/null 2>&1; then
                
                # Verify topic creation
                if kafka_cmd kafka-topics.sh \
                    --describe \
                    --topic "$TOPIC_NAME" \
                    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" >/dev/null 2>&1; then
                    log_success "Topic '$TOPIC_NAME' created and verified successfully"
                    
                    if [[ "$VERBOSE_MODE" == true ]]; then
                        # Show topic details in verbose mode
                        log_info "Topic details:"
                        kafka_cmd kafka-topics.sh \
                            --describe \
                            --topic "$TOPIC_NAME" \
                            --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
                    fi
                else
                    log_failed "Topic '$TOPIC_NAME' creation verification failed"
                    exit 1
                fi
            else
                log_failed "Topic '$TOPIC_NAME' creation failed"
                exit 1
            fi
            
            log_step "=== Kafka Setup Completed ==="
            log_success "Kafka topic setup completed successfully!"
        else
            log_error "Usage: ./kafka.sh setup <topic-name> <partitions> <replication-factor>"
            exit 1
        fi
        ;;
        
    "topics")
        if [[ $# -eq 4 ]]; then
            # Create topic: ./kafka.sh topics <topic-name> <partitions> <replication-factor>
            TOPIC_NAME="$2"
            PARTITIONS="$3"
            REPLICATION_FACTOR="$4"
            
            log_info "Creating Kafka topic: $TOPIC_NAME with $PARTITIONS partitions and replication factor $REPLICATION_FACTOR"
            
            # Create topic
            if kafka_cmd kafka-topics.sh \
                --create \
                --topic "$TOPIC_NAME" \
                --partitions "$PARTITIONS" \
                --replication-factor "$REPLICATION_FACTOR" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
                --if-not-exists >/dev/null 2>&1; then
                
                # Verify topic creation
                if kafka_cmd kafka-topics.sh \
                    --describe \
                    --topic "$TOPIC_NAME" \
                    --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" >/dev/null 2>&1; then
                    log_success "Topic '$TOPIC_NAME' created and verified successfully"
                else
                    log_failed "Topic '$TOPIC_NAME' creation verification failed"
                    exit 1
                fi
            else
                log_failed "Topic '$TOPIC_NAME' creation failed"
                exit 1
            fi
        else
            # List topics: ./kafka.sh topics
            log_info "Listing Kafka topics:"
            kafka_cmd kafka-topics.sh \
                --list \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        fi
        ;;
    
    "ps"|"status")
        # Show topic details
        log_info "Kafka topic details:"
        kafka_cmd kafka-topics.sh \
            --describe \
            --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        ;;
    
    "consumer")
        # Start consumer: ./kafka.sh consumer <topic-name>
        if [[ $# -eq 2 ]]; then
            TOPIC_NAME="$2"
            log_info "Starting consumer for topic: $TOPIC_NAME"
            kafka_cmd kafka-console-consumer.sh \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
                --from-beginning
        else
            log_error "Usage: ./kafka.sh consumer <topic-name>"
            exit 1
        fi
        ;;
    
    "producer")
        # Start producer: ./kafka.sh producer <topic-name>
        if [[ $# -eq 2 ]]; then
            TOPIC_NAME="$2"
            log_info "Starting producer for topic: $TOPIC_NAME (Type messages and press Enter)"
            kafka_cmd kafka-console-producer.sh \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        else
            log_error "Usage: ./kafka.sh producer <topic-name>"
            exit 1
        fi
        ;;
    
    "delete")
        # Delete topic: ./kafka.sh delete <topic-name>
        if [[ $# -eq 2 ]]; then
            TOPIC_NAME="$2"
            log_warning "Deleting Kafka topic: $TOPIC_NAME"
            kafka_cmd kafka-topics.sh \
                --delete \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
            log_success "Topic '$TOPIC_NAME' deleted successfully"
        else
            log_error "Usage: ./kafka.sh delete <topic-name>"
            exit 1
        fi
        ;;
    
    "help"|*)
        echo "Kafka Management Script"
        echo "Usage: $0 [command] [args...]"
        echo ""
        echo "Commands:"
        echo "  setup <name> <part> <repl>      - Complete setup: wait, delete old, create and verify topic"
        echo "  topics                          - List all topics"
        echo "  topics <name> <part> <repl>     - Create topic with partitions and replication factor"
        echo "  ps|status                       - Show topic details"
        echo "  consumer <topic>                - Start console consumer"
        echo "  producer <topic>                - Start console producer"
        echo "  delete <topic>                  - Delete topic"
        echo "  help                            - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 setup ticket.exchange 3 3   - Complete setup for ticket.exchange topic"
        echo "  $0 topics ticket.exchange 3 3  - Create topic with 3 partitions, replication factor 3"
        echo "  $0 topics                       - List all topics"
        echo "  $0 ps                           - Show topic details"
        echo "  $0 consumer ticket.exchange     - Start consumer for ticket.exchange topic"
        ;;
esac