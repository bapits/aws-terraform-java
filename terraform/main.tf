### Terraform IAC of aws-terraform-sample


###### KMS KEY ######
resource "aws_kms_key" "enc_kms_key" {
  description             	= "aws Terraform Sample KMS Key 1"
  deletion_window_in_days 	= 7
  tags 						= local.tags
  policy                  = data.aws_iam_policy_document.topic_key_kms_policy.json
}

data "aws_iam_policy_document" "topic_key_kms_policy" {
 statement {
 	sid = "Enable IAM User Permissions"
    effect =  "Allow"
    principals {
    	type        = "AWS"
      	identifiers = ["arn:aws:iam::${var.accountId}:root"]
    }
    actions= ["kms:*"]
    resources = ["*"]
 }
 
 statement {
	sid = "Allow S3 to push encrypted message to SNS"
 	effect =  "Allow"
 	principals {
 		type        = "Service"
 		identifiers = ["s3.amazonaws.com"]
    }
    actions = [
    	"kms:GenerateDataKey",
        "kms:Decrypt"
    ]
    resources = ["*"]
 }
 statement {
	sid = "Allow SNS to receive/push encrypted messages"
 	principals {
 		type        = "Service"
 		identifiers = ["sns.amazonaws.com"]
    }
    actions = [
    	"kms:GenerateDataKey",
        "kms:Decrypt",
        "kms:Encrypt"
    ]
    resources = ["*"]
 }
 statement {
 	sid = "Allow SQS to receive/push encrypted messages"
 	principals {
 		type        = "Service"
 		identifiers = ["sqs.amazonaws.com"]
    }
    actions = [
    	"kms:GenerateDataKey",
        "kms:Decrypt",
        "kms:Encrypt"
    ]
    resources = ["*"]
 }
 statement {
 	sid = "Allow Lambda to receive encrypted messages"
 	principals {
 		type        = "Service"
 		identifiers = ["lambda.amazonaws.com"]
    }
    actions = [
    	"kms:GenerateDataKey",
        "kms:Decrypt",
        "kms:Encrypt"
    ]
    resources = ["*"]
 }
  
}

# alias of the KMS Key
resource "aws_kms_alias" "aws_terraform_sample_key_alias" {
  name          = "alias/aws-terraform-sample-key"
  target_key_id = aws_kms_key.enc_kms_key.key_id
}

###### S3 ######

# create s3 bucket | bucket name can't have underscore (_)
resource "aws_s3_bucket" "s3_bucket" {
  bucket = "${var.projectName}-${var.env}"
  tags = local.tags
}


###### SNS ######

# create sns topic
resource "aws_sns_topic" "sns_topic" {
  name   = "${var.projectName}-topic"
  tags = local.tags
  kms_master_key_id = aws_kms_alias.aws_terraform_sample_key_alias.name
  
  # allow bucket to send notification to topic when file is uploaded
  policy = <<POLICY
{
    "Version":"2012-10-17",
    "Statement":[{
        "Effect": "Allow",
        "Principal": { "Service": "s3.amazonaws.com" },
        "Action": "SNS:Publish",
        "Resource": "arn:aws:sns:*:*:${var.projectName}-topic",
        "Condition": {
            "ArnLike": {
              "aws:SourceArn": "${aws_s3_bucket.s3_bucket.arn}"
            }
        }
    }]
}
POLICY
}


###### S3 - SNS ######

# send message to sns when a file is uploaded in s3 bucket
# added two events for 2 separate suffix
resource "aws_s3_bucket_notification" "bucket_notification_ts" {
  bucket			= aws_s3_bucket.s3_bucket.id
  topic {
    topic_arn		= aws_sns_topic.sns_topic.arn
    events			= ["s3:ObjectCreated:Put"]
    filter_prefix	= "${var.s3InputDir}/${var.s3InputDirTradeshift}"
    filter_suffix	= var.s3InputFilesTypesTS
  }
  
  topic {
    topic_arn		= aws_sns_topic.sns_topic.arn
    events			= ["s3:ObjectCreated:Put"]
    filter_prefix	= "${var.s3InputDir}/${var.s3InputDirGB}"
    filter_suffix	= var.s3InputFilesTypesGB
  }
  
}


###### SQS ######

# create sqs queue
resource "aws_sqs_queue" "queue" {
  name							= "${var.projectName}-queue"
  message_retention_seconds 	= 1209600
  # 2-hours of visibility time
  visibility_timeout_seconds	= 7200
  tags = local.tags
  kms_master_key_id             = aws_kms_alias.aws_terraform_sample_key_alias.name
  kms_data_key_reuse_period_seconds = 300
}

# create policy so topic can send messages to queue
resource "aws_sqs_queue_policy" "queue_policy" {
  queue_url = aws_sqs_queue.queue.id

  policy = <<POLICY
{
  "Version": "2012-10-17",
  "Id": "sqspolicy",
  "Statement": [
    {
      "Sid": "queue subscribes to topic",
      "Effect": "Allow",
      "Principal": {
        "AWS": "*"
      },
      "Action": "sqs:SendMessage",
      "Resource": "${aws_sqs_queue.queue.arn}",
      "Condition": {
        "ArnEquals": {
          "aws:SourceArn": "${aws_sns_topic.sns_topic.arn}"
        }
      }
    }
  ]
}
POLICY
}


###### SNS - SQS ######

# send notification from sns topic to sqs queue
resource "aws_sns_topic_subscription" "sns_sqs" {
  topic_arn = aws_sns_topic.sns_topic.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.queue.arn
}


###### DATABASE ######

