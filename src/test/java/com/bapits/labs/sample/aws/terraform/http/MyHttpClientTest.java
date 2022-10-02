package com.bapits.labs.sample.aws.terraform.http;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.bapits.labs.sample.aws.terraform.application.GlobalConstants;
import com.bapits.labs.sample.aws.terraform.aws.s3.S3Processor;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;
import com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData;
import com.bapits.labs.sample.aws.terraform.utils.FileResourcesUtilsTest;
import com.bapits.labs.sample.aws.terraform.utils.XmlUtils;
import okhttp3.Response;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;

class MyHttpClientTest extends VendorHttpMockServer {

  private static final Logger logger = LogManager.getLogger(MyHttpClientTest.class);

  private ConfigProperties configProperties = null;

  protected String rawResponseContent201 = null;
  protected String rawResponseContent401 = null;

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
      File inputConfigFile =
          FileResourcesUtilsTest.getFileFromResource("test_application.properties");

      Mockito
          .when(mockS3Processor.readContentFileFromS3(Mockito.nullable(String.class),
              Mockito.eq(GlobalConstants.APP_CONFIG_FILE)))
          .thenReturn(new FileInputStream(inputConfigFile));

      this.setupMockDispatcher();

    } catch (Exception excp) {
      logger.error("Error Occurred:{}", excp.getMessage(), excp);
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

        if (body.contains("filename=test_sample")) {
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
    Field instanceMyHttpClient = MyHttpClient.class.getDeclaredField("myHttpClient");
    instanceMyHttpClient.setAccessible(true);
    instanceMyHttpClient.set(null, null);
  }

  @Test
  void given_correct_request_THEN_response_should_be_201() throws Exception {

    try {
      // input file request to be sent to server
      URL targetSampleUrl = MyHttpClientTest.class.getResource("/xml/test_sample_out.xml");

      Unmarshaller jaxbUnmarshaller =
          JAXBContext.newInstance(SampleData.class).createUnmarshaller();

      SampleData targetSampleData = (SampleData) jaxbUnmarshaller.unmarshal(targetSampleUrl);

      Response response = MyHttpClient.getInstance().sendPost(targetSampleData);

      assertEquals(201, response.code());
      assertEquals(rawResponseContent201, response.peekBody(Long.MAX_VALUE).string());

    } catch (Exception excp) {
      logger.error("Some Error", excp);
      assertTrue(false);
    }

  }

}
