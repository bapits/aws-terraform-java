package com.bapits.labs.sample.aws.terraform.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.bapits.labs.sample.aws.terraform.application.GlobalConstants;
import com.bapits.labs.sample.aws.terraform.aws.dynamodb.DBProcessor;
import com.bapits.labs.sample.aws.terraform.aws.dynamodb.model.FileTracking;
import com.bapits.labs.sample.aws.terraform.aws.s3.S3Processor;
import com.bapits.labs.sample.aws.terraform.config.ApplicationProperties;
import com.bapits.labs.sample.aws.terraform.config.ConfigProperties;
import com.bapits.labs.sample.aws.terraform.http.MyHttpClient;
import com.bapits.labs.sample.aws.terraform.mapper.XmlSrcToDestMapper;
import com.bapits.labs.sample.aws.terraform.model.FileProcessStatus;
import com.bapits.labs.sample.aws.terraform.model.ReceivedFrom;
import com.bapits.labs.sample.aws.terraform.model.source.sampledata.SampleData;
import com.bapits.labs.sample.aws.terraform.utils.FileUtils;
import com.bapits.labs.sample.aws.terraform.utils.XmlUtils;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.uuid.Generators;
import okhttp3.Response;

/**
 * Service to process the Xml files recieved from Source. Map and transform the Xml to the required
 * Target Xml format.
 */
public class MapperService {

  private static final Logger logger = LogManager.getLogger(MapperService.class);

  private XmlSrcToDestMapper srcToTargetMapper;

  ConfigProperties configProperties = null;

