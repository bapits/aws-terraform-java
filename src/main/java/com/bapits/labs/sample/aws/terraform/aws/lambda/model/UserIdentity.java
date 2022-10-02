package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

public class UserIdentity {
  private String principalId;

  public String getPrincipalId() {
    return principalId;
  }

  public void setPrincipalId(String principalId) {
    this.principalId = principalId;
  }
}
