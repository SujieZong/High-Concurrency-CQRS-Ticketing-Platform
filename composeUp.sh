#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/deployment/scripts/common.sh"

# Set log prefix for this script
LOG_PREFIX="DEPLOY"

# Helper function to run scripts with appropriate verbosity
run_script() {
    local script="$1"
    local phase_name="$2"
    shift 2  # Remove script and phase_name from $@
    
    if [[ "$VERBOSE_MODE" == true ]]; then
        bash "$script" --verbose "$@" || { log_failed "$phase_name failed"; exit 1; }
    else
        # In quiet mode, suppress script output and only show our progress
        bash "$script" "$@" > /dev/null 2>&1 || { log_failed "$phase_name failed"; exit 1; }
        log_success "$phase_name completed"
    fi
}

# Show help information
show_help() {
    cat << 'EOF'
CQRS Ticketing Platform - Docker Deployment Script

Usage: ./composeUp.sh [OPTIONS]

OPTIONS:
  --verbose            Verbose mode - shows detailed deployment information
  --env <local|aws|prod>  Switch environment before deployment
  --help               Show this help message

EXAMPLES:
  ./composeUp.sh                        Default deployment (standard mode, current environment)
  ./composeUp.sh --verbose              Verbose deployment with detailed information
  ./composeUp.sh --env aws              Deploy to AWS environment (standard mode)
  ./composeUp.sh --env local --verbose  Deploy to local environment (verbose mode)

Note: Always performs clean Maven build for reliability

EOF
}

# Check for options
VERBOSE_MODE=false
TARGET_ENV=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --verbose)
            VERBOSE_MODE=true
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
MYSQL_SCRIPT="$ROOT/deployment/scripts/setup-mysql.sh"

# === BUILD PHASE ===
log_step "=== Starting Build Phase ==="
run_script "$BUILD_SCRIPT" "Build phase" "$@"

# Load cluster ID from build script
if [[ -f "$ROOT/deployment/.env" ]]; then
    source "$ROOT/deployment/.env"
fi

# === DEPLOYMENT PHASE ===
log_step "=== Starting Deployment Phase ==="

# Change to deployment directory to ensure .env file is loaded correctly
cd "$ROOT/deployment"

if [[ "$VERBOSE_MODE" == true ]]; then
    log_step "Starting all services via Docker Compose (verbose mode)..."
    docker compose up --build -d
else
    log_step "Starting all services via Docker Compose..."
    docker compose up --build -d > /dev/null 2>&1
    log_success "Docker services started successfully"
fi

cd "$ROOT"

# === DATABASE SETUP PHASE ===
log_step "=== Starting Database Setup Phase ==="

# Setup MySQL first (for QueryService and MqProjectionService)
run_script "$MYSQL_SCRIPT" "MySQL setup"

# Setup DynamoDB (for PurchaseService)  
run_script "$DYNAMODB_SCRIPT" "DynamoDB setup"

# === KAFKA SETUP PHASE ===
log_step "=== Starting Kafka Setup Phase ==="

# Setup Kafka topic using the unified approach
run_script "$KAFKA_SCRIPT" "Kafka setup" setup ticket.exchange 3 3

# === HEALTH CHECK PHASE ===
# === SYSTEM READY ===
log_step "=== System Deployment Completed Successfully ==="
log_success "CQRS Ticketing Platform is now running!"
echo ""
log_info "System Status:"
log_info "  Kafka UI: http://localhost:8088"
log_info "  PurchaseService: http://localhost:8080"  
log_info "  QueryService: http://localhost:8081"
echo ""
log_info "Run system tests:"
echo "   bash deployment/scripts/test-system.sh"