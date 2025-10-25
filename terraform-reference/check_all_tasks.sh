#!/usr/bin/env bash
set -euo pipefail

# check_all_tasks.sh - Display all running ECS tasks with their IPs
# This proves that each task runs on a separate server with different IP addresses

WORKDIR="$(cd "$(dirname "$0")" && pwd)"
cd "$WORKDIR"

echo "================================================"
echo "Checking All Running ECS Tasks"
echo "================================================"
echo ""

CLUSTER_NAME=$(terraform output -raw ecs_cluster_name 2>/dev/null || echo "")
SERVICE_NAME=$(terraform output -raw ecs_service_name 2>/dev/null || echo "")

if [[ -z "$CLUSTER_NAME" || -z "$SERVICE_NAME" ]]; then
    echo "❌ Error: Could not get cluster/service name from Terraform"
    echo "   Make sure you've deployed with: ./deploy.sh deploy <num_tasks>"
    exit 1
fi

echo "Cluster: $CLUSTER_NAME"
echo "Service: $SERVICE_NAME"
echo ""

# Get all task ARNs
TASK_ARNS=$(aws ecs list-tasks --cluster "$CLUSTER_NAME" --service-name "$SERVICE_NAME" --query 'taskArns[]' --output text 2>/dev/null || echo "")

if [[ -z "$TASK_ARNS" ]]; then
    echo "❌ No tasks found running"
    echo "   Deploy tasks first with: ./deploy.sh deploy <num_tasks>"
    exit 1
fi

TASK_COUNT=$(echo $TASK_ARNS | wc -w | tr -d ' ')
echo "Found $TASK_COUNT task(s) running"
echo ""
echo "================================================"

TASK_NUM=1
ALL_PUBLIC_IPS=()

for TASK_ARN in $TASK_ARNS; do
    echo ""
    echo "Task #$TASK_NUM"
    echo "────────────────────────────────────────────────"
    echo "ARN: $TASK_ARN"
    
    # Get task status
    TASK_STATUS=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" \
        --query 'tasks[0].lastStatus' --output text 2>/dev/null || echo "UNKNOWN")
    echo "Status: $TASK_STATUS"
    
    # Get ENI ID
    ENI_ID=$(aws ecs describe-tasks --cluster "$CLUSTER_NAME" --tasks "$TASK_ARN" \
        --query "tasks[0].attachments[0].details[?name=='networkInterfaceId'].value" --output text 2>/dev/null || echo "")
    
    if [[ -n "$ENI_ID" && "$ENI_ID" != "None" ]]; then
        echo "Network Interface: $ENI_ID"
        
        # Get private IP
        PRIVATE_IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI_ID" \
            --query 'NetworkInterfaces[0].PrivateIpAddress' --output text 2>/dev/null || echo "N/A")
        echo "Private IP: $PRIVATE_IP"
        
        # Get public IP
        PUBLIC_IP=$(aws ec2 describe-network-interfaces --network-interface-ids "$ENI_ID" \
            --query 'NetworkInterfaces[0].Association.PublicIp' --output text 2>/dev/null || echo "")
        
        if [[ -n "$PUBLIC_IP" && "$PUBLIC_IP" != "None" ]]; then
            echo "Public IP: $PUBLIC_IP ⭐"
            echo "Health Check: curl http://$PUBLIC_IP:8080/health"
            ALL_PUBLIC_IPS+=("$PUBLIC_IP")
        else
            echo "Public IP: (none assigned)"
        fi
    else
        echo "Network Interface: (not found)"
    fi
    
    TASK_NUM=$((TASK_NUM + 1))
done

echo ""
echo "================================================"
echo "Summary"
echo "================================================"
echo ""

if [[ ${#ALL_PUBLIC_IPS[@]} -gt 0 ]]; then
    echo "✓ All Public IPs:"
    for i in "${!ALL_PUBLIC_IPS[@]}"; do
        echo "  Task $((i+1)): ${ALL_PUBLIC_IPS[$i]}"
    done
    echo ""
    
    if [[ ${#ALL_PUBLIC_IPS[@]} -gt 1 ]]; then
        # Check if all IPs are different
        UNIQUE_IPS=$(printf '%s\n' "${ALL_PUBLIC_IPS[@]}" | sort -u | wc -l | tr -d ' ')
        if [[ "$UNIQUE_IPS" -eq "${#ALL_PUBLIC_IPS[@]}" ]]; then
            echo "✅ CONFIRMED: All tasks have DIFFERENT IP addresses!"
            echo "   Each task is running on a SEPARATE server."
        else
            echo "⚠️  WARNING: Some tasks share the same IP address"
        fi
    fi
    echo ""
    echo "For Load Testing:"
    echo "  Your Locust tests should target ONE of these IPs"
    echo "  (or use a Load Balancer if configured)"
    echo ""
    echo "  Example: http://${ALL_PUBLIC_IPS[0]}:8080"
else
    echo "⚠️  No public IPs found"
    echo "   Tasks may be using a Load Balancer or private networking"
fi

echo ""
echo "================================================"
