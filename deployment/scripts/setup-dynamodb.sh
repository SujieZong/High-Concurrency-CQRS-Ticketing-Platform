#!/usr/bin/env bash
set -euo pipefail

# Load common utilities
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPT_DIR/common.sh"

# Set log prefix for this script
LOG_PREFIX="DYNAMO"

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
    log_info "Loaded DynamoDB configuration from .env file"
    source "$ENV_FILE"
else
    log_warning ".env file not found at $ENV_FILE, using default values"
fi

# DynamoDB configuration - read from environment variables
# Use localhost when running from host, dynamodb when running inside container
if [[ "${AWS_DYNAMODB_END_POINT:-}" =~ ^http://dynamodb: ]]; then
    DYNAMODB_ENDPOINT="http://localhost:8000"
else
    DYNAMODB_ENDPOINT="${AWS_DYNAMODB_END_POINT:-http://localhost:8000}"
fi
AWS_REGION="${AWS_REGION:-us-west-2}"
TABLE_TICKETS="Tickets"
TABLE_OUTBOX="OutboxEvent"
GSI_NAME="gsi_sent_createdAt"

log_info "DynamoDB connection: $DYNAMODB_ENDPOINT (region: $AWS_REGION)"

# Function to wait for DynamoDB to be ready
wait_for_dynamodb() {
    log_info "Waiting for DynamoDB Local to be ready..."
    local max_attempts=15  
    local attempt=1
    
    while [[ $attempt -le $max_attempts ]]; do
        if aws dynamodb list-tables --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" >/dev/null 2>&1; then
            log_info "DynamoDB Local is ready!"
            return 0
        fi
        
        log_info "Attempt $attempt/$max_attempts: DynamoDB not ready yet..."
        sleep 2  # Unified with MySQL
        ((attempt++))
    done
    
    log_warning "DynamoDB Local failed to start after $max_attempts attempts"
    return 1
}

# Function to create Tickets table
create_tickets_table() {
    log_info "Creating DynamoDB table '${TABLE_TICKETS}'..."
    
    if aws dynamodb list-tables --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" 2>/dev/null | grep -q "\"${TABLE_TICKETS}\""; then
        log_info "Table '${TABLE_TICKETS}' already exists, skipping creation"
        return 0
    fi
    
    aws dynamodb create-table \
        --table-name "${TABLE_TICKETS}" \
        --attribute-definitions AttributeName=ticketId,AttributeType=S \
        --key-schema AttributeName=ticketId,KeyType=HASH \
        --billing-mode PAY_PER_REQUEST \
        --endpoint-url "${DYNAMODB_ENDPOINT}" \
        --region "${AWS_REGION}" >/dev/null
    log_success "Table '${TABLE_TICKETS}' created successfully"
}

# Function to create Outbox table
create_outbox_table() {
    log_info "Creating DynamoDB table '${TABLE_OUTBOX}'..."
    
    if aws dynamodb list-tables --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" 2>/dev/null | grep -q "\"${TABLE_OUTBOX}\""; then
        log_info "Table '${TABLE_OUTBOX}' already exists, skipping creation"
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
        --endpoint-url "${DYNAMODB_ENDPOINT}" \
        --region "${AWS_REGION}" >/dev/null
    log_success "Table '${TABLE_OUTBOX}' created successfully"
}

# Function to verify table creation and status
verify_dynamodb_tables() {
    log_info "Verifying DynamoDB tables..."
    local failed=0
    
    # Verify Tickets table
    if aws dynamodb describe-table --table-name "${TABLE_TICKETS}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" >/dev/null 2>&1; then
        if [[ "$VERBOSE_MODE" == true ]]; then
            table_status=$(aws dynamodb describe-table --table-name "${TABLE_TICKETS}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" --query 'Table.TableStatus' --output text 2>/dev/null || echo "UNKNOWN")
            item_count=$(aws dynamodb describe-table --table-name "${TABLE_TICKETS}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" --query 'Table.ItemCount' --output text 2>/dev/null || echo "0")
            log_success "Table '${TABLE_TICKETS}' verified (status: $table_status, items: $item_count)"
        else
            log_success "Table '${TABLE_TICKETS}' verified"
        fi
    else
        log_failed "Table '${TABLE_TICKETS}' verification failed"
        ((failed++))
    fi
    
    # Verify OutboxEvent table
    if aws dynamodb describe-table --table-name "${TABLE_OUTBOX}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" >/dev/null 2>&1; then
        if [[ "$VERBOSE_MODE" == true ]]; then
            table_status=$(aws dynamodb describe-table --table-name "${TABLE_OUTBOX}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" --query 'Table.TableStatus' --output text 2>/dev/null || echo "UNKNOWN")
            item_count=$(aws dynamodb describe-table --table-name "${TABLE_OUTBOX}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" --query 'Table.ItemCount' --output text 2>/dev/null || echo "0")
            gsi_status=$(aws dynamodb describe-table --table-name "${TABLE_OUTBOX}" --endpoint-url "${DYNAMODB_ENDPOINT}" --region "${AWS_REGION}" --query "Table.GlobalSecondaryIndexes[?IndexName=='${GSI_NAME}'].IndexStatus" --output text 2>/dev/null || echo "UNKNOWN")
            log_success "Table '${TABLE_OUTBOX}' verified (status: $table_status, items: $item_count, GSI '${GSI_NAME}': $gsi_status)"
        else
            log_success "Table '${TABLE_OUTBOX}' verified"
        fi
    else
        log_failed "Table '${TABLE_OUTBOX}' verification failed"
        ((failed++))
    fi
    
    if [[ $failed -gt 0 ]]; then
        log_failed "DynamoDB table verification failed for $failed table(s)"
        return 1
    fi
    
    log_info "All DynamoDB tables verified successfully!"
    return 0
}

# Main setup function
setup_dynamodb() {
    log_step "=== DynamoDB Setup Started ==="
    
    # Validate prerequisites
    if ! command -v aws >/dev/null 2>&1; then
        log_failed "AWS CLI is not installed or not in PATH"
        exit 1
    fi
    
    # Wait for DynamoDB to be ready
    if ! wait_for_dynamodb; then
        exit 1
    fi
    
    # Create tables
    create_tickets_table
    create_outbox_table
    
    # Verify tables were created successfully
    if ! verify_dynamodb_tables; then
        log_failed "DynamoDB setup failed - table verification failed"
        exit 1
    fi
    
    log_step "=== DynamoDB Setup Completed ==="
    log_success "All DynamoDB tables created and verified successfully!"
}

# If script is run directly (not sourced), execute setup
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    setup_dynamodb "$@"
fi