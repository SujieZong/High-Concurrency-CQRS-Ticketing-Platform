#!/usr/bin/env bash

# Kafka management script for Docker-based Kafka cluster
# Usage: ./kafka.sh [command] [args...]

set -euo pipefail

# Kafka broker configuration
KAFKA_CONTAINER="kafka-1"
KAFKA_BOOTSTRAP_SERVERS="kafka-1:9092,kafka-2:9092,kafka-3:9092"

# Function to run Kafka commands in Docker container
kafka_cmd() {
    docker exec -it "$KAFKA_CONTAINER" /opt/bitnami/kafka/bin/"$@"
}

case "${1:-help}" in
    "topics")
        if [[ $# -eq 4 ]]; then
            # Create topic: ./kafka.sh topics <topic-name> <partitions> <replication-factor>
            TOPIC_NAME="$2"
            PARTITIONS="$3"
            REPLICATION_FACTOR="$4"
            
            echo "Creating Kafka topic: $TOPIC_NAME with $PARTITIONS partitions and replication factor $REPLICATION_FACTOR"
            kafka_cmd kafka-topics.sh \
                --create \
                --topic "$TOPIC_NAME" \
                --partitions "$PARTITIONS" \
                --replication-factor "$REPLICATION_FACTOR" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
                --if-not-exists
        else
            # List topics: ./kafka.sh topics
            echo "Listing Kafka topics:"
            kafka_cmd kafka-topics.sh \
                --list \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        fi
        ;;
    
    "ps"|"status")
        # Show topic details
        echo "Kafka topic details:"
        kafka_cmd kafka-topics.sh \
            --describe \
            --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        ;;
    
    "consumer")
        # Start consumer: ./kafka.sh consumer <topic-name>
        if [[ $# -eq 2 ]]; then
            TOPIC_NAME="$2"
            echo "Starting consumer for topic: $TOPIC_NAME"
            kafka_cmd kafka-console-consumer.sh \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS" \
                --from-beginning
        else
            echo "Usage: ./kafka.sh consumer <topic-name>"
            exit 1
        fi
        ;;
    
    "producer")
        # Start producer: ./kafka.sh producer <topic-name>
        if [[ $# -eq 2 ]]; then
            TOPIC_NAME="$2"
            echo "Starting producer for topic: $TOPIC_NAME (Type messages and press Enter)"
            kafka_cmd kafka-console-producer.sh \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        else
            echo "Usage: ./kafka.sh producer <topic-name>"
            exit 1
        fi
        ;;
    
    "delete")
        # Delete topic: ./kafka.sh delete <topic-name>
        if [[ $# -eq 2 ]]; then
            TOPIC_NAME="$2"
            echo "Deleting Kafka topic: $TOPIC_NAME"
            kafka_cmd kafka-topics.sh \
                --delete \
                --topic "$TOPIC_NAME" \
                --bootstrap-server "$KAFKA_BOOTSTRAP_SERVERS"
        else
            echo "Usage: ./kafka.sh delete <topic-name>"
            exit 1
        fi
        ;;
    
    "help"|*)
        echo "Kafka Management Script"
        echo "Usage: $0 [command] [args...]"
        echo ""
        echo "Commands:"
        echo "  topics                          - List all topics"
        echo "  topics <name> <part> <repl>     - Create topic with partitions and replication factor"
        echo "  ps|status                       - Show topic details"
        echo "  consumer <topic>                - Start console consumer"
        echo "  producer <topic>                - Start console producer"
        echo "  delete <topic>                  - Delete topic"
        echo "  help                            - Show this help"
        echo ""
        echo "Examples:"
        echo "  $0 topics ticket.exchange 3 3  - Create topic with 3 partitions, replication factor 3"
        echo "  $0 topics                       - List all topics"
        echo "  $0 ps                           - Show topic details"
        echo "  $0 consumer ticket.exchange     - Start consumer for ticket.exchange topic"
        ;;
esac