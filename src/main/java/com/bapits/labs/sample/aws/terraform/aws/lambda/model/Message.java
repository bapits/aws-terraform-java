package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
  @JsonProperty("Records")
  List<Record> records;

  public List<Record> getRecords() {
    return this.records;
  }

  public void setRecords(List<Record> records) {
    this.records = records;
  }

}
