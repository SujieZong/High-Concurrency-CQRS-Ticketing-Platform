#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"
LOG_PREFIX="ENV"

ROOT="$(get_project_root)"
ENV_FILE="$ROOT/deployment/.env"
ENV_TEMPLATE="$ROOT/deployment/.env.template"
ENV_PERSONAL="$ROOT/deployment/.env.personal"

# Simple environment switching
switch_environment() {
    local env="$1"
    
    # Validate environment
    case "$env" in
        local|aws|prod) ;;
        *) log_failed "Invalid environment. Use: local, aws, prod"; exit 1 ;;
    esac
    
    # Check template exists
    [[ -f "$ENV_TEMPLATE" ]] || { log_failed ".env.template not found"; exit 1; }
    
    log_step "Switching to $env environment..."
    
    # Simple backup - keep only one backup
    [[ -f "$ENV_FILE" ]] && cp "$ENV_FILE" "$ENV_FILE.backup"
    
    # Use personal config if exists, otherwise use template
    if [[ -f "$ENV_PERSONAL" ]]; then
        cp "$ENV_PERSONAL" "$ENV_FILE"
        log_info "Using personal config (.env.personal) with real credentials"
    else
        cp "$ENV_TEMPLATE" "$ENV_FILE"
        log_warning "Using template config (.env.template) with placeholder values"
        log_info "Create .env.personal for real credentials"
    fi
    sed -i '' "s/DEPLOYMENT_ENV=.*/DEPLOYMENT_ENV=$env/" "$ENV_FILE"
    
    # Update active configurations based on environment
    case "$env" in
        "local")
            sed -i '' 's/${SPRING_DATASOURCE_URL_[A-Z]*}/${SPRING_DATASOURCE_URL_LOCAL}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATASOURCE_USERNAME_[A-Z]*}/${SPRING_DATASOURCE_USERNAME_LOCAL}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATASOURCE_PASSWORD_[A-Z]*}/${SPRING_DATASOURCE_PASSWORD_LOCAL}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATA_REDIS_HOST_[A-Z]*}/${SPRING_DATA_REDIS_HOST_LOCAL}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATA_REDIS_PORT_[A-Z]*}/${SPRING_DATA_REDIS_PORT_LOCAL}/g' "$ENV_FILE"
            sed -i '' 's/${AWS_DYNAMODB_END_POINT_[A-Z]*}/${AWS_DYNAMODB_END_POINT_LOCAL}/g' "$ENV_FILE"
            log_success "Switched to LOCAL environment"
            ;;
        "aws")
            sed -i '' 's/${SPRING_DATASOURCE_URL_[A-Z]*}/${SPRING_DATASOURCE_URL_AWS}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATASOURCE_USERNAME_[A-Z]*}/${SPRING_DATASOURCE_USERNAME_AWS}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATASOURCE_PASSWORD_[A-Z]*}/${SPRING_DATASOURCE_PASSWORD_AWS}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATA_REDIS_HOST_[A-Z]*}/${SPRING_DATA_REDIS_HOST_AWS}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATA_REDIS_PORT_[A-Z]*}/${SPRING_DATA_REDIS_PORT_AWS}/g' "$ENV_FILE"
            sed -i '' 's/${AWS_DYNAMODB_END_POINT_[A-Z]*}/${AWS_DYNAMODB_END_POINT_AWS}/g' "$ENV_FILE"
            log_success "Switched to AWS environment"
            log_warning "Set AWS credentials: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY"
            ;;
        "prod")
            sed -i '' 's/${SPRING_DATASOURCE_URL_[A-Z]*}/${SPRING_DATASOURCE_URL_PROD}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATASOURCE_USERNAME_[A-Z]*}/${SPRING_DATASOURCE_USERNAME_PROD}/g' "$ENV_FILE"
            sed -i '' 's/${SPRING_DATASOURCE_PASSWORD_[A-Z]*}/${SPRING_DATASOURCE_PASSWORD_PROD}/g' "$ENV_FILE"
            log_success "Switched to PRODUCTION environment"
            log_warning "Verify all production settings before deployment"
            ;;
    esac
    
    log_info "Current environment: $env"
}

# Show usage
show_usage() {
    echo "Environment Switcher - CQRS Ticketing Platform"
    echo "Usage: $0 <local|aws|prod>"
    echo "Examples: $0 local"
}

# Main
[[ $# -eq 0 ]] && { show_usage; exit 1; }
switch_environment "$1"