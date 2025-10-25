output "mysql_endpoint" {
  description = "MySQL endpoint"
  value       = aws_db_instance.main.endpoint
}

output "mysql_address" {
  description = "MySQL address"
  value       = aws_db_instance.main.address
}

output "mysql_port" {
  description = "MySQL port"
  value       = aws_db_instance.main.port
}

output "db_password_secret_arn" {
  description = "ARN of the secret containing database credentials"
  value       = aws_secretsmanager_secret.db_password.arn
}

output "database_name" {
  description = "Name of the database"
  value       = aws_db_instance.main.db_name
}
