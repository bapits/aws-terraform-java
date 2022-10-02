package com.bapits.labs.sample.aws.terraform.config;

import static org.junit.Assert.assertEquals;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import com.bapits.labs.sample.aws.terraform.utils.FileResourcesUtilsTest;

class PropertiesTest {

  private static final Logger logger = LogManager.getLogger(PropertiesTest.class);

  @Test
  void configurationFileTest() throws Exception {
    ConfigProperties cfg = FileResourcesUtilsTest.getProperties();
    assertEquals(30, cfg.vendorClientConnectionTimeout());
  }

}
