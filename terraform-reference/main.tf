# Wire together four focused modules: network, ecr, logging, ecs.

module "network" {
  source         = "./modules/network"
  service_name   = var.service_name
  container_port = var.container_port
  alb_security_group_id = var.enable_alb ? module.alb[0].alb_security_group_id : ""
}

module "ecr" {
  source          = "./modules/ecr"
  repository_name = var.ecr_repository_name
}

module "logging" {
  source            = "./modules/logging"
  service_name      = var.service_name
  retention_in_days = var.log_retention_days
}

# Reuse an existing IAM role for ECS tasks
data "aws_iam_role" "lab_role" {
  name = "LabRole"
}

# Application Load Balancer (optional, controlled by enable_alb variable)
module "alb" {
  count              = var.enable_alb ? 1 : 0
  source             = "./modules/alb"
  service_name       = var.service_name
  vpc_id             = module.network.vpc_id
  subnet_ids         = module.network.subnet_ids
  container_port     = var.container_port
  health_check_path  = "/health"
}

module "ecs" {
  source             = "./modules/ecs"
  service_name       = var.service_name
  image              = "${module.ecr.repository_url}:latest"
  container_port     = var.container_port
  subnet_ids         = module.network.subnet_ids
  security_group_ids = [module.network.security_group_id]
  execution_role_arn = data.aws_iam_role.lab_role.arn
  task_role_arn      = data.aws_iam_role.lab_role.arn
  log_group_name     = module.logging.log_group_name
  ecs_count          = var.ecs_count
  region             = var.aws_region
  target_group_arn   = var.enable_alb ? module.alb[0].target_group_arn : ""
  alb_security_group_id = var.enable_alb ? module.alb[0].alb_security_group_id : ""
  cpu                = var.cpu
  memory             = var.memory
}

# Auto-scaling configuration (only when ALB is enabled)
module "autoscaling" {
  count = var.enable_alb && var.enable_autoscaling ? 1 : 0
  
  source             = "./modules/autoscaling"
  cluster_name       = module.ecs.cluster_name
  service_name       = module.ecs.service_name
  min_capacity       = var.min_capacity
  max_capacity       = var.max_capacity
  target_cpu         = var.target_cpu_utilization
  scale_out_cooldown = var.scale_out_cooldown
  scale_in_cooldown  = var.scale_in_cooldown
}

// Build & push the Go app image into ECR
resource "docker_image" "app" {
  # Use the URL from the ecr module, and tag it "latest"
  name = "${module.ecr.repository_url}:latest"

  build {
    # relative path from terraform/ → src/
    context = "../src"
    # Dockerfile defaults to "Dockerfile" in that context
  }
}

resource "docker_registry_image" "app" {
  # this will push :latest → ECR
  name = docker_image.app.name
}
