package com.bapits.labs.sample.aws.terraform.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.aeonbits.owner.ConfigFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApplicationProperties {

  private static final Logger logger = LogManager.getLogger(ApplicationProperties.class);

  private static ApplicationProperties propInstance = null;

  private ConfigProperties configProperties = null;

  private ApplicationProperties() {

  }

  // static method to create instance of Singleton class
  public static ApplicationProperties getInstance() {
    if (propInstance == null)
      propInstance = new ApplicationProperties();

    return propInstance;
  }

  public void loadProperties(InputStream inputStream) throws IOException {
    Properties props = new Properties();
    props.load(inputStream);

    this.configProperties = ConfigFactory.create(ConfigProperties.class, props);

  }

  public ConfigProperties getProperties() throws IllegalStateException {
    if (configProperties == null) {
      logger.error("Properties have not been loaded");
      throw new IllegalStateException("Properties have not been loaded.");
    }

    return configProperties;

  }
}
