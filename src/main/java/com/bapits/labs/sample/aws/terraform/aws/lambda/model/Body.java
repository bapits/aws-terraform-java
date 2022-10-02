package com.bapits.labs.sample.aws.terraform.aws.lambda.model;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Body {

  @JsonProperty("Type")
  private String type;
  @JsonProperty("MessageId")
  private String messageId;
  @JsonProperty("TopicArn")
  private String topicArn;
  @JsonProperty("Subject")
  private String subject;
  @JsonProperty("Message")
  private Message message;
  @JsonProperty("Timestamp")
  private Date timestamp;
  @JsonProperty("SignatureVersion")
  private String signatureVersion;
  @JsonProperty("Signature")
  private String signature;
  @JsonProperty("SigningCertURL")
  private String signingCertURL;
  @JsonProperty("UnsubscribeURL")
  private String unsubscribeURL;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getTopicArn() {
    return topicArn;
  }

  public void setTopicArn(String topicArn) {
    this.topicArn = topicArn;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message = message;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getSignatureVersion() {
    return signatureVersion;
  }

  public void setSignatureVersion(String signatureVersion) {
    this.signatureVersion = signatureVersion;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getSigningCertURL() {
    return signingCertURL;
  }

  public void setSigningCertURL(String signingCertURL) {
    this.signingCertURL = signingCertURL;
  }

  public String getUnsubscribeURL() {
    return unsubscribeURL;
  }

  public void setUnsubscribeURL(String unsubscribeURL) {
    this.unsubscribeURL = unsubscribeURL;
  }
}
