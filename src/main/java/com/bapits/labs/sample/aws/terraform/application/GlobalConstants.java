package com.bapits.labs.sample.aws.terraform.application;


public final class GlobalConstants {

  /**
   * Do not apply style format on this file, or it will be less readable.
   */

  private GlobalConstants() {}

  // these configurations are for properties file, the actual value is obtained from the enviornment
  // variable of the lambda
  public static final String AWS_S3_CONFIG_BUCKET = "AWS_S3_CONFIG_BUCKET";
  public static final String AWS_S3_CONFIG_BUCKET_SAMPLE = "AWS_S3_CONFIG_BUCKET_SAMPLE";
  public static final String APP_CONFIG_FILE = "application.properties";
  public static final String AWS_S3_CONFIG_REGION = "EU_WEST_1";

  public static final String AWS_S3_CONFIG_BUCKET_CERTS = "common/certs/";
  public static final String AWS_S3_CONFIG_BUCKET_PRIVATE_KEY = "common/pk/";


}
