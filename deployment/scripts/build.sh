#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Set log prefix for this script
LOG_PREFIX="BUILD"

# Check for verbose mode
VERBOSE_MODE=false
if [[ "${1:-}" == "--verbose" ]]; then
    VERBOSE_MODE=true
    shift
fi

ROOT="$(get_project_root)"

# Generate cluster ID without creating temporary containers
generate_cluster_id() {
    if [[ -f "$ROOT/deployment/.env" ]] && grep -q '^CLUSTER_ID=' "$ROOT/deployment/.env"; then
        CLUSTER_ID=$(grep '^CLUSTER_ID=' "$ROOT/deployment/.env" | cut -d'=' -f2)
        if [[ "$VERBOSE_MODE" == true ]]; then
            log_info "Using existing CLUSTER_ID: $CLUSTER_ID (from deployment/.env)"
        else
            log_info "Using existing CLUSTER_ID: $CLUSTER_ID"
        fi
    else
        # Simple UUID generation - prefer uuidgen, fallback to timestamp
        CLUSTER_ID=$(uuidgen 2>/dev/null | tr '[:upper:]' '[:lower:]' || echo "cluster-$(date +%s)")
        echo "CLUSTER_ID=$CLUSTER_ID" >> "$ROOT/deployment/.env"
        if [[ "$VERBOSE_MODE" == true ]]; then
            log_info "Generated new CLUSTER_ID: $CLUSTER_ID (saved to deployment/.env)"
        else
            log_info "Generated new CLUSTER_ID: $CLUSTER_ID"
        fi
    fi
    export CLUSTER_ID
}

# Main build function
build_services() {
    log_step "=== Build Process Started ==="
    
    # Validate prerequisites
    if ! command -v mvn >/dev/null 2>&1; then
        log_failed "Maven is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker >/dev/null 2>&1; then
        log_failed "Docker is not installed or not in PATH"
        exit 1
    fi
    
    # Generate cluster ID first
    generate_cluster_id
    
    # Clean up old containers
    if [[ "$VERBOSE_MODE" == true ]]; then
        log_step "Stopping old dev containers (verbose: dev-redis, dev-dynamodb, ticketing-platform, query-service, purchase-service)..."
    else
        log_step "Stopping old dev containers..."
    fi
    docker rm -f dev-redis dev-dynamodb \
               ticketing-platform query-service purchase-service 2>/dev/null || true
    
    # Always perform clean build for reliability
    if [[ "$VERBOSE_MODE" == true ]]; then
        log_step "Building all modules with Maven clean package (verbose: includes all submodules)..."
        cd "$ROOT"
        mvn clean package -DskipTests -e  # Show stack traces on errors instead of full debug
        log_info "Maven clean build completed successfully with detailed output"
    else
        log_step "Building all modules with Maven clean package..."
        cd "$ROOT"
        mvn clean package -DskipTests
        log_info "Maven clean build completed successfully"
    fi
    
    log_step "=== Build Process Completed ==="
    log_success "Build completed successfully!"
}

# If script is run directly (not sourced), execute build
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    build_services "$@"
fi