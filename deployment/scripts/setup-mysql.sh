#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Set log prefix for this script
LOG_PREFIX="MYSQL"

# Check for verbose mode
VERBOSE_MODE=false
if [[ "${1:-}" == "--verbose" ]]; then
    VERBOSE_MODE=true
    shift
fi

# Load environment variables from .env file
ROOT="$(get_project_root)"
ENV_FILE="$ROOT/deployment/.env"
if [[ -f "$ENV_FILE" ]]; then
    source "$ENV_FILE"
    log_info "Loaded MySQL configuration from .env file"
else
    log_warning ".env file not found, using default values"
fi

# MySQL configuration - read from environment variables
MYSQL_HOST="${MYSQL_HOST:-host.docker.internal}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${SPRING_DATASOURCE_USERNAME:-root}"
MYSQL_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-root}"
DATABASE_NAME="ticket_platform"
SCHEMA_FILE="$SCRIPT_DIR/../../MqProjectionService/src/main/resources/schema.sql"

# Function to execute MySQL commands with environment variable
mysql_exec() {
    MYSQL_PWD="$MYSQL_PASSWORD" mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" "$@"
}

# Function to wait for MySQL to be ready
wait_for_mysql() {
    log_info "Waiting for MySQL to be ready..."
    local max_attempts=15
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if mysql_exec -e "SELECT 1;" >/dev/null 2>&1; then
            log_info "MySQL is ready!"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: MySQL not ready yet..."
        sleep 2
        ((attempt++))
    done
    
    log_warning "MySQL failed to start after $max_attempts attempts"
    return 1
}

# Function to create database
create_database() {
    log_info "Creating MySQL database '${DATABASE_NAME}'..."
    
    # Check if database already exists
    if mysql_exec -e "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '${DATABASE_NAME}';" 2>/dev/null | grep -q "${DATABASE_NAME}"; then
        log_info "Database '${DATABASE_NAME}' already exists, skipping creation"
        return 0
    fi
    
    mysql_exec -e "CREATE DATABASE ${DATABASE_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
    log_success "Database '${DATABASE_NAME}' created successfully"
}

# Function to create tables from schema
create_tables() {
    log_info "Creating MySQL tables from schema..."
    
    # Check if tables already exist
    local table_count
    table_count=$(mysql_exec -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '${DATABASE_NAME}';" 2>/dev/null | tail -1)
    
    if [[ "$table_count" -gt 0 ]]; then
        log_info "Database '${DATABASE_NAME}' already has $table_count tables, skipping schema creation"
        return 0
    fi
    
    mysql_exec "${DATABASE_NAME}" < "$SCHEMA_FILE" 2>/dev/null
    log_success "Tables created successfully from schema"
}

# Function to verify table creation and status
verify_mysql_tables() {
    log_info "Verifying MySQL tables..."
    local failed=0
    
    # List of expected tables
    local tables=("venue" "zone" "event" "ticket")
    
    for table in "${tables[@]}"; do
        if mysql_exec "${DATABASE_NAME}" -e "DESCRIBE $table;" >/dev/null 2>&1; then
            if [[ "$VERBOSE_MODE" == true ]]; then
                local columns
                local row_count
                columns=$(mysql_exec "${DATABASE_NAME}" -e "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='${DATABASE_NAME}' AND TABLE_NAME='$table';" 2>/dev/null | tail -1)
                row_count=$(mysql_exec "${DATABASE_NAME}" -e "SELECT COUNT(*) FROM $table;" 2>/dev/null | tail -1)
                log_success "Table '$table' verified (columns: $columns, rows: $row_count)"
            else
                log_success "Table '$table' verified"
            fi
        else
            log_failed "Table '$table' verification failed"
            ((failed++))
        fi
    done
    
    if [[ $failed -gt 0 ]]; then
        log_failed "MySQL table verification failed for $failed table(s)"
        return 1
    fi
    
    log_info "All MySQL tables verified successfully!"
    return 0
}

# Main setup function
setup_mysql() {
    log_step "=== MySQL Setup Started ==="
    
    # Validate prerequisites
    if [[ ! -f "$SCHEMA_FILE" ]]; then
        log_failed "Schema file not found: $SCHEMA_FILE"
        exit 1
    fi
    
    if ! command -v mysql >/dev/null 2>&1; then
        log_failed "MySQL client is not installed or not in PATH"
        exit 1
    fi
    
    # Show connection info (without password) in verbose mode
    if [[ "$VERBOSE_MODE" == true ]]; then
        log_info "MySQL connection: ${MYSQL_USER}@${MYSQL_HOST}:${MYSQL_PORT}"
    fi
    
    # Wait for MySQL to be ready
    if ! wait_for_mysql; then
        exit 1
    fi
    
    # Create database and tables
    create_database
    create_tables
    
    # Verify tables were created successfully
    if ! verify_mysql_tables; then
        log_failed "MySQL setup failed - table verification failed"
        exit 1
    fi
    
    log_step "=== MySQL Setup Completed ==="
    log_success "All MySQL resources created and verified successfully!"
}

# If script is run directly (not sourced), execute setup
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    setup_mysql "$@"
fi