terraform {
  required_version = ">= 1.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

module "networking" {
  source = "../../modules/networking"
  
  environment        = var.environment
  vpc_cidr          = var.vpc_cidr
  availability_zones = var.availability_zones
}

module "rds" {
  source = "../../modules/rds"
  
  environment           = var.environment
  vpc_id               = module.networking.vpc_id
  private_subnet_ids   = module.networking.private_subnet_ids
  ecs_security_group_id = module.ecs.ecs_security_group_id
  
  db_instance_class    = var.db_instance_class
  allocated_storage    = var.db_allocated_storage
  mysql_version        = var.mysql_version
}

module "elasticache" {
  source = "../../modules/elasticache"
  
  environment           = var.environment
  vpc_id               = module.networking.vpc_id
  private_subnet_ids   = module.networking.private_subnet_ids
  ecs_security_group_id = module.ecs.ecs_security_group_id
  
  redis_node_type       = var.redis_node_type
  redis_num_cache_nodes = var.redis_num_cache_nodes
}

module "msk" {
  source = "../../modules/msk"
  
  environment           = var.environment
  vpc_id               = module.networking.vpc_id
  private_subnet_ids   = module.networking.private_subnet_ids
  ecs_security_group_id = module.ecs.ecs_security_group_id
  
  kafka_instance_type  = var.kafka_instance_type
  kafka_broker_nodes   = var.kafka_broker_nodes
}

module "ecs" {
  source = "../../modules/ecs"
  
  environment             = var.environment
  vpc_id                 = module.networking.vpc_id
  private_subnet_ids     = module.networking.private_subnet_ids
  public_subnet_ids      = module.networking.public_subnet_ids
  
  docker_hub_username    = var.docker_hub_username
  image_tag             = var.image_tag
  
  redis_endpoint         = module.elasticache.redis_endpoint
  mysql_endpoint         = module.rds.mysql_endpoint
  kafka_bootstrap_servers = module.msk.kafka_bootstrap_servers
  
  purchase_service_count = var.purchase_service_count
  query_service_count    = var.query_service_count
  mq_projection_count    = var.mq_projection_count
}

module "monitoring" {
  source = "../../modules/monitoring"
  
  environment = var.environment
  ecs_cluster_name = module.ecs.cluster_name
  alb_arn = module.ecs.alb_arn
}