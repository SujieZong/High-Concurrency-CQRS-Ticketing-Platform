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

# MySQL configuration
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"

# Schema file path
SCHEMA_FILE="$SCRIPT_DIR/../../MqProjectionService/src/main/resources/schema.sql"

log_step "=== MySQL Setup Started ==="

# Validate prerequisites
if [[ ! -f "$SCHEMA_FILE" ]]; then
    log_failed "Schema file not found: $SCHEMA_FILE"
    exit 1
fi

# Wait for MySQL to be ready
log_info "Waiting for MySQL to be ready..."
for i in {1..30}; do
    if mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1;" >/dev/null 2>&1; then
        log_info "MySQL is ready!"
        break
    fi
    if [[ $i -eq 30 ]]; then
        log_warning "MySQL failed to start after 30 attempts"
        exit 1
    fi
    log_info "Attempt $i/30: MySQL not ready yet..."
    sleep 2
done

# Execute schema file  
log_info "Creating MySQL database and tables..."

# Check if tables already exist
table_count=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'ticket_platform';" 2>/dev/null | tail -1)
if [[ "$table_count" -gt 0 ]]; then
    log_info "Tables already exist, skipping schema creation"
else
    log_info "Executing schema file: $SCHEMA_FILE"
    if mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" < "$SCHEMA_FILE" 2>/dev/null; then
        log_info "Schema executed successfully"
    else
        log_failed "Schema execution failed"
        exit 1
    fi
fi

# Verify tables
log_info "Verifying table creation..."
created_tables=()
for table in venue zone event ticket; do
    if ! mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" ticket_platform -e "DESCRIBE $table;" >/dev/null 2>&1; then
        log_failed "Table '$table' verification failed"
        exit 1
    fi
    
    # Add to created tables list
    created_tables+=("$table")
    
    if [[ "$VERBOSE_MODE" == true ]]; then
        # Get column information for verbose mode
        columns=$(mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" ticket_platform -e "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='ticket_platform' AND TABLE_NAME='$table';" 2>/dev/null | tail -1)
        log_success "Table '$table' verified (columns: $columns)"
    else
        log_success "Table '$table' verified"
    fi
done

log_step "=== MySQL Setup Completed ==="

# Always show completion summary
log_success "***SUCCEEDED*** Setup completed successfully!"
log_success "[MYSQL] ***SUCCEEDED*** MySQL setup completed successfully!"
log_info "[MYSQL] ***INFO*** Database 'ticket_platform' ready"
if [[ ${#created_tables[@]} -gt 0 ]]; then
    tables_list=$(IFS=', '; echo "${created_tables[*]}")
    log_info "[MYSQL] ***INFO*** Tables available: $tables_list"
fi

# Exit successfully
exit 0