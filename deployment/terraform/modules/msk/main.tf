resource "aws_security_group" "kafka" {
  name        = "${var.environment}-kafka-sg"
  description = "Security group for Kafka"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 9092
    to_port         = 9092
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  ingress {
    from_port       = 2181
    to_port         = 2181
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.environment}-kafka-sg"
  }
}

resource "aws_msk_cluster" "main" {
  cluster_name           = "${var.environment}-ticketing-kafka"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.kafka_broker_nodes

  broker_node_group_info {
    instance_type   = var.kafka_instance_type
    client_subnets  = var.private_subnet_ids
    security_groups = [aws_security_group.kafka.id]
    
    storage_info {
      ebs_storage_info {
        volume_size = var.kafka_volume_size
      }
    }
  }

  encryption_info {
    encryption_at_rest_kms_key_arn = aws_kms_key.kafka.arn
    
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  configuration_info {
    arn      = aws_msk_configuration.main.arn
    revision = aws_msk_configuration.main.latest_revision
  }

  logging_info {
    broker_logs {
      cloudwatch_logs {
        enabled   = true
        log_group = aws_cloudwatch_log_group.kafka.name
      }
    }
  }

  tags = {
    Name        = "${var.environment}-ticketing-kafka"
    Environment = var.environment
  }
}

resource "aws_msk_configuration" "main" {
  kafka_versions = [var.kafka_version]
  name           = "${var.environment}-ticketing-kafka-config"

  server_properties = <<PROPERTIES
auto.create.topics.enable = true
delete.topic.enable = true
log.retention.hours = 168
default.replication.factor = 3
min.insync.replicas = 2
PROPERTIES
}

resource "aws_kms_key" "kafka" {
  description = "KMS key for Kafka encryption"
}

resource "aws_cloudwatch_log_group" "kafka" {
  name              = "/aws/kafka/${var.environment}-ticketing"
  retention_in_days = 7
}