#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Set log prefix for this script
LOG_PREFIX="BUILD"

# Check for quiet mode
QUIET_MODE=false
if [[ "${1:-}" == "--quiet" ]]; then
    QUIET_MODE=true
    shift
fi

ROOT="$(get_project_root)"

# Generate cluster ID without creating temporary containers
generate_cluster_id() {
    if [[ -f "$ROOT/deployment/.env" ]] && grep -q '^CLUSTER_ID=' "$ROOT/deployment/.env"; then
        CLUSTER_ID=$(grep '^CLUSTER_ID=' "$ROOT/deployment/.env" | cut -d'=' -f2)
        [[ "$QUIET_MODE" == false ]] && log_info "Using existing CLUSTER_ID: $CLUSTER_ID"
    else
        # Simple UUID generation - prefer uuidgen, fallback to timestamp
        CLUSTER_ID=$(uuidgen 2>/dev/null | tr '[:upper:]' '[:lower:]' || echo "cluster-$(date +%s)")
        echo "CLUSTER_ID=$CLUSTER_ID" >> "$ROOT/deployment/.env"
        [[ "$QUIET_MODE" == false ]] && log_info "Generated new CLUSTER_ID: $CLUSTER_ID"
    fi
    export CLUSTER_ID
}

# Main build function
build_services() {
    if [[ "$QUIET_MODE" == false ]]; then
        log_step "=== Build Process Started ==="
    fi
    
    # Generate cluster ID first
    generate_cluster_id
    
    # Clean up old containers
    if [[ "$QUIET_MODE" == false ]]; then
        log_step "Stopping old dev containers..."
    fi
    docker rm -f dev-redis dev-dynamodb \
               ticketing-platform query-service purchase-service 2>/dev/null || true
    
    # Always perform clean build for reliability
    if [[ "$QUIET_MODE" == false ]]; then
        log_step "Building all modules with Maven clean package..."
    fi
    cd "$ROOT"
    mvn clean package -DskipTests
    if [[ "$QUIET_MODE" == false ]]; then
        log_info "Maven clean build completed successfully"
    fi
    
    if [[ "$QUIET_MODE" == false ]]; then
        log_step "=== Build Process Completed ==="
    fi
}

# If script is run directly (not sourced), execute build
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    build_services "$@"
fi