  static {
    try {
      // load the properties from S3.
      logger.info("Loading properties from: <{}/{}>",
          System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET_SAMPLE),
          GlobalConstants.APP_CONFIG_FILE);
      InputStream inputStream = S3Processor.getInstance().readContentFileFromS3(
          System.getenv(GlobalConstants.AWS_S3_CONFIG_BUCKET_SAMPLE),
          GlobalConstants.APP_CONFIG_FILE);
      ApplicationProperties.getInstance().loadProperties(inputStream);
      logger.info("Properties file loaded successfully.");
    } catch (Exception e) {
      logger.error("Properties File not loaded.", e);
    }

  }


  public MapperService() {
    configProperties = ApplicationProperties.getInstance().getProperties();

    srcToTargetMapper = new XmlSrcToDestMapper();

  }

  /*
   * process File received in the S3 Bucket
   */
  public boolean processS3File(String sBucketName, String sFileName, Context lambdaContext) {

    boolean bResult = false;
    logger.info("Preprocessing File<{}>", sFileName);

    FileTracking fileTracking =
        this.insertNewFileTracking(sFileName, lambdaContext.getLogStreamName());

    boolean isSource1File = false;
    if (sFileName.startsWith(configProperties.awsS3BucketSource1In())) {
      isSource1File = true;
      fileTracking.setReceivedFrom(ReceivedFrom.SOURCE1.toString());
    } else {
      logger.error("File<{}> is not correct. File is not from {}.sFileName:{}",
          ReceivedFrom.SOURCE1, sFileName);
      return bResult;
    }

    // copy input file from S3 to lambda local directory
    File inputFile = S3Processor.getInstance().copyS3FileToLocalDir(sBucketName, sFileName,
        (configProperties.appTempDirPath() + FileUtils.getFileNameFromPath(sFileName)));

    if (inputFile == null || !inputFile.exists()) {
      logger.error("Could not copy file<{}> from<{}>", inputFile, sFileName);
      return bResult;
    }

    // archive the file to S3 with in archive path with year and month.
    int year = LocalDate.now().getYear();
    int month = LocalDate.now().getMonthValue();
    int day = LocalDate.now().getDayOfMonth();
    String sArchiveDestinationFilePath = configProperties.awsS3BucketTargetFilesArchiveIn()
        + File.separator + year + File.separator + month + File.separator + day + File.separator;

    String sArchiveDestinationFileName = FileUtils.getFileNameFromPath(sFileName);

    // check if the fileTracking has already been processed, mark it as duplicated
    String sHashCode = FileUtils.generateFileHashCodeSHA256(inputFile);
    fileTracking.setHashCode(sHashCode);

    FileTracking fileTrackingWithHash =
        DBProcessor.getInstance().findByFileTrackingHashAndNotStatus(sHashCode,
            FileProcessStatus.ALREADY_PROCESSED.toString());
    if (fileTrackingWithHash != null) {
      logger.info("FileTracking<{}> already processed. Marking as ALREADY_PROCESSED.", sFileName);
      fileTracking.setStatus(FileProcessStatus.ALREADY_PROCESSED.toString());
      DBProcessor.getInstance().insertFileTracking(fileTracking);
      // move duplicated fileTracking to archive/<date>/duplicated
      this.moveS3File(sBucketName, sFileName, sArchiveDestinationFilePath + "ALREADY_PROCESSED"
          + File.separator + sArchiveDestinationFileName);
      return true;
    }

    // input file is ready to be processed
    com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData targetSampleData = null;
    if (isSource1File) { // Source1 File Handling
      targetSampleData = this.processZipFile(inputFile);
    }

    if (targetSampleData != null) {

      // send the target file to vendor
      if (configProperties.sendToVendorIsActive()) {
        if (this.postToVendor(targetSampleData)) {
          fileTracking.setStatus(FileProcessStatus.TRANSFERRED.toString());
          DBProcessor.getInstance().insertFileTracking(fileTracking);

          // move the input file to S3 Archive
          this.moveS3File(sBucketName, sFileName,
              sArchiveDestinationFilePath + sArchiveDestinationFileName);
          bResult = true;
        } else {
          logger.info("Failed to transfer File to Vendor.");
          fileTracking.setStatus(FileProcessStatus.ALREADY_PROCESSED.toString());
          DBProcessor.getInstance().insertFileTracking(fileTracking);
        }

      } else {
        logger.info("File Transfer to Vendor is not active.");
        fileTracking.setStatus(FileProcessStatus.PROCESSED.toString());
        DBProcessor.getInstance().insertFileTracking(fileTracking);
      }



      String targetFileName = FileUtils.getFileNameFromPath(this.getOutputFileName(sFileName));
      String outFile = configProperties.appTempDirPath() + targetFileName;

      XmlUtils.marshalTargetXmlToFileResource(outFile, targetSampleData, Arrays.asList(
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
          "<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.045/testSample.dtd\">"));

      // create target output file in S3
      if (!S3Processor.getInstance().putFileOnS3(configProperties.awsS3BucketTarget(),
          configProperties.awsS3BucketTargetFilesOut() + File.separator + targetFileName,
          new File(outFile))) {
        logger.warn("Unable to create <{}> in S3 bucket<{}>", targetFileName,
            ApplicationProperties.getInstance().getProperties().awsS3BucketTargetFilesOut());
      }
      logger.info("Cxml File successfully created.");

    } else {
      logger.error("Input file is not correct. Check the logs for more details.");
    }
    return bResult;
  }

  /**
   * insert new fileTracking in the database
   * 
   * @return
   */
  private FileTracking insertNewFileTracking(String sFileName, String logStreamName) {
    FileTracking fileTracking = new FileTracking();

    UUID uuid = Generators.timeBasedGenerator().generate();

    fileTracking.setId(uuid.toString());
    fileTracking.setRowCreationDate(Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
    fileTracking.setStatus(FileProcessStatus.COLLECTED.toString());
    fileTracking.setOriginName(FileUtils.getFileNameFromPath(sFileName));
    fileTracking.setLogStreamName(logStreamName);

    DBProcessor.getInstance().insertFileTracking(fileTracking);
    return fileTracking;
  }

  /**
   * Move the input file to archived directory
   * 
   * @param sBucketName
   * @param sFileName
   * @param sFileDestinationPath
   */
  private void moveS3File(String sBucketName, String sFileName, String sFileDestinationPath) {
    if (!S3Processor.getInstance().moveFileOnS3(sBucketName, sFileName,
        configProperties.awsS3BucketTarget(), sFileDestinationPath)) {
      logger.warn("Unable to move <{}> in S3 bucket<{}>", sFileName,
          configProperties.awsS3BucketTarget());
    }
  }

  /**
   * Get Output Xml file name for the platform, which will go to Vendor
   * 
   * @param fileName
   * @return
   */
  private String getOutputFileName(String fileName) {
    return FileUtils.getFileNameWithoutExtensions(fileName) + "."
        + configProperties.vendorOutputFileExtension();
  }

  public boolean postToVendor(
      com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData targetSampleData) {

    boolean bResult = false;
    try {
      logger.info("Sending File to Vendor");
      Response response = MyHttpClient.getInstance().sendPost(targetSampleData);
      String sResponse = response.body().string();

      XmlMapper xmlMapper = new XmlMapper();

      com.bapits.labs.sample.aws.terraform.model.vendor.ResponseRoot value = xmlMapper.readValue(
          sResponse, com.bapits.labs.sample.aws.terraform.model.vendor.ResponseRoot.class);

      if (value != null && value.getResponse() != null && value.getResponse().getStatus() != null) {
        com.bapits.labs.sample.aws.terraform.model.vendor.Status respStatus =
            value.getResponse().getStatus();
        if (respStatus.getCode() != null && respStatus.getCode().startsWith("20")) {// response code
                                                                                    // is 200/201
          logger.info("File successfully sent to Vendor. Response {}", sResponse);
          bResult = true;
        } else {
          logger.error("There was an error from Vendor, errorCode:{}, Response {}",
              respStatus.getCode(), sResponse);
        }

      }

    } catch (Exception e) {
      logger.error("Exception while posting File to Vendor. {}", e.getMessage(), e);
    }

    return bResult;

  }

  /*
   * transform Source Xml File to Destination Xml File create new file at outputFilePath and write
   * output
   */
  public com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData processXMLFile(
      File fileXML) {

    logger.info("Processing {} file.", fileXML.getName());
    com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData targetSampleData = null;
    try {
      // use the root class with any of the root element
      Unmarshaller jaxbUnmarshaller =
          JAXBContext.newInstance(SampleData.class).createUnmarshaller();
      Object obj = jaxbUnmarshaller.unmarshal(fileXML);

      if (obj != null) {

        logger.info("{} File is of type Source Sample Data.", fileXML.getName());
        SampleData srcSampleData = (SampleData) obj;


        // map Source Sample to Target Sample
        targetSampleData = srcToTargetMapper.mapSourceToDestination(srcSampleData);

      }
    } catch (JAXBException e) {
      logger.error("Error Processing {} File exception. {}", fileXML.getName(), e.getMessage(), e);
    }

    logger.info("Finished processing {} file.", fileXML.getName());

    return targetSampleData;
  }



  /*
   * transform source Xml File recieved in zip format to destination Xml File
   */
  public com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData processZipFile(
      File zipFile) {

    logger.info("Processing Source Zip File {}", zipFile.getName());
    com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData targetSampleData = null;

    try {
      Path unzipDirPath = this.unzipFiles(zipFile);
      if (unzipDirPath != null) {
        List<String> xmlFiles = FileUtils.findSourceFile(unzipDirPath, "xml",
            configProperties.appSourceReceivedFileXmlPrefix(), false);

        if (xmlFiles.size() != 1) {
          logger.error("There should be only one xml file inside the zip file. Files found:{}",
              xmlFiles.size());
        } else {
          logger.info("Reading Xml file {}", xmlFiles.get(0));

          File gbXmlFile = new File(xmlFiles.get(0));

          targetSampleData = this.processXMLFile(gbXmlFile);

        }
      } else {
        logger.error("Unzipped directory not found for file<{}>.", zipFile.getName());
      }
    } catch (IOException e) {
      logger.error("Error while processing File.", e);
    }

    return targetSampleData;
  }

  /**
   * Unzip the files received from GB. The zip file will be composed of 2 files, i.e. pdf file, and
   * its corresponding xml file
   * 
   * @param fileXMLStream
   * @param sZipFileName
   * @return
   */
  private Path unzipFiles(File fileZip) {
    logger.info("------- Unzipping file: {} -------", fileZip.getAbsolutePath());

    String sUnzipDirPath = configProperties.appTempDirPath() + File.separator
        + FileUtils.getFileNameWithoutExtensions(fileZip.getName());
    try {
      Path createdDirPath = Files.createDirectories(Paths.get(sUnzipDirPath));
      byte[] buffer = new byte[1024];
      try (ZipInputStream zIS = new ZipInputStream(new FileInputStream(fileZip))) {
        ZipEntry zipEntry = zIS.getNextEntry();
        while (zipEntry != null) {
          File newFile = FileUtils.newFile(new File(sUnzipDirPath), zipEntry);
          try (FileOutputStream fos = new FileOutputStream(newFile)) {
            int len;
            while ((len = zIS.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
          zipEntry = zIS.getNextEntry();
        }
        zIS.closeEntry();
        logger.info("------- Files unzipped to {} -------", createdDirPath.toAbsolutePath());

        return createdDirPath;
      } catch (Exception excp) {
        logger.error("Error while unzipping files.", excp);
      }
    } catch (Exception excp) {
      logger.error("------- Cannot create directory:{} to unzip for file:{}. -------",
          sUnzipDirPath, fileZip.getAbsolutePath());
    }
    return null;
  }

  private void updateFileTrackingStatus(FileTracking fileTracking, String statusType) {
    fileTracking.setStatus(statusType);
    DBProcessor.getInstance().insertFileTracking(fileTracking);
  }

}
