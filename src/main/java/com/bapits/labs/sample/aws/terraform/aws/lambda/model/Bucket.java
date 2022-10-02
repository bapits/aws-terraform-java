package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Bucket {
  @JsonProperty("name")
  String name;
  @JsonProperty("ownerIdentity")
  OwnerIdentity ownerIdentity;
  @JsonProperty("arn")
  String arn;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }


  public OwnerIdentity getOwnerIdentity() {
    return this.ownerIdentity;
  }

  public void setOwnerIdentity(OwnerIdentity ownerIdentity) {
    this.ownerIdentity = ownerIdentity;
  }


  public String getArn() {
    return this.arn;
  }

  public void setArn(String arn) {
    this.arn = arn;
  }

}
