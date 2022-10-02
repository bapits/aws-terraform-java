locals {
  tags = {
    terraform		= "true"
    project 		= var.projectName
    environment 	= var.env
  }
}
