environment = "production"
aws_region  = "us-west-2"

# VPC Configuration
vpc_cidr           = "10.0.0.0/16"
availability_zones = ["us-west-2a", "us-west-2b", "us-west-2c"]

# RDS Configuration
db_instance_class     = "db.t3.large"
db_allocated_storage  = 100
mysql_version        = "8.0.35"
backup_retention_period = 30

# ElastiCache Configuration
redis_node_type       = "cache.r7g.large"
redis_num_cache_nodes = 3
redis_version        = "7.0"

# MSK Configuration
kafka_instance_type = "kafka.m5.large"
kafka_broker_nodes  = 3
kafka_version      = "3.5.1"

# ECS Service Configuration
purchase_service_count = 3
purchase_service_cpu   = "1024"
purchase_service_memory = "2048"
purchase_service_min_count = 2
purchase_service_max_count = 10

query_service_count = 3
query_service_cpu   = "512"
query_service_memory = "1024"

mq_projection_count = 2
mq_projection_cpu   = "512"
mq_projection_memory = "1024"