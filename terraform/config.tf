provider "aws" {
  region = "eu-west-1"
}


data "aws_caller_identity" "current" {}

terraform {
    backend "s3" {
      encrypt = true
      #dynamodb_table = "terraform-state-lock"
      region  = "eu-west-1"
      bucket  = "sfa-terraform-states"
      key     = "eks/terraform.tfstate"
    }
  required_version = ">= 0.12.28"
}
