package com.bapits.labs.sample.aws.terraform.aws.s3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.s3.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.bapits.labs.sample.aws.terraform.application.GlobalConstants;
import com.bapits.labs.sample.aws.terraform.utils.FileUtils;

public class S3Processor {

  private static final Logger logger = LogManager.getLogger(S3Processor.class);

  private static final String STATIC_STRING_ERROR_OCCURRED = "Error Occurred:{}";

  private static S3Processor s3Instance = null;

  private AmazonS3 s3Client;

  private S3Processor() {

  }

  // static method to create instance of Singleton class
  public static S3Processor getInstance() {
    if (s3Instance == null)
      s3Instance = new S3Processor();

    return s3Instance;
  }

  /**
   * Copy file from S3 to local directory
   * 
   * @param bucketName
   * @param srcFileName
   * @param sDestinationFileAbsoluteName: local directory absolute path with file name
   * @return
   */
  public File copyS3FileToLocalDir(String bucketName, String srcFileName,
      String sDestinationFileAbsoluteName) {
    logger.info("Connecting with S3 to retrieve files. path<{}/{}>", bucketName, srcFileName);
    InputStream contents = null;
    try {
      // S3 file names are encoded e.g. test 1.xml => test+1.xml
      String decodedFileName = java.net.URLDecoder.decode(srcFileName, "UTF-8");

      // https://stackoverflow.com/a/36463107/311420
      S3Object xFile = getS3Client().getObject(bucketName, decodedFileName);

      logger.info("Files retrieved from S3. path<{}/{}>", xFile.getBucketName(), xFile.getKey());

      contents = xFile.getObjectContent();

      File destFile =
          FileUtils.copyInputStreamToFile(contents, new File(sDestinationFileAbsoluteName));

      logger.info("File successfully copied to<{}>", sDestinationFileAbsoluteName);

      return destFile;
    } catch (SdkClientException | IOException e) {
      logger.error("Cannot get S3 file<{}>", srcFileName, e);
    }

    return null;
  }

  public InputStream readContentFileFromS3(String bucketName, String fileName) {
    logger.info("Connecting with S3 to retrieve files. path<{}/{}>", bucketName, fileName);
    InputStream contents = null;
    try {
      // S3 file names are encoded e.g. test 1.xml => test+1.xml
      String decodedFileName = java.net.URLDecoder.decode(fileName, "UTF-8");

      // https://stackoverflow.com/a/36463107/311420
      S3Object xFile = getS3Client().getObject(bucketName, decodedFileName);

      logger.info("Files retrieved from S3. path<{}/{}>", xFile.getBucketName(), xFile.getKey());

      contents = xFile.getObjectContent();

    } catch (SdkClientException | UnsupportedEncodingException e) {
      logger.error("Cannot get S3 file<{}>", fileName, e);
    }

    return contents;
  }

  public boolean putFileOnS3(String bucketName, String key, File outputFile) {
    try {
      getS3Client().putObject(bucketName, key, outputFile);
      logger.info("File created to path<{}/{}>", bucketName, key);
      return true;
    } catch (Exception excp) {
      logger.error(STATIC_STRING_ERROR_OCCURRED, excp.getMessage(), excp);

    }
    return false;
  }

  private AmazonS3 getS3Client() {
    if (s3Client == null) {
      s3Client = AmazonS3ClientBuilder.standard()
          .withRegion(Regions.valueOf(GlobalConstants.AWS_S3_CONFIG_REGION))
          .withCredentials(DefaultAWSCredentialsProviderChain.getInstance()) // https://stackoverflow.com/a/44079772/311420
          .build();
    }
    return s3Client;
  }

  public boolean copyFileFromS3ToS3(String sourceBucketName, String sourceKey,
      String deistnationBucketName, String deistnationKey) {
    try {
      CopyObjectRequest copyObjRequest =
          new CopyObjectRequest(sourceBucketName, sourceKey, deistnationBucketName, deistnationKey);
      getS3Client().copyObject(copyObjRequest);
      logger.info("copied from path<{}/{}> to path<{}/{}>", sourceBucketName, sourceKey,
          deistnationBucketName, deistnationKey);
      return true;
    } catch (Exception excp) {
      logger.error(STATIC_STRING_ERROR_OCCURRED, excp.getMessage(), excp);

    }
    return false;
  }

  /**
   * Move one file from one bucket to another
   * 
   * @param sourceBucketName
   * @param sourceKey
   * @param deistnationBucketName
   * @param deistnationKey
   * @return
   */
  public boolean moveFileOnS3(String sourceBucketName, String sourceKey,
      String deistnationBucketName, String deistnationKey) {
    try {
      CopyObjectRequest copyObjRequest =
          new CopyObjectRequest(sourceBucketName, sourceKey, deistnationBucketName, deistnationKey);
      getS3Client().copyObject(copyObjRequest);
      logger.info("copied from path<{}/{}> to path<{}/{}>", sourceBucketName, sourceKey,
          deistnationBucketName, deistnationKey);

      getS3Client().deleteObject(new DeleteObjectRequest(sourceBucketName, sourceKey));
      logger.info("deleted from path<{}/{}>", sourceBucketName, sourceKey);

      return true;
    } catch (Exception excp) {
      logger.error(STATIC_STRING_ERROR_OCCURRED, excp.getMessage(), excp);

    }
    return false;
  }

  /**
   * Move All files from one bucket to another
   * 
   * @param sourceBucketName
   * @param sourceKey
   * @param deistnationBucketName
   * @param deistnationKey
   * @return
   */
  public boolean moveAllFilesOnS3(String sourceBucketName, String sourceKey,
      String deistnationBucketName, String deistnationKey) {

    boolean bResult = false;
    try {
      ListObjectsV2Result result = getS3Client().listObjectsV2(sourceBucketName, sourceKey);
      List<S3ObjectSummary> objects = result.getObjectSummaries();

      for (S3ObjectSummary os : objects) {
        String bucketKey = os.getKey();
        logger.info("bucketKey---",  bucketKey);
        CopyObjectRequest copyObjRequest = new CopyObjectRequest(sourceBucketName, bucketKey,
            deistnationBucketName, deistnationKey + "/" + FileUtils.getFileNameFromPath(bucketKey));
        getS3Client().copyObject(copyObjRequest);
        getS3Client().deleteObject(new DeleteObjectRequest(sourceBucketName, bucketKey));
        logger.info("deleted from path<{}/{}>", sourceBucketName, bucketKey);
      }

      bResult = true;
    } catch (Exception e) {
      logger.error(STATIC_STRING_ERROR_OCCURRED, e.getMessage(), e);
    }
    return bResult;
  }

  public List<String> getFileKeysFromS3(String bucketName, String key) {

    List<String> fileKeys = new ArrayList<>();

    ListObjectsV2Request listObjectsV2Request =
        new ListObjectsV2Request().withBucketName(bucketName).withPrefix(key);
    ListObjectsV2Result listObjectsV2Result;

    do {
      listObjectsV2Result = getS3Client().listObjectsV2(listObjectsV2Request);
      listObjectsV2Result.getObjectSummaries().stream().forEach(p -> {
        if (!p.getKey().equals(key)) {
          fileKeys.add(p.getKey());
        }
      });

    } while (listObjectsV2Result.isTruncated());

    return fileKeys;
  }

}
