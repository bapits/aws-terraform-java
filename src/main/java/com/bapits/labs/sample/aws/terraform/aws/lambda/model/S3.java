package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class S3 {
  @JsonProperty("s3SchemaVersion")
  String s3SchemaVersion;
  @JsonProperty("configurationId")
  String configurationId;
  @JsonProperty("bucket")
  Bucket bucket;
  @JsonProperty("object")
  S3Object s3Object;

  public String getS3SchemaVersion() {
    return this.s3SchemaVersion;
  }

  public void setS3SchemaVersion(String s3SchemaVersion) {
    this.s3SchemaVersion = s3SchemaVersion;
  }

  public String getConfigurationId() {
    return this.configurationId;
  }

  public void setConfigurationId(String configurationId) {
    this.configurationId = configurationId;
  }

  public Bucket getBucket() {
    return this.bucket;
  }

  public void setBucket(Bucket bucket) {
    this.bucket = bucket;
  }

  public S3Object getS3Object() {
    return s3Object;
  }

  public void setS3Object(S3Object s3Object) {
    this.s3Object = s3Object;
  }

}
