variable "environment" {
  description = "Environment name (e.g., production, staging)"
  type        = string
}

variable "vpc_id" {
  description = "ID of the VPC"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "List of public subnet IDs"
  type        = list(string)
}

variable "docker_hub_username" {
  description = "Docker Hub username"
  type        = string
}

variable "image_tag" {
  description = "Docker image tag"
  type        = string
  default     = "latest"
}

variable "redis_endpoint" {
  description = "Redis endpoint"
  type        = string
}

variable "mysql_endpoint" {
  description = "MySQL endpoint"
  type        = string
}

variable "kafka_bootstrap_servers" {
  description = "Kafka bootstrap servers"
  type        = string
}

variable "purchase_service_count" {
  description = "Number of purchase service tasks"
  type        = number
  default     = 2
}

variable "query_service_count" {
  description = "Number of query service tasks"
  type        = number
  default     = 2
}

variable "mq_projection_count" {
  description = "Number of MQ projection service tasks"
  type        = number
  default     = 1
}

variable "purchase_service_cpu" {
  description = "CPU units for purchase service"
  type        = number
  default     = 512
}

variable "purchase_service_memory" {
  description = "Memory (MiB) for purchase service"
  type        = number
  default     = 1024
}

variable "query_service_cpu" {
  description = "CPU units for query service"
  type        = number
  default     = 512
}

variable "query_service_memory" {
  description = "Memory (MiB) for query service"
  type        = number
  default     = 1024
}

variable "mq_projection_cpu" {
  description = "CPU units for MQ projection service"
  type        = number
  default     = 512
}

variable "mq_projection_memory" {
  description = "Memory (MiB) for MQ projection service"
  type        = number
  default     = 1024
}

variable "purchase_service_min_count" {
  description = "Minimum number of purchase service tasks for autoscaling"
  type        = number
  default     = 1
}

variable "purchase_service_max_count" {
  description = "Maximum number of purchase service tasks for autoscaling"
  type        = number
  default     = 10
}

variable "query_service_min_count" {
  description = "Minimum number of query service tasks for autoscaling"
  type        = number
  default     = 1
}

variable "query_service_max_count" {
  description = "Maximum number of query service tasks for autoscaling"
  type        = number
  default     = 10
}

variable "mq_projection_min_count" {
  description = "Minimum number of MQ projection service tasks for autoscaling"
  type        = number
  default     = 1
}

variable "mq_projection_max_count" {
  description = "Maximum number of MQ projection service tasks for autoscaling"
  type        = number
  default     = 5
}

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-west-2"
}
