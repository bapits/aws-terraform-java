# BAPits Lab AWS-Terraform-Java Sample
A sample Lambda application written in java, with a simple use case of data mapping of data(xml) files received in S3 Bucket. 
Below is the process of the application. 
	A zip file which contains an xml file is received in S3, a notification is generated to SNS which is then put in the SQS and then this message is sent to Lambda.
	S3->SNS->SQS->Lambda->S3

## AWS Services:
	Lambda
	Simple Notification Service (SNS)
	Message Queue Service (SQS)
	Cloud Object Storage (S3)
	CloudWatch
	DynamoDb
	
## Tech Stack:
	Java 11
	AWS SDK
	Gradle

## Build
	```Shell
		./gradle.bat clean build 
	```
## Deploy
	The project could be deployed to AWS using Terraform (Infrastructure as Code). Terraform scripts could be found in terraform/* directory 
