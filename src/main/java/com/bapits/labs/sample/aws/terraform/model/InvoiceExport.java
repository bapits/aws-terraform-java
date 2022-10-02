package com.bapits.labs.sample.aws.terraform.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"InvoiceId", "POId", "ReceptionDate", "Status", "ReceivedFrom", "VendorName",
    "InvoiceDate"})
public class InvoiceExport {

  @JsonProperty("InvoiceId")
  private String invoiceId;

  @JsonProperty("POId")
  private String poId;

  @JsonProperty("ReceptionDate")
  private String receptionDate;

  @JsonProperty("ReceivedFrom")
  private String receivedFrom;

  @JsonProperty("VendorName")
  private String vendorName;

  @JsonProperty("InvoiceDate")
  private String invoiceDate;

  @JsonProperty("Status")
  private String status;

  @JsonProperty("SourceLegalEntity")
  private String sourceLegalEntity;

  @JsonProperty("DeptId")
  private String deptId;

  @JsonProperty("TotalAmount")
  private String totalAmount;

  public String getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(String invoiceId) {
    this.invoiceId = invoiceId;
  }

  public String getPoId() {
    return poId;
  }

  public void setPoId(String poId) {
    this.poId = poId;
  }

  public String getReceptionDate() {
    return receptionDate;
  }

  public void setReceptionDate(String receptionDate) {
    this.receptionDate = receptionDate;
  }

  public String getReceivedFrom() {
    return receivedFrom;
  }

  public void setReceivedFrom(String receivedFrom) {
    this.receivedFrom = receivedFrom;
  }

  public String getVendorName() {
    return vendorName;
  }

  public void setVendorName(String vendorName) {
    this.vendorName = vendorName;
  }

  public String getInvoiceDate() {
    return invoiceDate;
  }

  public void setInvoiceDate(String invoiceDate) {
    this.invoiceDate = invoiceDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getSourceLegalEntity() {
    return sourceLegalEntity;
  }

  public void setSourceLegalEntity(String sourceLegalEntity) {
    this.sourceLegalEntity = sourceLegalEntity;
  }

  public String getDeptId() {
    return deptId;
  }

  public void setDeptId(String deptId) {
    this.deptId = deptId;
  }

  public String getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(String totalAmount) {
    this.totalAmount = totalAmount;
  }

}
