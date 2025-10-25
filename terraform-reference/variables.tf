# Region to deploy into
variable "aws_region" {
  type    = string
  default = "us-west-2"
}

# ECR & ECS settings
variable "ecr_repository_name" {
  type    = string
  default = "ecr_service"
}

variable "service_name" {
  type    = string
  default = "CS6650L2"
}

variable "container_port" {
  type    = number
  default = 8080
}

variable "ecs_count" {
  type    = number
  default = 1
}

# Enable Application Load Balancer
variable "enable_alb" {
  type        = bool
  default     = true
  description = "Enable Application Load Balancer for distributing traffic across ECS tasks"
}

# How long to keep logs
variable "log_retention_days" {
  type    = number
  default = 7
}

# CPU and memory for ECS tasks
variable "cpu" {
  description = "CPU units for the ECS task (256 = 0.25 vCPU, 512 = 0.5 vCPU, 1024 = 1 vCPU)"
  type        = number
  default     = 256
}

variable "memory" {
  description = "Memory for the ECS task in MB"
  type        = number
  default     = 512
}

# Auto-scaling configuration
variable "enable_autoscaling" {
  description = "Enable auto-scaling for ECS service (requires enable_alb = true)"
  type        = bool
  default     = true
}

variable "min_capacity" {
  description = "Minimum number of ECS tasks when auto-scaling is enabled"
  type        = number
  default     = 2
}

variable "max_capacity" {
  description = "Maximum number of ECS tasks when auto-scaling is enabled"
  type        = number
  default     = 4
}

variable "target_cpu_utilization" {
  description = "Target CPU utilization percentage for auto-scaling"
  type        = number
  default     = 70.0
}

variable "scale_out_cooldown" {
  description = "Cooldown period in seconds after scale-out before another scale-out can occur"
  type        = number
  default     = 30
}

variable "scale_in_cooldown" {
  description = "Cooldown period in seconds after scale-in before another scale-in can occur"
  type        = number
  default     = 30
}
