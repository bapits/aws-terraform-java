package com.bapits.labs.sample.aws.terraform.aws.dynamodb.model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName = "file-tracking")
public class FileTracking {

  private String id;

  private String originName;

  private String receivedFrom;

  private String rowCreationDate;

  private String hashCode;

  private String fileId;

  private String status;

  private String logStreamName;


  @DynamoDBHashKey(attributeName = "id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @DynamoDBAttribute(attributeName = "originName")
  public String getOriginName() {
    return originName;
  }

  public void setOriginName(String originName) {
    this.originName = originName;
  }


  @DynamoDBAttribute(attributeName = "rowCreationDate")
  public String getRowCreationDate() {
    return rowCreationDate;
  }

  public void setRowCreationDate(String rowCreationDate) {
    this.rowCreationDate = rowCreationDate;
  }

  @DynamoDBAttribute(attributeName = "hashCode")
  public String getHashCode() {
    return hashCode;
  }



  public void setHashCode(String hashCode) {
    this.hashCode = hashCode;
  }

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  @DynamoDBAttribute(attributeName = "status")
  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  @DynamoDBAttribute(attributeName = "receivedFrom")
  public String getReceivedFrom() {
    return receivedFrom;
  }

  public void setReceivedFrom(String receivedFrom) {
    this.receivedFrom = receivedFrom;
  }

  public String getLogStreamName() {
    return logStreamName;
  }

  public void setLogStreamName(String logStreamName) {
    this.logStreamName = logStreamName;
  }

}

