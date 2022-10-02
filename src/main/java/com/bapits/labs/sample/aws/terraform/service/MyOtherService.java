package com.bapits.labs.sample.aws.terraform.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.bapits.labs.sample.aws.terraform.config.ApplicationProperties;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;

public class MyOtherService {

  private static final Logger logger = LogManager.getLogger(MyOtherService.class);

  private ConfigProperties configProperties = null;

  public MyOtherService() {
    configProperties = ApplicationProperties.getInstance().getProperties();
  }

  public boolean processS3File() {

    boolean bResult = false;

    // do Other Service Processing
    
    return bResult;

  }

  
}
