resource "aws_ecs_cluster" "main" {
  name = "${var.environment}-ticketing-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  configuration {
    execute_command_configuration {
      logging = "OVERRIDE"

      log_configuration {
        cloud_watch_log_group_name = aws_cloudwatch_log_group.ecs_exec.name
      }
    }
  }
}

resource "aws_cloudwatch_log_group" "ecs_exec" {
  name              = "/ecs/${var.environment}-exec"
  retention_in_days = 7
}

resource "aws_ecs_task_definition" "purchase_service" {
  family                   = "${var.environment}-purchase-service"
  network_mode            = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                     = var.purchase_service_cpu
  memory                  = var.purchase_service_memory
  execution_role_arn      = aws_iam_role.ecs_task_execution.arn
  task_role_arn           = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name  = "purchase-service"
      image = "${var.docker_hub_username}/purchase-service:${var.image_tag}"
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment
        },
        {
          name  = "REDIS_HOST"
          value = var.redis_endpoint
        },
        {
          name  = "KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        }
      ]

      secrets = [
        {
          name      = "MYSQL_PASSWORD"
          valueFrom = aws_secretsmanager_secret.db_password.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.purchase_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/api/v1/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])
}

resource "aws_ecs_service" "purchase_service" {
  name            = "${var.environment}-purchase-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.purchase_service.arn
  desired_count   = var.purchase_service_count
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = var.private_subnet_ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.purchase_service.arn
    container_name   = "purchase-service"
    container_port   = 8080
  }

  service_registries {
    registry_arn = aws_service_discovery_service.purchase_service.arn
  }

  depends_on = [aws_lb_listener.main]
}

resource "aws_appautoscaling_target" "purchase_service" {
  max_capacity       = var.purchase_service_max_count
  min_capacity       = var.purchase_service_min_count
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.purchase_service.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "purchase_service_cpu" {
  name               = "${var.environment}-purchase-service-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.purchase_service.resource_id
  scalable_dimension = aws_appautoscaling_target.purchase_service.scalable_dimension
  service_namespace  = aws_appautoscaling_target.purchase_service.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 50
  }
}