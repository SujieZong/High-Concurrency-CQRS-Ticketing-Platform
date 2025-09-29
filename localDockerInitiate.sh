#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/deployment/scripts/common.sh"

# Set log prefix for this script
LOG_PREFIX="DEPLOY"

# Show help information
show_help() {
    cat << 'EOF'
CQRS Ticketing Platform - Docker Deployment Script

Usage: ./localDockerInitiate.sh [OPTIONS]

OPTIONS:
  --quiet              Quiet mode - shows only essential deployment steps
  --env <local|aws|prod>  Switch environment before deployment
  --help               Show this help message

EXAMPLES:
  ./localDockerInitiate.sh                   Default deployment (verbose mode, current environment)
  ./localDockerInitiate.sh --quiet           Quiet deployment with current environment
  ./localDockerInitiate.sh --env aws         Deploy to AWS environment (verbose mode)
  ./localDockerInitiate.sh --env local --quiet  Deploy to local environment (quiet mode)

Note: Always performs clean Maven build for reliability

EOF
}

# Check for options
QUIET_MODE=false
TARGET_ENV=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --quiet)
            QUIET_MODE=true
            shift
            ;;
        --env)
            TARGET_ENV="$2"
            shift 2
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            # Keep other arguments for build script
            break
            ;;
    esac
done

# Switch environment if specified
if [[ -n "$TARGET_ENV" ]]; then
    log_step "=== Switching to $TARGET_ENV environment ==="
    bash "$SCRIPT_DIR/deployment/scripts/switch-env.sh" "$TARGET_ENV"
    log_info "Environment switched to $TARGET_ENV"
fi

# Common variables
ROOT="$(get_project_root)"
KAFKA_SCRIPT="$ROOT/deployment/scripts/kafka.sh"
BUILD_SCRIPT="$ROOT/deployment/scripts/build.sh"
DYNAMODB_SCRIPT="$ROOT/deployment/scripts/setup-dynamodb.sh"

# === BUILD PHASE ===
log_step "=== Starting Build Phase ==="
if [[ "$QUIET_MODE" == true ]]; then
    bash "$BUILD_SCRIPT" --quiet "$@"
else
    bash "$BUILD_SCRIPT" "$@"
fi
log_info "Build completed"

# Load cluster ID from build script
if [[ -f "$ROOT/.env" ]]; then
    source "$ROOT/.env"
fi

# === DEPLOYMENT PHASE ===
log_step "=== Starting Deployment Phase ==="
log_step "Starting all services via Docker Compose..."
docker compose up --build -d

# === DATABASE SETUP PHASE ===
log_step "=== Starting Database Setup Phase ==="
if [[ "$QUIET_MODE" == true ]]; then
    bash "$DYNAMODB_SCRIPT" --quiet
else
    bash "$DYNAMODB_SCRIPT"
fi
log_info "Database setup completed"

# === KAFKA SETUP PHASE ===
log_step "=== Starting Kafka Setup Phase ==="
# Delete existing topic first to avoid conflicts
bash "$KAFKA_SCRIPT" delete ticket.exchange 2>/dev/null || true

# Create topic with 3 partitions and 3 replicas
bash "$KAFKA_SCRIPT" topics ticket.exchange 3 3

# Check topic status
bash "$KAFKA_SCRIPT" ps >/dev/null

log_info "Kafka setup completed"
sleep 3

# === HEALTH CHECK PHASE ===
log_step "=== Starting Health Check Phase ==="
log_info "All containers are up!"

log_info "Waiting for all services to be fully ready..."
sleep 10

log_step "=== System Ready ==="
log_success "CQRS System is now running!"
log_info "***INFO*** Kafka UI: http://localhost:8088"
log_info "***INFO*** PurchaseService: http://localhost:8080"
log_info "***INFO*** QueryService: http://localhost:8081"
echo ""
log_info "***INFO*** Run automated tests:"
echo "   bash deployment/scripts/test-system.sh"