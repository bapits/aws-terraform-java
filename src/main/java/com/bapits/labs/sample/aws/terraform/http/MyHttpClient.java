package com.bapits.labs.sample.aws.terraform.http;

import static java.util.Map.entry;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.bapits.labs.sample.aws.terraform.application.GlobalConstants;
import com.bapits.labs.sample.aws.terraform.aws.s3.S3Processor;
import com.bapits.labs.sample.aws.terraform.config.ApplicationProperties;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;
import com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData;
import com.bapits.labs.sample.aws.terraform.utils.CertificatesUtils;
import com.bapits.labs.sample.aws.terraform.utils.FileUtils;
import com.bapits.labs.sample.aws.terraform.utils.GeneralUtils;
import com.bapits.labs.sample.aws.terraform.utils.XmlUtils;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MyHttpClient {

  private static final Logger logger = LogManager.getLogger(MyHttpClient.class);

  private static OkHttpClient httpClient;

  private static MyHttpClient myHttpClient = null;

  private static ConfigProperties CONFIG_PROPERTIES;

  private static final String CONTENT_DISPOSITION = "Content-Disposition";
  private static final String ATTACHMENT_FILE_NAME = "attachment; filename=";
  private static final String CONTENT_ID = "Content-ID";

  private static final String CUSTOM_STORE_NAME = "CustomTruststore";


  private MyHttpClient() {

    CONFIG_PROPERTIES = ApplicationProperties.getInstance().getProperties();

    okhttp3.OkHttpClient.Builder okHttpBuilder =
        new okhttp3.OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS);

    // do TLS Setup
    if (CONFIG_PROPERTIES.vendorTLSIsActive()) {
      bSetupTLS(okHttpBuilder);
    }

    httpClient = okHttpBuilder.build();
  }

  // static method to create instance of Singleton class
  public static MyHttpClient getInstance() {
    if (myHttpClient == null)
      myHttpClient = new MyHttpClient();

    return myHttpClient;
  }

  public Response sendPost(File outputFile) throws IOException {

    RequestBody formBody =
        RequestBody.create(outputFile, MediaType.parse("application/x-www-form-urlencoded"));

    Request request = this.buildRequest(formBody);
    return this.postRequest(request);
  }

  public Response sendPost(SampleData targetSampleData) throws IOException {

    okhttp3.MultipartBody.Builder requestBodyBuilder =
        new MultipartBody.Builder().setType(MultipartBody.FORM);

    // get fileName to be inserted in the bondary meta of multipart request
    String tempFileName = FileUtils.getFileNameFromPath("test_sample_");

    // create first multipart

    String xmlFileName = tempFileName + ".xml";

    String invoiceCxmlFile = CONFIG_PROPERTIES.appTempDirPath() + xmlFileName;
    XmlUtils.marshalTargetXmlToFileResource(invoiceCxmlFile, targetSampleData, Arrays.asList(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
        "<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.045/SampleData.dtd\">"));
    RequestBody invoiceBody = RequestBody.create(new File(invoiceCxmlFile),
        MediaType.parse("text/xml; charset=UTF-8; name=" + xmlFileName));

    requestBodyBuilder.addPart(
        Headers.of(Map.ofEntries(entry(CONTENT_DISPOSITION, ATTACHMENT_FILE_NAME + xmlFileName),
            entry(CONTENT_ID, "<testXml>"))),
        invoiceBody);



    // create third multipart

    String pdfFileName = tempFileName + ".xml";
    RequestBody tsInvoicePdfBody = RequestBody.create("NO_XML_TO_ATTACH",
        MediaType.parse("application/xml; name=" + pdfFileName));

    requestBodyBuilder.addPart(
        Headers.of(Map.ofEntries(entry(CONTENT_DISPOSITION, ATTACHMENT_FILE_NAME + pdfFileName),
            entry(CONTENT_ID, "<image>"), entry("Content-Transfer-Encoding", "base64"))),
        tsInvoicePdfBody);


    Request request = buildRequest(requestBodyBuilder.build());

    String sData = GeneralUtils.bodyToString(request);
    logger.info("request body:{}", sData);

    return this.postRequest(request);
  }

  public Response sendPost(Request request) throws IOException {
    return this.postRequest(request);
  }

  private Response postRequest(final Request request) throws IOException {

    Response response = null;
    int retry = 0;

    while (retry++ != CONFIG_PROPERTIES.vendorClientConnectionRetry()) {
      try {
        response = httpClient.newCall(request).execute();
        break;
      } catch (Exception excp) {
        logger.error("Call to vendor failed.{}", excp.getMessage(), excp);
      }
    }

    // throw the exception if the call was not successful
    if (response == null) {
      String errorMsg = "postRequest response is null. Check logs for more details";
      logger.error(errorMsg);
      throw new IOException(errorMsg);
    }

    if (!response.isSuccessful()) {
      String errorMsg = "postRequest Unexpected code " + response + " , body ="
          + response.peekBody(Long.MAX_VALUE).string();
      logger.error(errorMsg);
      throw new IOException(errorMsg);
    }

    return response;
  }

  public Request buildRequest(RequestBody requestBody) {
    String sVendorBaseUrl =
        CONFIG_PROPERTIES.vendorTLSIsActive() ? CONFIG_PROPERTIES.vendorTLSBaseUrl()
            : CONFIG_PROPERTIES.vendorBaseUrl();
    return new Request.Builder().url(sVendorBaseUrl).post(requestBody).build();
  }

  private boolean bSetupTLS(okhttp3.OkHttpClient.Builder okHttpBuilder) {

    boolean bResult = false;

    logger.info("Setting up TLS and adding Certificates");

    logger.debug("javax.net.ssl.trustStore:{}", System.getProperty("javax.net.ssl.trustStore"));
    logger.debug("https.protocols:{}", System.getProperty("https.protocols"));

    try {

      // get jre key store
      String jreKeyStorePath = System.getProperty("java.home")
          + "/lib/security/cacerts".replace('/', File.separatorChar);

      // New location of keystore where certificates will be added. can only save to /tmp from a
      // lambda
      String localCertDir = CONFIG_PROPERTIES.appTempDirPath() + "cert";

      // create directories if does not exists.
      Files.createDirectories(Paths.get(localCertDir));

      String newKeyStorePath = localCertDir + File.separator + CUSTOM_STORE_NAME;

      // copy jre keystore to new location
      Files.copy(Paths.get(jreKeyStorePath), Paths.get(newKeyStorePath),
          StandardCopyOption.REPLACE_EXISTING);

      // get certificates to add from S3 Bucket
      List<String> certificateList = getCertsFromS3(localCertDir);

      // add all certificates to new keystore
      CertificatesUtils.addCertificates(certificateList, newKeyStorePath,
          CONFIG_PROPERTIES.appJREKeysotrePass());

      // add certificate private key to new keystore, new store must have already been created
      CertificatesUtils.importP12Certificate(getPrivateKeyFromS3(localCertDir),
          CONFIG_PROPERTIES.appJREKeysotrePass(), newKeyStorePath,
          CONFIG_PROPERTIES.appJREKeysotrePass());

      // apply custom key manager, as the default key manager is not able to find the correct
      // certificate set by Vendor, which causes handshake_failure
      CertificatesUtils.addCustomKeyManager(okHttpBuilder, newKeyStorePath,
          CONFIG_PROPERTIES.appJREKeysotrePass());

    } catch (Exception e) {
      logger.error("Exception Occurred:{}", e.getMessage(), e);
    }

    System.setProperty("https.protocols", "TLSv1,TLSv1.1,TLSv1.2,TLSv1.3");

    logger.debug("Local Trust Store Path, javax.net.ssl.trustStore:{}",
        System.getProperty("javax.net.ssl.trustStore"));
    logger.debug("Updated https.protocols:{}", System.getProperty("https.protocols"));

    logger.info("New Keystore to use :{}", System.getProperty("javax.net.ssl.keyStore"));

    bResult = true;
    return bResult;
  }

  private List<String> getCertsFromS3(String localCertDir) {
    logger.info("getting Certificate keys from S3 to localDir:{}", localCertDir);

    List<String> s3CertificateList = S3Processor.getInstance().getFileKeysFromS3(
        System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET),
        GlobalConstants.AWS_S3_CONFIG_BUCKET_CERTS);

    logger.info("{} Certificates found to add in keystore.", s3CertificateList.size());

    return this.copyToLocalDir(s3CertificateList, localCertDir);
  }

  private String getPrivateKeyFromS3(String localCertDir) {
    String privateKeyPath = null;
    List<String> s3CertificateList = S3Processor.getInstance().getFileKeysFromS3(
        System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET),
        GlobalConstants.AWS_S3_CONFIG_BUCKET_PRIVATE_KEY);
    if (s3CertificateList.size() != 1) {
      logger.error(
          "SG Certificate PrivateKey Not found. There should be one key, Found {} Keys in path:{}/{}",
          s3CertificateList.size(), System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET),
          GlobalConstants.AWS_S3_CONFIG_BUCKET_PRIVATE_KEY);
    } else {
      privateKeyPath =
          localCertDir + File.separator + FileUtils.getFileNameFromPath(s3CertificateList.get(0));
      S3Processor.getInstance().copyS3FileToLocalDir(
          System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET), s3CertificateList.get(0),
          privateKeyPath);

    }

    return privateKeyPath;
  }

  private List<String> copyToLocalDir(List<String> s3CertificateList, String localDirPath) {
    List<String> certificateList = new ArrayList<>();

    for (String certificate : s3CertificateList) {
      logger.info(localDirPath);
      String localCertFile =
          localDirPath + File.separator + FileUtils.getFileNameFromPath(certificate);
      logger.info("localCertFile:{}", localDirPath);
      S3Processor.getInstance().copyS3FileToLocalDir(
          System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET), certificate, localCertFile);
      certificateList.add(localCertFile);
    }
    return certificateList;
  }



}
