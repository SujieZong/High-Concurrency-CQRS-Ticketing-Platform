#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Set log prefix for this script
LOG_PREFIX="DYNAMO"

# Check for quiet mode
QUIET_MODE=false
if [[ "${1:-}" == "--quiet" ]]; then
    QUIET_MODE=true
    shift
fi

# DynamoDB configuration
DynamoDB_ENDPOINT="http://localhost:8000"
AWS_REGION="us-west-2"
TABLE_TICKETS="Tickets"
TABLE_OUTBOX="OutboxEvent"
GSI_NAME="gsi_sent_createdAt"

# Function to wait for DynamoDB to be ready
wait_for_dynamodb() {
    [[ "$QUIET_MODE" == false ]] && log_info "Waiting for DynamoDB Local to be ready..."
    local max_attempts=15  # Reduced from 30
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if aws dynamodb list-tables --endpoint-url "${DynamoDB_ENDPOINT}" --region "${AWS_REGION}" >/dev/null 2>&1; then
            [[ "$QUIET_MODE" == false ]] && log_info "DynamoDB Local is ready!"
            return 0
        fi
        
        [[ "$QUIET_MODE" == false ]] && log_info "Attempt $attempt/$max_attempts: DynamoDB not ready yet..."
        sleep 3  # Slightly longer sleep, fewer attempts
        ((attempt++))
    done
    
    log_warning "DynamoDB Local failed to start after $max_attempts attempts"
    return 1
}

# Function to create Tickets table
create_tickets_table() {
    [[ "$QUIET_MODE" == false ]] && log_info "Creating DynamoDB table '${TABLE_TICKETS}'..."
    
    if aws dynamodb list-tables --endpoint-url "${DynamoDB_ENDPOINT}" --region "${AWS_REGION}" 2>/dev/null | grep -q "\"${TABLE_TICKETS}\""; then
        [[ "$QUIET_MODE" == false ]] && log_info "Table '${TABLE_TICKETS}' already exists, skipping creation"
        return 0
    fi
    
    aws dynamodb create-table \
        --table-name "${TABLE_TICKETS}" \
        --attribute-definitions AttributeName=ticketId,AttributeType=S \
        --key-schema AttributeName=ticketId,KeyType=HASH \
        --billing-mode PAY_PER_REQUEST \
        --endpoint-url "${DynamoDB_ENDPOINT}" \
        --region "${AWS_REGION}" >/dev/null
    
    [[ "$QUIET_MODE" == false ]] && log_info "Table '${TABLE_TICKETS}' created successfully"
}

# Function to create Outbox table
create_outbox_table() {
    [[ "$QUIET_MODE" == false ]] && log_info "Creating DynamoDB table '${TABLE_OUTBOX}'..."
    
    if aws dynamodb list-tables --endpoint-url "${DynamoDB_ENDPOINT}" --region "${AWS_REGION}" 2>/dev/null | grep -q "\"${TABLE_OUTBOX}\""; then
        [[ "$QUIET_MODE" == false ]] && log_info "Table '${TABLE_OUTBOX}' already exists, skipping creation"
        return 0
    fi
    
    aws dynamodb create-table \
        --table-name "${TABLE_OUTBOX}" \
        --attribute-definitions \
            AttributeName=id,AttributeType=S \
            AttributeName=sent,AttributeType=N \
            AttributeName=createdAt,AttributeType=S \
        --key-schema AttributeName=id,KeyType=HASH \
        --global-secondary-indexes "[
            {
                \"IndexName\": \"${GSI_NAME}\",
                \"KeySchema\": [
                    {\"AttributeName\": \"sent\", \"KeyType\": \"HASH\"},
                    {\"AttributeName\": \"createdAt\", \"KeyType\": \"RANGE\"}
                ],
                \"Projection\": {\"ProjectionType\": \"ALL\"}
            }
        ]" \
        --billing-mode PAY_PER_REQUEST \
        --endpoint-url "${DynamoDB_ENDPOINT}" \
        --region "${AWS_REGION}" >/dev/null
    
    [[ "$QUIET_MODE" == false ]] && log_info "Table '${TABLE_OUTBOX}' created successfully"
}

# Function to verify table creation
verify_tables() {
    [[ "$QUIET_MODE" == false ]] && log_info "Verifying table creation..."
    
    if aws dynamodb describe-table --table-name "${TABLE_TICKETS}" --endpoint-url "${DynamoDB_ENDPOINT}" --region "${AWS_REGION}" >/dev/null 2>&1; then
        [[ "$QUIET_MODE" == false ]] && log_success "Table '${TABLE_TICKETS}' verified"
    else
        log_failed "Table '${TABLE_TICKETS}' verification failed"
        return 1
    fi
    
    if aws dynamodb describe-table --table-name "${TABLE_OUTBOX}" --endpoint-url "${DynamoDB_ENDPOINT}" --region "${AWS_REGION}" >/dev/null 2>&1; then
        [[ "$QUIET_MODE" == false ]] && log_success "Table '${TABLE_OUTBOX}' verified"
    else
        log_failed "Table '${TABLE_OUTBOX}' verification failed"
        return 1
    fi
    
    [[ "$QUIET_MODE" == false ]] && log_info "All DynamoDB tables verified successfully!"
}

# Main setup function
setup_dynamodb() {
    [[ "$QUIET_MODE" == false ]] && log_step "=== DynamoDB Setup Started ==="
    
    # Wait for DynamoDB to be ready
    if ! wait_for_dynamodb; then
        exit 1
    fi
    
    # Create tables
    create_tickets_table
    create_outbox_table
    
    # Verify tables
    verify_tables
    
    [[ "$QUIET_MODE" == false ]] && log_step "=== DynamoDB Setup Completed ==="
}

# If script is run directly (not sourced), execute setup
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    setup_dynamodb "$@"
fi