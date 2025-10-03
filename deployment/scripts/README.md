# Deployment Scripts Guide

## Main Scripts Overview

### `localDockerInitiate.sh` - Main Deployment Script
**Purpose**: One-click deployment of the entire CQRS ticketing system

**Features**:
- Orchestrates build, deployment, database setup, and Kafka configuration
- **Default mode**: `./localDockerInitiate.sh` (standard output)
- **Verbose mode**: `./localDockerInitiate.sh --verbose` (detailed output)
- **Environment switching**: `./localDockerInitiate.sh --env <local|aws|prod>`
- **Help**: `./localDockerInitiate.sh --help`

## Individual Scripts

### `build.sh` - Service Builder
**Purpose**: Builds all microservices with Maven clean package

**Features**:
- Generates Kafka Cluster ID (prefers local `uuidgen`)
- Cleans old containers
- Always performs clean build to ensure code changes take effect
- **Usage**: `bash scripts/build.sh [--verbose]`

### `setup-dynamodb.sh` - Database Setup
**Purpose**: Creates and configures DynamoDB tables

**Features**:
- Waits for DynamoDB Local to be ready
- Creates `Tickets` and `OutboxEvent` tables
- Verifies table creation success
- Detects existing tables to avoid duplicates
- **Usage**: `bash scripts/setup-dynamodb.sh [--verbose]`

### `switch-env.sh` - Environment Switcher
**Purpose**: Switches between deployment environments

**Supported Environments**:
- `local` - Local development (Docker Compose + local MySQL)
- `aws` - AWS cloud (RDS + ElastiCache + MSK + DynamoDB)
- `prod` - Production configuration
- **Usage**: `./scripts/switch-env.sh <local|aws|prod>`

**Features**:
- Auto-backs up current `.env` file
- Uses `.env.personal` if available (with real credentials)
- Falls back to `.env.template` (with placeholders)

### `common.sh` - Shared Utilities
**Purpose**: Provides common functions for all scripts

**Features**:
- Colored logging system (`log_info`, `log_success`, `log_failed`, `log_warning`)
- Customizable log prefix (`LOG_PREFIX`)
- Project root detection (`get_project_root`)

### `kafka.sh` - Kafka Manager
**Purpose**: Manages Kafka topics and consumers

**Features**:
- Create/delete topics
- Start console consumers/producers
- View topic status
- **Usage**: `bash scripts/kafka.sh <command> [args]`

### `test-system.sh` - System Validator
**Purpose**: Validates complete CQRS system functionality

**Test Phases**:
1. **Health Check**: Verifies PurchaseService (8080) and QueryService (8081)
2. **CQRS Flow**: Creates ticket → waits for projection → queries result
3. **Infrastructure**: Checks Kafka consumer group `ticketSqlSync`
- **Usage**: `bash scripts/test-system.sh`

## Quick Start

### One-Click Deployment (Recommended)
```bash
# Default deployment with current environment
./localDockerInitiate.sh

# Deploy with specific environment
./localDockerInitiate.sh --env local
./localDockerInitiate.sh --env aws --verbose
```

### Individual Operations
```bash
# Build services only
bash deployment/scripts/build.sh [--verbose]

# Setup database only
bash deployment/scripts/setup-dynamodb.sh [--verbose]

# Switch environment
bash deployment/scripts/switch-env.sh <local|aws|prod>

# Test system
bash deployment/scripts/test-system.sh

# Manage Kafka
bash deployment/scripts/kafka.sh topics ticket.exchange 3 3
bash deployment/scripts/kafka.sh consumer ticket.exchange
```

## Environment Configuration

### Personal vs Template Configuration
- **`.env.template`**: Shared template with placeholder values (committed to git)
- **`.env.personal`**: Your personal config with real credentials (ignored by git)
- **`.env`**: Runtime config (auto-generated, ignored by git)

### Kafka Cluster ID Management
- **CLUSTER_ID is automatically generated** by `build.sh` script
- **DO NOT manually set CLUSTER_ID** in template or personal files
- Once generated, the same ID is reused for consistency
- This ensures proper Kafka cluster formation in Docker

## Script Dependencies

```
localDockerInitiate.sh (main orchestrator)
├── scripts/build.sh (build phase)
├── scripts/setup-dynamodb.sh (database setup)
├── scripts/kafka.sh (kafka setup)
└── scripts/common.sh (shared utilities)

scripts/switch-env.sh (environment switching)
scripts/test-system.sh (system validation)
```