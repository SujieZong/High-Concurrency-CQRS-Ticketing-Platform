variable "cluster_name" {
  description = "ECS cluster name"
  type        = string
}

variable "service_name" {
  description = "ECS service name"
  type        = string
}

variable "min_capacity" {
  description = "Minimum number of tasks"
  type        = number
  default     = 2
}

variable "max_capacity" {
  description = "Maximum number of tasks"
  type        = number
  default     = 4
}

variable "target_cpu" {
  description = "Target CPU utilization percentage"
  type        = number
  default     = 70.0
}

variable "scale_out_cooldown" {
  description = "Cooldown period for scale-out actions (seconds)"
  type        = number
  default     = 30
}

variable "scale_in_cooldown" {
  description = "Cooldown period for scale-in actions (seconds)"
  type        = number
  default     = 30
}