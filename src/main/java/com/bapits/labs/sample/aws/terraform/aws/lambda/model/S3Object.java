package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class S3Object {
  @JsonProperty("key")
  String key;
  @JsonProperty("size")
  int size;
  @JsonProperty("eTag")
  String eTag;
  @JsonProperty("sequencer")
  String sequencer;

  public String getKey() {
    return this.key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public int getSize() {
    return this.size;
  }

  public void setSize(int size) {
    this.size = size;
  }


  public String getETag() {
    return this.eTag;
  }

  public void setETag(String eTag) {
    this.eTag = eTag;
  }

  public String getSequencer() {
    return this.sequencer;
  }

  public void setSequencer(String sequencer) {
    this.sequencer = sequencer;
  }

}
