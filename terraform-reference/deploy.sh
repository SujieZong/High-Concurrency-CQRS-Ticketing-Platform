#!/usr/bin/env bash
set -euo pipefail

# deploy.sh - helper to configure AWS temp credentials, apply Terraform, fetch public IP and test Product API
# Usage: ./deploy.sh [deploy|destroy] [num_tasks]

WORKDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$WORKDIR"

function help() {
  cat <<EOF
Usage: $0 [deploy|destroy] [num_tasks]

Commands:
  deploy    Configure AWS temporary credentials, terraform init & apply, then fetch public IP and run a test curl
  destroy   Run 'terraform destroy -auto-approve' to remove resources

Arguments:
  num_tasks   Number of ECS tasks to run (default: 1). Only used with deploy command.

Examples:
  $0 deploy           # Deploy with 1 task (default)
  $0 deploy 3         # Deploy with 3 tasks
  $0 destroy          # Destroy all resources

Before running deploy, make sure you have Docker, AWS CLI and Terraform installed and Docker running.
You will be prompted to run 'aws configure' to enter your temporary credentials.
EOF
}

if [[ ${#} -lt 1 ]] || [[ ${#} -gt 2 ]]; then
  help
  exit 1
fi

CMD=$1
NUM_TASKS=${2:-1}  # Default to 1 task if not specified

if [[ "$CMD" == "deploy" ]]; then
  echo "Deploying with $NUM_TASKS ECS task(s)..."
  echo "Step 1/6: Configure AWS credentials with temporary credentials."
  echo "You will be prompted to enter AWS Access Key, Secret Key, and default region (e.g., us-west-2)."
  echo "If you already configured them, press Enter to skip each prompt."
  aws configure

  read -r -p "Do you have a session token to set? (y/N): " set_token
  if [[ "$set_token" =~ ^[Yy]$ ]]; then
    read -r -p "Enter aws_session_token: " session_token
    aws configure set aws_session_token "$session_token"
  fi

  echo "Step 2/6: Initialize Terraform..."
  terraform init

  echo "Step 3/6: Apply Terraform (this will build the Docker image, push to ECR, and create ECS resources)..."
  terraform apply -auto-approve -var="ecs_count=$NUM_TASKS"

  # Check if ALB is enabled
  ALB_ENABLED=$(terraform output -raw alb_dns_name 2>/dev/null || echo "")
  
  if [[ -n "$ALB_ENABLED" && "$ALB_ENABLED" != "ALB not enabled" ]]; then
    echo ""
    echo "✓ Application Load Balancer detected!"
    echo "  ALB DNS: $ALB_ENABLED"
    echo ""
    echo "Step 4/6: Waiting for ALB to become healthy (this can take 60-90 seconds)..."
    
    # Wait for ALB health checks to pass
    MAX_WAIT=120
    WAITED=0
    
    while [[ $WAITED -lt $MAX_WAIT ]]; do
      HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "http://$ALB_ENABLED/health" 2>/dev/null || echo "000")
      
      if [[ "$HTTP_CODE" == "200" ]]; then
        echo "✓ ALB is healthy and responding!"
        break
      fi
      
      echo "  Waiting for ALB health checks... HTTP $HTTP_CODE (${WAITED}s elapsed)"
      sleep 10
      WAITED=$((WAITED + 10))
    done
    
    if [[ "$HTTP_CODE" != "200" ]]; then
      echo "⚠️  ALB did not become healthy after ${MAX_WAIT}s, but continuing..."
    fi
    
    echo ""
    echo "Step 5/6: Testing ALB endpoint..."
    PORT=80
    PUBLIC_ENDPOINT="$ALB_ENABLED"
    
    echo "Curling http://$PUBLIC_ENDPOINT/health"
    echo
    set +e
    curl -sS "http://$PUBLIC_ENDPOINT/health" || echo "(request failed)"
    echo
    echo ""
    echo "Curling http://$PUBLIC_ENDPOINT/products/1"
    curl -sS "http://$PUBLIC_ENDPOINT/products/1" || echo "(request failed)"
    echo
    set -e

    echo ""
    echo "Step 6/6: Deployment Summary"
    echo "✓ Deployment complete with Application Load Balancer!"
    echo ""
    echo "Load Balancer URL: http://$PUBLIC_ENDPOINT"
    echo "Number of ECS tasks: $NUM_TASKS"
    echo ""
    echo "Available endpoints:"
    echo "  GET  http://$PUBLIC_ENDPOINT/health"
    echo "  GET  http://$PUBLIC_ENDPOINT/products/{productId}"
    echo "  POST http://$PUBLIC_ENDPOINT/products/{productId}/details"
    echo ""
    echo "The ALB will automatically distribute traffic across all $NUM_TASKS task(s)."
    echo "Each request may be handled by a different task for true load balancing!"
    echo ""
    echo "To tear down resources: $0 destroy"
    exit 0
  fi

  echo ""
  echo "Step 4/6: Waiting for ECS task to start (this can take 30-60 seconds)..."
  
  CLUSTER_NAME=$(terraform output -raw ecs_cluster_name)
  SERVICE_NAME=$(terraform output -raw ecs_service_name)

  # Wait up to 90 seconds for a task to appear
  MAX_WAIT=90
  WAITED=0
  TASK_ARN=""
  
  while [[ $WAITED -lt $MAX_WAIT ]]; do
    TASK_ARN=$(aws ecs list-tasks --cluster "$CLUSTER_NAME" --service-name "$SERVICE_NAME" --query 'taskArns[0]' --output text 2>/dev/null || echo "")
    
    if [[ -n "$TASK_ARN" && "$TASK_ARN" != "None" ]]; then
      echo "✓ Task found after ${WAITED}s: $TASK_ARN"
      break
    fi
    
    echo "  Waiting for task... (${WAITED}s elapsed)"
    sleep 5
    WAITED=$((WAITED + 5))
  done

  if [[ -z "$TASK_ARN" || "$TASK_ARN" == "None" ]]; then
    echo ""
    echo "❌ No task found after ${MAX_WAIT}s. Checking service events for errors..."
    echo ""
    aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME" \
      --query 'services[0].events[0:5].[createdAt,message]' --output table
    echo ""
    echo "Possible issues:"
    echo "  1. IAM role 'LabRole' is missing AmazonECSTaskExecutionRolePolicy"
    echo "  2. Container failed to start (check CloudWatch logs: /ecs/$SERVICE_NAME)"
    echo "  3. ECR pull permissions issue"
    echo ""
    echo "To diagnose further, run:"
    echo "  aws ecs describe-services --cluster $CLUSTER_NAME --services $SERVICE_NAME"
    echo "  aws logs tail /ecs/$SERVICE_NAME --follow"
    exit 1
  fi
  
  # Wait for task to reach RUNNING state
  echo "Waiting for task to reach RUNNING state..."
  MAX_WAIT=60
  WAITED=0
  TASK_STATUS=""
  
  while [[ $WAITED -lt $MAX_WAIT ]]; do
    TASK_STATUS=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" \
      --query 'tasks[0].lastStatus' --output text 2>/dev/null || echo "")
    
    if [[ "$TASK_STATUS" == "RUNNING" ]]; then
      echo "✓ Task is RUNNING"
      break
    elif [[ "$TASK_STATUS" == "STOPPED" ]]; then
      echo "❌ Task stopped unexpectedly. Checking reason..."
      STOP_REASON=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" \
        --query 'tasks[0].stoppedReason' --output text)
      CONTAINER_REASON=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" \
        --query 'tasks[0].containers[0].reason' --output text)
      echo "  Stop reason: $STOP_REASON"
      echo "  Container reason: $CONTAINER_REASON"
      echo ""
      echo "Check CloudWatch logs:"
      echo "  aws logs tail /ecs/$SERVICE_NAME --follow"
      exit 1
    fi
    
    echo "  Task status: $TASK_STATUS (${WAITED}s elapsed)"
    sleep 5
    WAITED=$((WAITED + 5))
  done
  
  if [[ "$TASK_STATUS" != "RUNNING" ]]; then
    echo "❌ Task did not reach RUNNING state after ${MAX_WAIT}s"
    exit 1
  fi

  echo ""
  echo "Step 5/6: Retrieving public IP of the running ECS task..."

  ENI_ID=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)
  if [[ -z "$ENI_ID" || "$ENI_ID" == "None" ]]; then
    echo "Could not find ENI for task $TASK_ARN"
    exit 1
  fi

  ENI_ID=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text)
  if [[ -z "$ENI_ID" || "$ENI_ID" == "None" ]]; then
    echo "Could not find ENI for task $TASK_ARN"
    exit 1
  fi

  PUBLIC_IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI_ID" --query 'NetworkInterfaces[0].Association.PublicIp' --output text)
  if [[ -z "$PUBLIC_IP" || "$PUBLIC_IP" == "None" ]]; then
    echo "No public IP associated with ENI $ENI_ID. The task might not have assign_public_ip or still initializing."
    exit 1
  fi

  echo "Public IP: $PUBLIC_IP"

  echo ""
  echo "Step 6/6: Sending sample request to the Product API..."
  PORT=${PRODUCT_PORT:-8080}
  
  # Give the app a moment to fully initialize
  echo "Waiting 5 seconds for app to initialize..."
  sleep 5
  
  echo "Curling http://$PUBLIC_IP:$PORT/health"
  echo
  set +e
  curl -sS "http://$PUBLIC_IP:$PORT/health" || echo "(request failed)"
  echo
  echo ""
  echo "Curling http://$PUBLIC_IP:$PORT/products/1"
  curl -sS "http://$PUBLIC_IP:$PORT/products/1" || echo "(request failed)"
  echo
  set -e

  echo ""
  echo "✓ Deployment complete!"
  echo ""
  echo "Public endpoint: http://$PUBLIC_IP:$PORT"
  echo "Available endpoints:"
  echo "  GET  http://$PUBLIC_IP:$PORT/health"
  echo "  GET  http://$PUBLIC_IP:$PORT/products/{productId}"
  echo "  POST http://$PUBLIC_IP:$PORT/products/{productId}/details"
  echo ""
  echo "To tear down resources: $0 destroy"

elif [[ "$CMD" == "destroy" ]]; then
  echo "Running terraform destroy -auto-approve"
  terraform destroy -auto-approve
  echo "Destroyed resources."
else
  help
  exit 1
fi
