//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2021.06.25 at 03:59:45 PM CEST
//


package com.bapits.labs.sample.aws.terraform.model.target.sampledata;

import javax.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each Java content interface and Java element interface
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation
 * for XML content. The Java representation of XML content can consist of schema derived interfaces
 * and classes representing the binding of schema type definitions, element declarations and model
 * groups. Factory methods for each of these are provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

  private static final String STATIC_URN =
      "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
  private static final String STATIC_W3_ORG = "http://www.w3.org/2000/09/xmldsig#";

  /**
   * Create an instance of {@link Data1 }
   * 
   */
  public Data1 createData1() {
    return new Data1();
  }



  /**
   * Create an instance of {@link Invoice }
   * 
   */
  public SampleData createSampleData() {
    return new SampleData();
  }

  /**
   * Create an instance of {@link ID }
   * 
   */
  public ID createID() {
    return new ID();
  }

}
