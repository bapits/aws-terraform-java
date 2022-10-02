package com.bapits.labs.sample.aws.terraform.http;

import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import okhttp3.mockwebserver.MockWebServer;

public class VendorHttpMockServer {

  private static final int MOCK_SERVER_PORT = 9999;

  public static MockWebServer mockBackEnd;

  @BeforeAll
  static void setUp() throws IOException {
    mockBackEnd = new MockWebServer();
    mockBackEnd.start(MOCK_SERVER_PORT);
  }

  @AfterAll
  static void tearDown() throws IOException {
    mockBackEnd.shutdown();
  }

  @BeforeEach
  public void prepareResourcesForTest() {

  }

}
