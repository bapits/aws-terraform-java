package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RequestParameters {
  @JsonProperty("sourceIPAddress") 
  private String sourceIPAddress;

  public String getSourceIPAddress() {
    return sourceIPAddress;
  }

  public void setSourceIPAddress(String sourceIPAddress) {
    this.sourceIPAddress = sourceIPAddress;
  }
}
