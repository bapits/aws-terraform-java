package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import java.util.Date;

public class Record {
  private String eventVersion;
  private String eventSource;
  private String awsRegion;
  private Date eventTime;
  private String eventName;
  private UserIdentity userIdentity;
  private RequestParameters requestParameters;
  private ResponseElements responseElements;
  private S3 s3;

  public String getEventVersion() {
    return eventVersion;
  }

  public void setEventVersion(String eventVersion) {
    this.eventVersion = eventVersion;
  }

  public String getEventSource() {
    return eventSource;
  }

  public void setEventSource(String eventSource) {
    this.eventSource = eventSource;
  }

  public Date getEventTime() {
    return eventTime;
  }

  public void setEventTime(Date eventTime) {
    this.eventTime = eventTime;
  }

  public String getEventName() {
    return eventName;
  }

  public void setEventName(String eventName) {
    this.eventName = eventName;
  }

  public UserIdentity getUserIdentity() {
    return userIdentity;
  }

  public void setUserIdentity(UserIdentity userIdentity) {
    this.userIdentity = userIdentity;
  }

  public RequestParameters getRequestParameters() {
    return requestParameters;
  }

  public void setRequestParameters(RequestParameters requestParameters) {
    this.requestParameters = requestParameters;
  }

  public ResponseElements getResponseElements() {
    return responseElements;
  }

  public void setResponseElements(ResponseElements responseElements) {
    this.responseElements = responseElements;
  }

  public S3 getS3() {
    return s3;
  }

  public void setS3(S3 s3) {
    this.s3 = s3;
  }

  public String getAwsRegion() {
    return awsRegion;
  }

  public void setAwsRegion(String awsRegion) {
    this.awsRegion = awsRegion;
  }

}
