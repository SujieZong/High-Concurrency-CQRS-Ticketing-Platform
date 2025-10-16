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

# MySQL configuration
MYSQL_CONTAINER="${MYSQL_CONTAINER:-mysql-ticketing}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
DATABASE_NAME="${DATABASE_NAME:-ticket_platform}"
SCHEMA_FILE="$SCRIPT_DIR/../../MqProjectionService/src/main/resources/schema.sql"
DATA_FILE="$SCRIPT_DIR/../../MqProjectionService/src/main/resources/data.sql"

# Function to execute MySQL commands inside the container
mysql_exec() {
    docker exec -i "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$@" 2>/dev/null
}

# Function to wait for MySQL container to be ready
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

# Function to create tables from schema file
create_tables() {
    log_info "Creating tables from schema file..."
    
    if [[ ! -f "$SCHEMA_FILE" ]]; then
        log_error "Schema file not found: $SCHEMA_FILE"
        return 1
    fi
    
    # Check if key tables already exist
    log_info "Checking for existing tables..."
    local table_check
    table_check=$(mysql_exec "$DATABASE_NAME" -e "SHOW TABLES LIKE 'venue';" 2>/dev/null)
    
    if [[ -n "$table_check" ]]; then
        log_info "Tables already exist, skipping schema creation"
        return 0
    fi
    
    # Execute schema file inside MySQL container
    log_info "No existing tables found, creating from schema..."
    if docker exec -i "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DATABASE_NAME" < "$SCHEMA_FILE" 2>/dev/null; then
        log_info "Tables created successfully!"
        return 0
    else
        log_error "Failed to create tables from schema file"
        return 1
    fi
}

# Function to insert default data
insert_default_data() {
    log_info "Inserting default data..."
    
    if [[ ! -f "$DATA_FILE" ]]; then
        log_warning "Data file not found: $DATA_FILE, skipping data insertion"
        return 0
    fi
    
    # Check if we already have venues
    local venue_count
    venue_count=$(mysql_exec "$DATABASE_NAME" -e "SELECT COUNT(*) FROM venue;" 2>/dev/null | tail -1)
    
    if [[ "$venue_count" -gt 0 ]]; then
        log_info "Default data already exists ($venue_count venues found), skipping data insertion"
        return 0
    fi
    
    # Execute data file inside MySQL container
    log_info "Inserting default venues, zones, and events..."
    if docker exec -i "$MYSQL_CONTAINER" mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$DATABASE_NAME" < "$DATA_FILE" 2>/dev/null; then
        log_info "Default data inserted successfully!"
        
        # Show inserted data summary
        local venues=$(mysql_exec "$DATABASE_NAME" -e "SELECT COUNT(*) FROM venue;" 2>/dev/null | tail -1)
        local zones=$(mysql_exec "$DATABASE_NAME" -e "SELECT COUNT(*) FROM zone;" 2>/dev/null | tail -1)
        local events=$(mysql_exec "$DATABASE_NAME" -e "SELECT COUNT(*) FROM event;" 2>/dev/null | tail -1)
        log_info "Data summary: $venues venues, $zones zones, $events events"
        
        return 0
    else
        log_error "Failed to insert default data"
        return 1
    fi
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
    
    if [[ ! -f "$DATA_FILE" ]]; then
        log_warning "Data file not found: $DATA_FILE, will skip data insertion"
    fi
    
    # Show connection info (without password) in verbose mode
    if [[ "$VERBOSE_MODE" == true ]]; then
        log_info "MySQL connection: ${MYSQL_USER}@${MYSQL_CONTAINER} (Docker container)"
    fi
    
    # Wait for MySQL to be ready
    if ! wait_for_mysql; then
        exit 1
    fi
    
    # Create database and tables
    create_database
    create_tables
    
    # Insert default data
    insert_default_data
    
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