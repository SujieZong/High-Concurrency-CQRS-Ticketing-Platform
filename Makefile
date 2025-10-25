.PHONY: help init plan apply destroy

ENVIRONMENT ?= production
AWS_REGION ?= us-west-2

help:
	@echo "Available commands:"
	@echo "  make init ENVIRONMENT=<env>     - Initialize Terraform"
	@echo "  make plan ENVIRONMENT=<env>     - Create Terraform plan"
	@echo "  make apply ENVIRONMENT=<env>    - Apply Terraform changes"
	@echo "  make destroy ENVIRONMENT=<env>  - Destroy infrastructure"
	@echo "  make fmt                        - Format Terraform files"
	@echo "  make validate                   - Validate Terraform configuration"

init:
	cd deployment/terraform/environments/$(ENVIRONMENT) && terraform init

plan:
	cd deployment/terraform/environments/$(ENVIRONMENT) && \
	terraform plan -var-file=terraform.tfvars -out=tfplan

apply:
	cd deployment/terraform/environments/$(ENVIRONMENT) && \
	terraform apply tfplan

destroy:
	cd deployment/terraform/environments/$(ENVIRONMENT) && \
	terraform destroy -var-file=terraform.tfvars -auto-approve

fmt:
	terraform fmt -recursive deployment/terraform/

validate:
	cd deployment/terraform/environments/$(ENVIRONMENT) && terraform validate

docker-build:
	docker-compose build

docker-push:
	docker-compose push

deploy: docker-build docker-push apply

rollback:
	@echo "Rolling back to previous version..."
	cd deployment/terraform/environments/$(ENVIRONMENT) && \
	terraform apply -var="image_tag=$(shell git rev-parse --short HEAD~1)"