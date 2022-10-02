package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OwnerIdentity {
  @JsonProperty("principalId")
  String principalId;

  public String getPrincipalId() {
    return this.principalId;
  }

  public void setPrincipalId(String principalId) {
    this.principalId = principalId;
  }

}
