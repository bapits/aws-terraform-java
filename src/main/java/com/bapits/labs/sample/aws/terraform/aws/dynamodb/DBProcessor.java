package com.bapits.labs.sample.aws.terraform.aws.dynamodb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.bapits.labs.sample.aws.terraform.aws.dynamodb.model.FileTracking;
import com.bapits.labs.sample.aws.terraform.config.ApplicationProperties;
import com.bapits.labs.sample.aws.terraform.model.FileProcessStatus;



public class DBProcessor {

  private static final Logger logger = LogManager.getLogger(DBProcessor.class);

  private static DBProcessor dbInstance = null;

  AmazonDynamoDB amazonDynamoDB;

  private DBProcessor() {
    amazonDynamoDB = this.buildAmazonDynamoDB();
  }

  // static method to create instance of Singleton class
  public static DBProcessor getInstance() {
    if (dbInstance == null)
      dbInstance = new DBProcessor();
    return dbInstance;
  }

  private AmazonDynamoDB buildAmazonDynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
        .withRegion(
            Regions.valueOf(ApplicationProperties.getInstance().getProperties().awsRegion()))
        .withCredentials(DefaultAWSCredentialsProviderChain.getInstance()).build();
  }

  public void insertFileTracking(FileTracking fileTracking) {
    DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(this.amazonDynamoDB);
    dynamoDBMapper.save(fileTracking);
  }

  /**
   * find the records filter by hashCode
   * 
   * @param fileTrackingHashCode
   * @return FileTracking
   */
  public FileTracking findByFileTrackingHash(String fileTrackingHashCode) {
    return findFileTrackingUniqueRecord("hashCode", fileTrackingHashCode);
  }

  public FileTracking findByFileTrackingHashAndStatus(String fileTrackingHashCode, String status) {

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":val1", new AttributeValue().withS(fileTrackingHashCode));
    expressionAttributeValues.put(":val2", new AttributeValue().withS(status));

    Map<String, String> expressionAttributeNames = new HashMap<>();
    expressionAttributeNames.put("#st", "status");

    String filterExpression = "hashCode = :val1 AND #st = :val2";

    return findFileTrackingUniqueRecord(expressionAttributeValues, expressionAttributeNames,
        filterExpression);
  }

  public List<FileTracking> findFileTrackingsNotStatus(FileProcessStatus fileProcessorStatus) {

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

    expressionAttributeValues.put(":Mystatus",
        new AttributeValue().withS(fileProcessorStatus.toString()));

    Map<String, String> expressionAttributeNames = new HashMap<>();
    expressionAttributeNames.put("#st", "status");

    String filterExpression = "#st <> :Mystatus ";

    return findFileTrackingsRecord(expressionAttributeValues, expressionAttributeNames,
        filterExpression);
  }

  private List<FileTracking> findFileTrackingsRecord(
      Map<String, AttributeValue> expressionAttributeValues,
      Map<String, String> expressionAttributeNames, String filterExpression) {

    logger.debug(
        "Finding FileTracking for expressionAttributeValues:{}, expressionAttributeNames:{}, filterExpression:{}",
        expressionAttributeValues, expressionAttributeNames, filterExpression);

    DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(this.amazonDynamoDB);
    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    scanExpression.withFilterExpression(filterExpression)
        .withExpressionAttributeNames(expressionAttributeNames)
        .withExpressionAttributeValues(expressionAttributeValues);

    PaginatedScanList<FileTracking> paginatedFileTracking =
        dynamoDBMapper.scan(FileTracking.class, scanExpression);
    logger.info("------ count FileTrackings PaginatedScanList ----{}--",
        paginatedFileTracking.size());

    List<FileTracking> fileTrackings = new ArrayList<>(paginatedFileTracking.size());
    Iterator<FileTracking> iterator = paginatedFileTracking.iterator();
    while (iterator.hasNext()) {
      FileTracking element = iterator.next();
      fileTrackings.add(element);
    }
    return fileTrackings;
  }

  public FileTracking findByFileTrackingHashAndNotStatus(String fileTrackingHashCode,
      String status) {

    Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
    expressionAttributeValues.put(":val1", new AttributeValue().withS(fileTrackingHashCode));
    expressionAttributeValues.put(":val2", new AttributeValue().withS(status));

    Map<String, String> expressionAttributeNames = new HashMap<>();
    expressionAttributeNames.put("#st", "status");

    String filterExpression = "hashCode = :val1 AND #st <> :val2";

    return findFileTrackingUniqueRecord(expressionAttributeValues, expressionAttributeNames,
        filterExpression);
  }

  /**
   * find the records filter by hashCode
   * 
   * @param fileTrackingHashCode
   * @return FileTracking
   */
  public FileTracking findByFileTrackingId(Integer fileTrackingId) {
    return findFileTrackingUniqueRecord("id", fileTrackingId.toString());
  }

  private FileTracking findFileTrackingUniqueRecord(String filter, String query) {

    logger.info("Finding FileTracking for:{}, query:{}", filter, query);

    FileTracking fileTracking = null;
    DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(this.amazonDynamoDB);

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    scanExpression.addFilterCondition(filter,
        new Condition().withComparisonOperator(ComparisonOperator.EQ)
            .withAttributeValueList(new AttributeValue().withS(query)));

    PaginatedScanList<FileTracking> paginatedFileTracking =
        dynamoDBMapper.scan(FileTracking.class, scanExpression);
    if (paginatedFileTracking != null && paginatedFileTracking.size() == 1) {// there should be only
      fileTracking = paginatedFileTracking.get(0);
    }

    return fileTracking;
  }

  private FileTracking findFileTrackingUniqueRecord(
      Map<String, AttributeValue> expressionAttributeValues,
      Map<String, String> expressionAttributeNames, String filterExpression) {

    logger.debug(
        "Finding FileTracking for expressionAttributeValues:{}, expressionAttributeNames:{}, filterExpression:{}",
        expressionAttributeValues, expressionAttributeNames, filterExpression);

    FileTracking fileTracking = null;
    DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(this.amazonDynamoDB);

    DynamoDBScanExpression scanExpression = new DynamoDBScanExpression();
    scanExpression.withFilterExpression(filterExpression)
        .withExpressionAttributeNames(expressionAttributeNames)
        .withExpressionAttributeValues(expressionAttributeValues);

    PaginatedScanList<FileTracking> paginatedFileTracking =
        dynamoDBMapper.scan(FileTracking.class, scanExpression);
    logger.debug("FileTracking Query Received Size:{}, query:{}", paginatedFileTracking.size(),
        paginatedFileTracking);

    if (paginatedFileTracking.size() == 1) {// there should be only
      fileTracking = paginatedFileTracking.get(0);
    }

    return fileTracking;
  }

}
