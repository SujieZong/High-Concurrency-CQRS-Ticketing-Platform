# Use existing LabRole from AWS Learner Lab
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# ECS Cluster
resource "aws_ecs_cluster" "main" {
  name = "${var.environment}-ticketing-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "purchase_service" {
  name              = "/ecs/${var.environment}-purchase-service"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "query_service" {
  name              = "/ecs/${var.environment}-query-service"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "mq_projection_service" {
  name              = "/ecs/${var.environment}-mq-projection-service"
  retention_in_days = 7
}

# Security Group for ECS Tasks
resource "aws_security_group" "ecs_tasks" {
  name        = "${var.environment}-ecs-tasks-sg"
  description = "Security group for ECS tasks"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
    description     = "Allow traffic from ALB"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "${var.environment}-ecs-tasks-sg"
  }
}

# Security Group for ALB
resource "aws_security_group" "alb" {
  name        = "${var.environment}-alb-sg"
  description = "Security group for Application Load Balancer"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow HTTP from anywhere"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
    description = "Allow all outbound traffic"
  }

  tags = {
    Name = "${var.environment}-alb-sg"
  }
}

# Application Load Balancer
resource "aws_lb" "main" {
  name               = "${var.environment}-ticketing-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = var.public_subnet_ids

  enable_deletion_protection = false

  tags = {
    Name        = "${var.environment}-ticketing-alb"
    Environment = var.environment
  }
}

# Target Groups
resource "aws_lb_target_group" "purchase_service" {
  name        = "${var.environment}-purchase-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/api/v1/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  deregistration_delay = 30

  tags = {
    Name = "${var.environment}-purchase-tg"
  }
}

resource "aws_lb_target_group" "query_service" {
  name        = "${var.environment}-query-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    enabled             = true
    path                = "/api/v1/tickets/health"
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  deregistration_delay = 30

  tags = {
    Name = "${var.environment}-query-tg"
  }
}

# ALB Listener
resource "aws_lb_listener" "main" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.purchase_service.arn
  }
}

# Listener Rules for path-based routing
resource "aws_lb_listener_rule" "query_service" {
  listener_arn = aws_lb_listener.main.arn
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.query_service.arn
  }

  condition {
    path_pattern {
      values = ["/api/v1/tickets/*"]
    }
  }
}

# Task Definitions
resource "aws_ecs_task_definition" "purchase_service" {
  family                   = "${var.environment}-purchase-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.purchase_service_cpu
  memory                   = var.purchase_service_memory
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name  = "purchase-service"
      image = "${var.docker_hub_username}/purchaseservice:${var.image_tag}"
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment == "production" ? "prod" : "docker"
        },
        {
          name  = "REDIS_HOST"
          value = var.redis_endpoint
        },
        {
          name  = "KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "MYSQL_HOST"
          value = split(":", var.mysql_endpoint)[0]
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
    }
  ])
}

resource "aws_ecs_task_definition" "query_service" {
  family                   = "${var.environment}-query-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.query_service_cpu
  memory                   = var.query_service_memory
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name  = "query-service"
      image = "${var.docker_hub_username}/queryservice:${var.image_tag}"
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment == "production" ? "prod" : "docker"
        },
        {
          name  = "REDIS_HOST"
          value = var.redis_endpoint
        },
        {
          name  = "MYSQL_HOST"
          value = split(":", var.mysql_endpoint)[0]
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.query_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_task_definition" "mq_projection_service" {
  family                   = "${var.environment}-mq-projection-service"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.mq_projection_cpu
  memory                   = var.mq_projection_memory
  execution_role_arn       = data.aws_iam_role.lab_role.arn
  task_role_arn            = data.aws_iam_role.lab_role.arn

  container_definitions = jsonencode([
    {
      name  = "mq-projection-service"
      image = "${var.docker_hub_username}/mqprojectionservice:${var.image_tag}"
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment == "production" ? "prod" : "docker"
        },
        {
          name  = "KAFKA_BOOTSTRAP_SERVERS"
          value = var.kafka_bootstrap_servers
        },
        {
          name  = "MYSQL_HOST"
          value = split(":", var.mysql_endpoint)[0]
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.mq_projection_service.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# ECS Services
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

  depends_on = [aws_lb_listener.main]
}

resource "aws_ecs_service" "query_service" {
  name            = "${var.environment}-query-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.query_service.arn
  desired_count   = var.query_service_count
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = var.private_subnet_ids
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.query_service.arn
    container_name   = "query-service"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.main]
}

resource "aws_ecs_service" "mq_projection_service" {
  name            = "${var.environment}-mq-projection-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.mq_projection_service.arn
  desired_count   = var.mq_projection_count
  launch_type     = "FARGATE"

  network_configuration {
    security_groups  = [aws_security_group.ecs_tasks.id]
    subnets          = var.private_subnet_ids
    assign_public_ip = false
  }

  depends_on = [aws_lb_listener.main]
}

# Auto Scaling for Purchase Service
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
    target_value = 70.0
  }
}