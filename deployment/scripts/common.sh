#!/usr/bin/env bash
# Common utility functions for all scripts

# Color definitions
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Logging functions - can be customized per script by setting LOG_PREFIX
LOG_PREFIX="${LOG_PREFIX:-SCRIPT}"

log_info() {
    echo -e "${GREEN}[${LOG_PREFIX}]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[${LOG_PREFIX}]${NC} $1"
}

log_error() {
    echo -e "${RED}[${LOG_PREFIX}]${NC} $1"
}

log_step() {
    echo -e "${BLUE}[${LOG_PREFIX}]${NC} $1"
}

# Status messages
log_success() {
    log_info "***SUCCEEDED*** $1"
}

log_failed() {
    log_error "***FAILED*** $1"
}

log_warning() {
    log_warn "***WARNING*** $1"
}

# Utility function to get project root
get_project_root() {
    local script_dir="$(cd "$(dirname "${BASH_SOURCE[1]}")" && pwd)"
    if [[ "$script_dir" == *"/deployment/scripts" ]]; then
        # If called from deployment/scripts, go up two levels
        echo "$(dirname "$(dirname "$script_dir")")"
    elif [[ "$script_dir" == *"/scripts" ]]; then
        # If called from scripts, go up one level  
        echo "$(dirname "$script_dir")"
    else
        # If called from project root
        echo "$script_dir"
    fi
}