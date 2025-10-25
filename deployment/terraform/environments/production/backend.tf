terraform {
  backend "s3" {
    bucket         = "ticketing-terraform-state-zsj"
    key            = "production/terraform.tfstate"
    region         = "us-west-2"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}