package com.bapits.labs.sample.aws.terraform.mapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.bapits.labs.sample.aws.terraform.config.ApplicationProperties;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;
import com.bapits.labs.sample.aws.terraform.model.source.sampledata.Data1;
import com.bapits.labs.sample.aws.terraform.model.source.sampledata.SampleData;
import com.fasterxml.uuid.Generators;

public class XmlSrcToDestMapper {

  private static final Logger logger = LogManager.getLogger(XmlSrcToDestMapper.class);

  ConfigProperties configProperties = null;

  public XmlSrcToDestMapper() {
    configProperties = ApplicationProperties.getInstance().getProperties();
  }

  /**
   * Map Source Xml to Target Xml
   * 
   * @param payment
   */
  public com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData mapSourceToDestination(
      SampleData srcSampleData) {
    com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData targetSampleData = null;
    try {

      // unique Id to be sent to
      String uniqueId = Generators.timeBasedGenerator().generate().toString();

      targetSampleData =
          new com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData();


      for (Data1 data1 : srcSampleData.getData1()) {
        com.bapits.labs.sample.aws.terraform.model.target.sampledata.Data1 tarData1 =
            new com.bapits.labs.sample.aws.terraform.model.target.sampledata.Data1();

        com.bapits.labs.sample.aws.terraform.model.target.sampledata.ID tarId =
            new com.bapits.labs.sample.aws.terraform.model.target.sampledata.ID();
        tarId.setValue(data1.getID().getValue());
        tarData1.setID(tarId);

        targetSampleData.getData1().add(tarData1);
      }

    } catch (Exception e) {
      logger.error("JAXB Marshalling Error", e);
    }

    return targetSampleData;
  }


}