# Table SampleNames
resource "aws_dynamodb_table" "ddbtable_names" {
  name             = "${var.projectName}-names"
  hash_key       = "id"
  #billing_mode   = "PAY_PER_REQUEST"
  billing_mode   = "PROVISIONED"
  read_capacity  = 5
  write_capacity = 5
  attribute {
    name = "id"
    type = "S"
  }
  provisioner "local-exec" {
    command = <<EOT
     	aws dynamodb batch-write-item --request-items file://data/data_1.json
		aws dynamodb batch-write-item --request-items file://data/data_2.json
    EOT
  }
  tags = local.tags
  
}

###### LAMBDA ######

# policy lambda to encrypt/decrypt using kms 
resource "aws_iam_role_policy" "policy_kms_lambda" {
  name		= "${var.projectName}-kms-lambda-policy"
  role		= aws_iam_role.role_lambda.id
  policy	= <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
        "Effect": "Allow",
        "Action": "kms:*",
        "Resource": "*"
    }
  ]
}
EOF
}

# policy lambda to access db 
resource "aws_iam_role_policy" "policy_db_lambda" {
  name		= "${var.projectName}-db-lambda-policy"
  role		= aws_iam_role.role_lambda.id
  policy	= <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
        "Effect": "Allow",
        "Action": "dynamodb:*",
        "Resource": ["${aws_dynamodb_table.ddbtable_names.arn}"]
    }
  ]
}
EOF
}

# policy lambda to access S3 
resource "aws_iam_policy" "policy_s3_lambda" {
  name		= "${var.projectName}-s3-lambda-policy"
  policy	= <<EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "s3:Get*",
                "s3:Put*",
                "s3:DeleteObject*",
                "s3:ListBucket"
            ],
            "Resource": [
                "${aws_s3_bucket.s3_bucket.arn}",
                "${aws_s3_bucket.s3_bucket.arn}/*"
            ]
        },
        {
            "Effect": "Allow",
            "Action": [
                "s3:Get*",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::aws-terraform-sample-configs-${var.env}",
                "arn:aws:s3:::aws-terraform-sample-configs-${var.env}/*"
            ]
        }
    ]
}
EOF
}

# policy lambda logging feature 
resource "aws_iam_policy" "policy_lambda_logs" {
  name        = "${var.projectName}-policy-lambda-logs"
  path        = "/"

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*",
      "Effect": "Allow"
    }
  ]
}
EOF
}


# role iam for lambda 
resource "aws_iam_role" "role_lambda" {
  name = "${var.projectName}-role-lambda"
  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
EOF
}

# policy attachment of policy_s3_lambda to role_lambda 
resource "aws_iam_role_policy_attachment" "policy_s3_lambda_attachment" {
  role       = aws_iam_role.role_lambda.name
  policy_arn = aws_iam_policy.policy_s3_lambda.arn
}

# policy attachment of policy_lambda_logs to role_lambda 
resource "aws_iam_role_policy_attachment" "policy_lambda_logs_attachment" {
  role       = aws_iam_role.role_lambda.name
  policy_arn = aws_iam_policy.policy_lambda_logs.arn
}

# lambda function
resource "aws_lambda_function" "function" {
  filename      = var.buildFile
  source_code_hash = filebase64sha256(var.buildFile)
  function_name = "${var.projectName}-function"
  role          = aws_iam_role.role_lambda.arn
  handler       = "com.bapits.labs.sample.aws.terraform.aws.lambda.functions.MyLambdaHandler::handleRequest"
  runtime       = "java11"
  timeout       = 120 # seconds
  memory_size   = 512 # MB
  environment {
    variables = {
      AWS_S3_CONFIG_BUCKET = "${var.configBucket}-${var.env}"
      AWS_S3_CONFIG_BUCKET_SAMPLE = "${var.configBucket}-${var.env}/"
    }
  }
  tags = local.tags
}


###### SQS - LAMBDA ######

# create policy for lambda to be executed from queue
data "aws_iam_policy_document" "policy_sqs_lambda_document" {
  statement {
    actions   = [
      "sqs:ReceiveMessage",
      "sqs:DeleteMessage",
      "sqs:GetQueueAttributes"
    ]
    resources = [
      aws_sqs_queue.queue.arn
    ]
  }
}

# policy lambda to sqs
resource "aws_iam_policy" "policy_sqs_lambda" {
  name   = "${var.projectName}-sqs-lambda-policy"
  policy = data.aws_iam_policy_document.policy_sqs_lambda_document.json
}

# attach policy to the role
resource "aws_iam_role_policy_attachment" "policy_sqs_lambda_attachment" {
  role       = aws_iam_role.role_lambda.name
  policy_arn = aws_iam_policy.policy_sqs_lambda.arn
}

# bind function to queue
resource "aws_lambda_event_source_mapping" "event_sqs_lambda" {
  event_source_arn = aws_sqs_queue.queue.arn
  function_name    = aws_lambda_function.function.arn
}

#EVENT BRIDGE to call lambda handler with schedule
resource "aws_cloudwatch_event_rule" "aws-terraform-sample-lambda-event-rule" {
  name = "aws-terraform-sample-lambda-event-rule"
  description = "scheduled one time per day"
  schedule_expression = "cron(00 05 ? * * *)"
  tags = local.tags
}

resource "aws_cloudwatch_event_target" "aws-terraform-sample-export_lambda_target" {
  arn = aws_lambda_function.function.arn
  rule = aws_cloudwatch_event_rule.aws-terraform-sample-lambda-event-rule.name
}

resource "aws_lambda_permission" "allow_cloudwatch_to_call_aws_terraform_sample_lambda" {
  statement_id = "AllowExecutionFromCloudWatch"
  action = "lambda:InvokeFunction"
  function_name = aws_lambda_function.function.function_name
  principal = "events.amazonaws.com"
  source_arn = aws_cloudwatch_event_rule.aws-terraform-sample-lambda-event-rule.arn
}
