variable "environment" {
  description = "Environment name"
  type        = string
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}

# VPC Configuration
variable "vpc_cidr" {
  description = "CIDR block for VPC"
  type        = string
}

variable "availability_zones" {
  description = "List of availability zones"
  type        = list(string)
}

# RDS Configuration
variable "db_instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "db_allocated_storage" {
  description = "Allocated storage for RDS in GB"
  type        = number
}

variable "mysql_version" {
  description = "MySQL version"
  type        = string
}

variable "backup_retention_period" {
  description = "Backup retention period in days"
  type        = number
  default     = 7
}

# ElastiCache Configuration
variable "redis_node_type" {
  description = "ElastiCache node type"
  type        = string
}

variable "redis_num_cache_nodes" {
  description = "Number of cache nodes"
  type        = number
}

variable "redis_version" {
  description = "Redis version"
  type        = string
  default     = "7.0"
}

# MSK Configuration
variable "kafka_instance_type" {
  description = "Kafka instance type"
  type        = string
}

variable "kafka_broker_nodes" {
  description = "Number of Kafka broker nodes"
  type        = number
}

variable "kafka_version" {
  description = "Kafka version"
  type        = string
  default     = "3.5.1"
}

# ECS Service Configuration
variable "purchase_service_count" {
  description = "Number of purchase service tasks"
  type        = number
}

variable "purchase_service_cpu" {
  description = "CPU units for purchase service"
  type        = string
}

variable "purchase_service_memory" {
  description = "Memory for purchase service"
  type        = string
}

variable "purchase_service_min_count" {
  description = "Minimum number of purchase service tasks"
  type        = number
}

variable "purchase_service_max_count" {
  description = "Maximum number of purchase service tasks"
  type        = number
}

variable "query_service_count" {
  description = "Number of query service tasks"
  type        = number
}

variable "query_service_cpu" {
  description = "CPU units for query service"
  type        = string
}

variable "query_service_memory" {
  description = "Memory for query service"
  type        = string
}

variable "mq_projection_count" {
  description = "Number of MQ projection service tasks"
  type        = number
}

variable "mq_projection_cpu" {
  description = "CPU units for MQ projection service"
  type        = string
}

variable "mq_projection_memory" {
  description = "Memory for MQ projection service"
  type        = string
}

# Docker Configuration
variable "docker_hub_username" {
  description = "Docker Hub username"
  type        = string
  default     = ""
}

variable "image_tag" {
  description = "Docker image tag"
  type        = string
  default     = "latest"
}
