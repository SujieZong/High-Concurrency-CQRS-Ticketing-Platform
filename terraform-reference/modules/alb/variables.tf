variable "service_name" {
  description = "Name of the service for naming resources"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where ALB will be created"
  type        = string
}

variable "subnet_ids" {
  description = "List of subnet IDs for the ALB (must be at least 2 in different AZs)"
  type        = list(string)
}

variable "container_port" {
  description = "Port that containers are listening on"
  type        = number
}

variable "health_check_path" {
  description = "Path for ALB health checks"
  type        = string
  default     = "/health"
}
