output "ecs_cluster_name" {
  description = "Name of the created ECS cluster"
  value       = module.ecs.cluster_name
}

output "ecs_service_name" {
  description = "Name of the running ECS service"
  value       = module.ecs.service_name
}

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer (if enabled)"
  value       = var.enable_alb ? module.alb[0].alb_dns_name : "ALB not enabled"
}

output "alb_url" {
  description = "Full URL of the Application Load Balancer (if enabled)"
  value       = var.enable_alb ? module.alb[0].alb_url : "ALB not enabled"
}

output "target_group_arn" {
  description = "ARN of the target group (if ALB enabled)"
  value       = var.enable_alb ? module.alb[0].target_group_arn : "ALB not enabled"
}

# Auto-scaling outputs (only when enabled)
output "autoscaling_enabled" {
  description = "Whether auto-scaling is enabled"
  value       = var.enable_alb && var.enable_autoscaling
}

output "autoscaling_min_capacity" {
  description = "Minimum number of tasks (if auto-scaling enabled)"
  value       = var.enable_alb && var.enable_autoscaling ? module.autoscaling[0].min_capacity : "Auto-scaling not enabled"
}

output "autoscaling_max_capacity" {
  description = "Maximum number of tasks (if auto-scaling enabled)"
  value       = var.enable_alb && var.enable_autoscaling ? module.autoscaling[0].max_capacity : "Auto-scaling not enabled"
}

output "autoscaling_target_cpu" {
  description = "Target CPU utilization for auto-scaling (if enabled)"
  value       = var.enable_alb && var.enable_autoscaling ? module.autoscaling[0].target_cpu_utilization : "Auto-scaling not enabled"
}

output "autoscaling_policy_arn" {
  description = "ARN of the CPU scaling policy (if auto-scaling enabled)"
  value       = var.enable_alb && var.enable_autoscaling ? module.autoscaling[0].cpu_scaling_policy_arn : "Auto-scaling not enabled"
}