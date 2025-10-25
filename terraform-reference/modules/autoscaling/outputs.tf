output "scaling_target_id" {
  description = "ID of the auto-scaling target"
  value       = aws_appautoscaling_target.ecs_target.id
}

output "scaling_target_resource_id" {
  description = "Resource ID of the auto-scaling target"
  value       = aws_appautoscaling_target.ecs_target.resource_id
}

output "cpu_scaling_policy_arn" {
  description = "ARN of the CPU-based scaling policy"
  value       = aws_appautoscaling_policy.cpu_scaling.arn
}

output "cpu_scaling_policy_name" {
  description = "Name of the CPU-based scaling policy"
  value       = aws_appautoscaling_policy.cpu_scaling.name
}

output "min_capacity" {
  description = "Minimum number of tasks configured"
  value       = aws_appautoscaling_target.ecs_target.min_capacity
}

output "max_capacity" {
  description = "Maximum number of tasks configured"
  value       = aws_appautoscaling_target.ecs_target.max_capacity
}

output "target_cpu_utilization" {
  description = "Target CPU utilization percentage"
  value       = var.target_cpu
}
