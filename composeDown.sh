#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/deployment/scripts/common.sh"

# Set log prefix for this script
LOG_PREFIX="DOWN"

# Get project root and change to deployment directory
ROOT="$(get_project_root)"
cd "$ROOT/deployment"

log_step "=== Stopping Docker Services ==="
docker compose down

log_success "All services stopped successfully!"