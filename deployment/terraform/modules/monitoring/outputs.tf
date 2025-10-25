output "dashboard_name" {
  description = "Name of the CloudWatch dashboard"
  value       = aws_cloudwatch_dashboard.main.dashboard_name
}

output "dashboard_arn" {
  description = "ARN of the CloudWatch dashboard"
  value       = aws_cloudwatch_dashboard.main.dashboard_arn
}

output "sns_topic_arn" {
  description = "ARN of the SNS topic for alarms (if created)"
  value       = var.create_sns_topic ? aws_sns_topic.alarms[0].arn : ""
}

output "alarm_names" {
  description = "Names of all CloudWatch alarms created"
  value = [
    aws_cloudwatch_metric_alarm.purchase_service_cpu_high.alarm_name,
    aws_cloudwatch_metric_alarm.query_service_cpu_high.alarm_name,
    aws_cloudwatch_metric_alarm.mq_projection_cpu_high.alarm_name,
    aws_cloudwatch_metric_alarm.purchase_service_memory_high.alarm_name,
    aws_cloudwatch_metric_alarm.alb_5xx_errors.alarm_name,
    aws_cloudwatch_metric_alarm.alb_response_time.alarm_name
  ]
}
