variable "projectName" {
  default     = "aws-terraform-sample"
  description = "project name"
  type        = string
}
variable "accountId" {
  description = "aws account Id"
  type        = string
}
variable "env" {
  description = "environment"
  type        = string
}
variable "region" {
  description = "region"
  type        = string
}

variable "s3InvoicesDir" {
  default     = "invoices"
  description = "name of Tradeshift directory to receive the input files"
  type        = string
}
variable "buildFile" {
  description = "Path and name of the Artifact file to be deployed to Lambda in zip format "
  type        = string
}
