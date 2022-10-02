package com.bapits.labs.sample.aws.terraform.aws.lambda;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.bapits.labs.sample.aws.terraform.application.GlobalConstants;
import com.bapits.labs.sample.aws.terraform.aws.dynamodb.DBProcessor;
import com.bapits.labs.sample.aws.terraform.aws.dynamodb.model.FileTracking;
import com.bapits.labs.sample.aws.terraform.aws.lambda.functions.MyLambdaHandler;
import com.bapits.labs.sample.aws.terraform.aws.s3.S3Processor;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;
import com.bapits.labs.sample.aws.terraform.http.VendorHttpMockServer;
import com.bapits.labs.sample.aws.terraform.utils.FileResourcesUtilsTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

class LambdaHandlerTest extends VendorHttpMockServer {

  private static final Logger logger = LogManager.getLogger(LambdaHandlerTest.class);

  private ConfigProperties configProperties = null;

  private S3Processor mockS3Processor;
  private DBProcessor mockDBProcessor;

  protected String rawResponseContent201 = null;

  @BeforeEach
  public void setup() {
    try {
      configProperties = FileResourcesUtilsTest.getProperties();

      mockS3Processor = mock(S3Processor.class);
      Field instanceS3 = S3Processor.class.getDeclaredField("s3Instance");
      instanceS3.setAccessible(true);
      instanceS3.set(instanceS3, mockS3Processor);

      mockDBProcessor = mock(DBProcessor.class);
      Field instanceDB = DBProcessor.class.getDeclaredField("dbInstance");
      instanceDB.setAccessible(true);
      instanceDB.set(instanceDB, mockDBProcessor);

      File inputFile = FileResourcesUtilsTest.getFileFromResource("zip/test_sample_file.zip");

      Mockito.when(mockS3Processor.readContentFileFromS3(Mockito.anyString(),
          Mockito.eq("test_sample_file.zip"))).thenReturn(new FileInputStream(inputFile));

      Mockito.when(mockS3Processor.putFileOnS3(Mockito.anyString(), Mockito.anyString(),
          Mockito.any(File.class))).thenReturn(true);

      Mockito.when(mockS3Processor.moveFileOnS3(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyString(), Mockito.anyString())).thenReturn(true);


      Mockito.doNothing().when(mockDBProcessor).insertFileTracking(Mockito.any(FileTracking.class));

      // test properties file
      File inputConfigFile =
          FileResourcesUtilsTest.getFileFromResource("test_application.properties");

      Mockito
          .when(mockS3Processor.readContentFileFromS3(Mockito.nullable(String.class),
              Mockito.eq(GlobalConstants.APP_CONFIG_FILE)))
          .thenReturn(new FileInputStream(inputConfigFile));


      // copy input file to other directory just for mocking
      Path copiedInputFile = Files.copy(
          inputFile.toPath(), (new File(System.getProperty("user.dir") + File.separator + "build"
              + File.separator + inputFile.getName())).toPath(),
          StandardCopyOption.REPLACE_EXISTING);

      Mockito.when(mockS3Processor.copyS3FileToLocalDir(Mockito.anyString(), Mockito.anyString(),
          Mockito.anyString())).thenReturn(copiedInputFile.toFile());

      this.setupMockDispatcher();


    } catch (Exception e) {
      logger.error("Some Error Occurred.", e);
      assertTrue("Some Error Occurred.", false);
    }
  }

  private void setupMockDispatcher() {


    // prepare the response and enqueue in the server response
    rawResponseContent201 = FileResourcesUtilsTest.readFile("xml/vendor_server_response_201.xml",
        StandardCharsets.UTF_8);


    Dispatcher dispatcher = new Dispatcher() {
      @Override
      public MockResponse dispatch(RecordedRequest request) {
        String body = request.getBody().readString(Charset.forName("UTF-8"));


        if (body.contains("filename=test_sample_")) {
          return new MockResponse().setBody(rawResponseContent201)
              .addHeader("Content-Type", "application/xml; charset=UTF-8").setResponseCode(201);
        }
        return new MockResponse().setResponseCode(404);
      }
    };

    /*
     * mockBackEnd.enqueue(new MockResponse().setBody(rawResponseContent).addHeader("Content-Type",
     * "application/json; charset=UTF-8"));
     */
    mockBackEnd.setDispatcher(dispatcher);
  }

  @AfterEach
  public void resetSingleton() throws Exception {
    Field instanceS3 = S3Processor.class.getDeclaredField("s3Instance");
    instanceS3.setAccessible(true);
    instanceS3.set(null, null);

    Field instanceDb = DBProcessor.class.getDeclaredField("dbInstance");
    instanceDb.setAccessible(true);
    instanceDb.set(null, null);

    try {
      FileUtils.cleanDirectory(new File(configProperties.appTempDirPath()));
    } catch (Exception e) {
      logger.error("Could not delete directory:{}", configProperties.appTempDirPath(),
          e.getMessage(), e);
    }

  }

  @Test
  void testInjectSQSEvent() {
    try {

      ObjectMapper mapper = new ObjectMapper();
      URL resource = SQSEvent.class.getResource("/json/sqs_event.json");

      SQSEvent sqsEventNotification = mapper.readValue(resource, SQSEvent.class);

      MyLambdaHandler lambdaHandler = new MyLambdaHandler();
      Boolean bResult = lambdaHandler.handleRequest(sqsEventNotification, getFakeContext());

      assertTrue("Lambda Handler Test", bResult);
    } catch (IOException excp) {
      logger.error("Error while processing", excp);
      assertTrue("Some Error Occurred", false);
    }
  }

  private Context getFakeContext() {
    Context ctx = new Context() {

      @Override
      public String getAwsRequestId() {
        return null;
      }

      @Override
      public String getLogGroupName() {
        return null;
      }

      @Override
      public String getLogStreamName() {
        return null;
      }

      @Override
      public String getFunctionName() {
        return null;
      }

      @Override
      public String getFunctionVersion() {
        return null;
      }

      @Override
      public String getInvokedFunctionArn() {
        return null;
      }

      @Override
      public CognitoIdentity getIdentity() {
        return null;
      }

      @Override
      public ClientContext getClientContext() {
        return null;
      }

      @Override
      public int getRemainingTimeInMillis() {
        return 0;
      }

      @Override
      public int getMemoryLimitInMB() {
        return 0;
      }

      @Override
      public LambdaLogger getLogger() {
        return new LambdaLogger() {

          @Override
          public void log(String string) {
            System.out.println(string);

          }

          @Override
          public void log(byte[] message) {}

        };
      }

    };
    return ctx;
  }

}
