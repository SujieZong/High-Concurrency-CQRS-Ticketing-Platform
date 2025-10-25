#!/usr/bin/env bash
set -euo pipefail

# diagnose.sh - troubleshoot ECS deployment issues
# Usage: ./diagnose.sh

WORKDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$WORKDIR"

echo "ECS Deployment Diagnostics"
echo "==========================="
echo ""

if [[ ! -f ".terraform/terraform.tfstate" ]]; then
  echo "❌ No Terraform state found. Run './deploy.sh deploy' first."
  exit 1
fi

CLUSTER_NAME=$(terraform output -raw ecs_cluster_name 2>/dev/null || echo "")
SERVICE_NAME=$(terraform output -raw ecs_service_name 2>/dev/null || echo "")

if [[ -z "$CLUSTER_NAME" || -z "$SERVICE_NAME" ]]; then
  echo "❌ Could not read cluster/service names from Terraform outputs"
  exit 1
fi

echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo ""

echo "1. Service Status:"
echo "-------------------"
aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME" \
  --query 'services[0].{Status:status,Running:runningCount,Pending:pendingCount,Desired:desiredCount}' \
  --output table

echo ""
echo "2. Recent Service Events (last 5):"
echo "-----------------------------------"
aws ecs describe-services --cluster "$CLUSTER_NAME" --services "$SERVICE_NAME" \
  --query 'services[0].events[0:5].[createdAt,message]' --output table

echo ""
echo "3. Tasks:"
echo "---------"
TASK_ARNS=$(aws ecs list-tasks --cluster "$CLUSTER_NAME" --service-name "$SERVICE_NAME" \
  --query 'taskArns' --output text)

if [[ -z "$TASK_ARNS" || "$TASK_ARNS" == "None" ]]; then
  echo "No tasks found (this usually means tasks failed to start)"
  echo ""
  echo "Common causes:"
  echo "  • IAM role 'LabRole' missing AmazonECSTaskExecutionRolePolicy"
  echo "  • Container crashes on startup"
  echo "  • Image pull failures"
else
  for TASK_ARN in $TASK_ARNS; do
    echo "Task: $TASK_ARN"
    aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" \
      --query 'tasks[0].{Status:lastStatus,Health:healthStatus,StoppedReason:stoppedReason,ContainerReason:containers[0].reason}' \
      --output table
  done
fi

echo ""
echo "4. CloudWatch Logs (last 50 lines):"
echo "------------------------------------"
LOG_GROUP="/ecs/$SERVICE_NAME"

set +e
LOG_STREAMS=$(aws logs describe-log-streams --log-group-name "$LOG_GROUP" \
  --order-by LastEventTime --descending --max-items 1 \
  --query 'logStreams[0].logStreamName' --output text 2>/dev/null)
set -e

if [[ -n "$LOG_STREAMS" && "$LOG_STREAMS" != "None" ]]; then
  aws logs tail "$LOG_GROUP" --since 10m --format short 2>/dev/null || echo "(no recent logs)"
else
  echo "(no log streams found - container may not have started)"
fi

echo ""
echo "5. IAM Role Check:"
echo "------------------"
ROLE_ARN=$(aws iam get-role --role-name LabRole --query 'Role.Arn' --output text 2>/dev/null || echo "NOT_FOUND")
if [[ "$ROLE_ARN" == "NOT_FOUND" ]]; then
  echo "❌ IAM role 'LabRole' not found"
else
  echo "✓ Role exists: $ROLE_ARN"
  echo ""
  echo "Attached policies:"
  aws iam list-attached-role-policies --role-name LabRole --query 'AttachedPolicies[*].PolicyName' --output table
fi

echo ""
echo "==========================="
echo "Diagnostic complete"
