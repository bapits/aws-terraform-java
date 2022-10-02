package com.bapits.labs.sample.aws.terraform.mapper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Comparison;
import org.xmlunit.diff.ComparisonResult;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.DifferenceEvaluator;
import org.xmlunit.util.Convert;
import com.bapits.labs.sample.aws.terraform.application.GlobalConstants;
import com.bapits.labs.sample.aws.terraform.aws.dynamodb.DBProcessor;
import com.bapits.labs.sample.aws.terraform.aws.s3.S3Processor;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;
import com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData;
import com.bapits.labs.sample.aws.terraform.service.MapperService;
import com.bapits.labs.sample.aws.terraform.utils.FileResourcesUtilsTest;
import com.bapits.labs.sample.aws.terraform.utils.FileUtils;
import com.bapits.labs.sample.aws.terraform.utils.XmlUtils;

/**
 * Test the functionalities provided by MapperService
 */
class MapperServiceTest {

  private static final Logger logger = LogManager.getLogger(MapperServiceTest.class);

  List<String> ELEMENTS_TO_IGNORE = Arrays.asList(new String[] {""});
  List<String> ATTRIBUTES_TO_IGNORE =
      Arrays.asList(new String[] {"@timestamp", "@otherDate"});


  private ConfigProperties configProperties = null;

  @BeforeEach
  public void setup() {
    try {
      configProperties = FileResourcesUtilsTest.getProperties();

      S3Processor mockS3Processor = mock(S3Processor.class);

      Field instance = S3Processor.class.getDeclaredField("s3Instance");
      instance.setAccessible(true);
      instance.set(instance, mockS3Processor);

      Mockito.when(mockS3Processor.putFileOnS3(eq(configProperties.awsS3BucketTarget()),
          Mockito.anyString(), Mockito.any(File.class))).thenReturn(true);


      DBProcessor mockDBProcessor = mock(DBProcessor.class);
      Field instanceDB = DBProcessor.class.getDeclaredField("dbInstance");
      instanceDB.setAccessible(true);
      instanceDB.set(instanceDB, mockDBProcessor);

      // test properties file
      File inputConfigFile =
          FileResourcesUtilsTest.getFileFromResource("test_application.properties");

      Mockito
          .when(mockS3Processor.readContentFileFromS3(Mockito.nullable(String.class),
              Mockito.eq(GlobalConstants.APP_CONFIG_FILE)))
          .thenReturn(new FileInputStream(inputConfigFile));

    } catch (Exception excp) {
      logger.error("Error Occurred: {}", excp.getMessage(), excp);
    }
  }

  /**
   * Test the Source Xml transformation to Target Xml
   */
  @Test
  void testXmlSourceToTargetTransformation() {
    try {

      // file to write Target Xml after transformation/mapping
      String outputFilePath = "build/target_file.xml";

      boolean result = Files.deleteIfExists(Paths.get(outputFilePath));

      String expectedOutputFilePath = "xml/test_sample_out.xml";

      File fileXML = FileResourcesUtilsTest.getFileFromResource("xml/test_sample_in.xml");

      File expectedOutputFile = FileResourcesUtilsTest.getFileFromResource(expectedOutputFilePath);


      MapperService convertXmlService = new MapperService();
      SampleData sampleData =
          convertXmlService.processXMLFile(fileXML);

      // the result should be true
      assertNotNull(sampleData);

      XmlUtils.marshalTargetXmlToFileResource(outputFilePath, sampleData, Arrays.asList(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
          "<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.045/SampleData.dtd\">"));

      String sResult = new String(Files.readAllBytes(Paths.get(outputFilePath)));

      String sExpectedString =
          new String(Files.readAllBytes(Paths.get(expectedOutputFile.getAbsolutePath())));

      this.compareResults(sResult, sExpectedString);

    } catch (IOException e) {
      logger.error(e.getMessage(), e);
      assertFalse(true);
    }

  }


  /**
   * Compare the result obtained after processing and expected result using xmlunit
   *
   * @param sResultXml
   * @param sExpectedResultXml
   */
  private void compareResults(String sResultXml, String sExpectedResultXml) {

    try {
      logger.info("resultXml: " + sResultXml);
      logger.info("expectedResultXml : " + sExpectedResultXml);

      // workaround to ignore the xml validations
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setValidating(false);
      dbf.setFeature("http://xml.org/sax/features/validation", false);
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      Document control = Convert.toDocument(Input.fromString(sResultXml).build(), dbf);
      Document test = Convert.toDocument(Input.fromString(sExpectedResultXml).build(), dbf);

      logger.info("Elements will be ignored:" + ELEMENTS_TO_IGNORE);
      logger.info("Attribute will be ignored:" + ATTRIBUTES_TO_IGNORE);

      final Diff documentDiff =
          DiffBuilder.compare(control).withTest(test).ignoreComments().ignoreWhitespace()
              .withNodeFilter(node -> !ELEMENTS_TO_IGNORE.contains(node.getNodeName()))
              .withDifferenceEvaluator(new DifferenceEvaluator() {
                @Override
                public ComparisonResult evaluate(Comparison comparison, ComparisonResult outcome) {
                  Comparison.Detail control = comparison.getControlDetails();
                  if (control != null && control.getXPath() != null) {
                    String xPath = control.getXPath();
                    // attributes are at the end of the XPath example: /cXML[1]/@payloadID
                    int index = xPath.lastIndexOf("/");
                    if (index > 0) {
                      String sAttr = xPath.substring(xPath.lastIndexOf("/") + 1);
                      if (!sAttr.isEmpty() && ATTRIBUTES_TO_IGNORE.contains(sAttr)) {
                        outcome = ComparisonResult.EQUAL;
                      }
                    }
                  }
                  return outcome;
                }
              }).build();

      assertFalse(documentDiff.toString(), documentDiff.hasDifferences());
    } catch (ParserConfigurationException e) {
      logger.error(e.getMessage(), e);
      assertFalse(true);
    }

  }

  /**
   * Test Source zip file send to Vendor
   */
  @Test
  void testZipSrcToTargetMapping() {
    try {

      String sSrcZipFileName = "test_sample_file.zip";

      // original file should be copied to build for testing
      String outputFilePath = "build/test_sample_file.zip";

      boolean result = Files.deleteIfExists(Paths.get(outputFilePath));
      if (result) {
        logger.info("Existing File <{}> deleted.", outputFilePath);
      }

      File fileZip = FileResourcesUtilsTest.getFileFromResource("zip/" + sSrcZipFileName);

      // delete the unzipped dir, if already created
      Path zipDir = Paths.get(configProperties.appTempDirPath(),
          FileUtils.getFileNameWithoutExtensions(sSrcZipFileName));
      if (Files.exists(zipDir)) {
        boolean resultZipped = FileUtils.deleteDir(zipDir);
        if (resultZipped) {
          logger.info("Existing dir <{}> deleted.", zipDir);
        }
      }

      MapperService convertXmlService = new MapperService();
      SampleData targetSampleData = convertXmlService.processZipFile(fileZip);

      Assert.assertEquals("1", targetSampleData.getData1().get(0).getID().getValue());

      logger.info("Finished Test");

    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      assertFalse(true);
    }
  }



}
