package com.bapits.labs.sample.aws.terraform.model.vendor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseRoot {

  @JacksonXmlProperty(isAttribute = true, localName = "Response")
  protected Response response;

  @JacksonXmlProperty(isAttribute = true)
  protected String payloadID;

  @JacksonXmlProperty(isAttribute = true)
  protected String timestamp;

  public Response getResponse() {
    return response;
  }

  public void setResponse(Response response) {
    this.response = response;
  }

  public String getPayloadID() {
    return payloadID;
  }

  public void setPayloadID(String payloadID) {
    this.payloadID = payloadID;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

}
