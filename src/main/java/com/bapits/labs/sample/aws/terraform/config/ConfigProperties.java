package com.bapits.labs.sample.aws.terraform.config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.Config.LoadPolicy;
import org.aeonbits.owner.Config.LoadType;

@LoadPolicy(LoadType.MERGE)
public interface ConfigProperties extends Config {

  @Key("app.temp.dir")
  String appTempDirPath();

  @Key("app.jre.keystore.pass")
  String appJREKeysotrePass();

  @Key("app.source.received.file.xml.prefix")
  String appSourceReceivedFileXmlPrefix();

  // vendor connection details
  @Key("send.to.vendor.is.active")
  Boolean sendToVendorIsActive();

  @Key("vendor.base_url")
  String vendorBaseUrl();

  @Key("vendor.tls.is.active")
  Boolean vendorTLSIsActive();

  @Key("vendor.tls.base_url")
  String vendorTLSBaseUrl();

  @Key("vendor.connection.timeout")
  int vendorClientConnectionTimeout();

  // number of times to retry if there is a problem for connection with Vendor
  @Key("vendor.client.connection.retry")
  int vendorClientConnectionRetry();

  @Key("vendor.output.file.extension")
  String vendorOutputFileExtension();

  // configuration for aws
  @Key("aws.s3.bucket.source1.in")
  String awsS3BucketSource1In();

  @Key("aws.s3.bucket.target")
  String awsS3BucketTarget();

  @Key("aws.s3.bucket.target.files.out")
  String awsS3BucketTargetFilesOut();

  @Key("aws.s3.bucket.target.files.archive.in")
  String awsS3BucketTargetFilesArchiveIn();

  @Key("aws.region")
  String awsRegion();

}
