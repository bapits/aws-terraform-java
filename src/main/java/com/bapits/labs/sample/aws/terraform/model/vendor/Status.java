package com.bapits.labs.sample.aws.terraform.model.vendor;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Status {

  @JacksonXmlProperty(isAttribute = true)
  protected String text;

  @JacksonXmlProperty(isAttribute = true)
  protected String code;

  @JacksonXmlText
  protected String value;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

}
