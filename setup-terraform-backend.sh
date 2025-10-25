#!/bin/bash
# filepath: /Users/sujiezong/Desktop/NEU/6620-cloud-computing/High-Concurrency-CQRS-Ticketing-Platform/setup-terraform-backend.sh

BUCKET_NAME="ticketing-terraform-state-zsj"

# Create S3 bucket for state
aws s3api create-bucket \
  --bucket $BUCKET_NAME \
  --region us-west-2 \
  --create-bucket-configuration LocationConstraint=us-west-2

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket $BUCKET_NAME \
  --region us-west-2 \
  --versioning-configuration Status=Enabled

# Enable encryption
aws s3api put-bucket-encryption \
  --bucket $BUCKET_NAME \
  --region us-west-2 \
  --server-side-encryption-configuration '{
    "Rules": [
      {
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        }
      }
    ]
  }'

# Create DynamoDB table for state locking (only if not exists)
aws dynamodb describe-table --table-name terraform-state-lock --region us-west-2 2>/dev/null || \
aws dynamodb create-table \
  --table-name terraform-state-lock \
  --region us-west-2 \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5

echo "Terraform backend setup complete!"
echo "S3 Bucket: $BUCKET_NAME"
echo "DynamoDB Table: terraform-state-lock"
echo "Region: us-west-2"
echo ""
echo "⚠️  Update your backend.tf with:"
echo "bucket = \"$BUCKET_NAME\""