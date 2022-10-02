package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseElements {
  @JsonProperty("x-amz-request-id")
  private String xAmzRequestId;
  @JsonProperty("x-amz-id-2")
  private String xAmzId2;

  public String getxAmzRequestId() {
    return xAmzRequestId;
  }

  public void setxAmzRequestId(String xAmzRequestId) {
    this.xAmzRequestId = xAmzRequestId;
  }

  public String getxAmzId2() {
    return xAmzId2;
  }

  public void setxAmzId2(String xAmzId2) {
    this.xAmzId2 = xAmzId2;
  }
}
