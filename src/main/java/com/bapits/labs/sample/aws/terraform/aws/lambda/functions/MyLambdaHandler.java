package com.bapits.labs.sample.aws.terraform.aws.lambda.functions;

import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;
import com.bapits.labs.sample.aws.terraform.aws.lambda.model.Body;
import com.bapits.labs.sample.aws.terraform.aws.lambda.model.Record;
import com.bapits.labs.sample.aws.terraform.service.MapperService;
import com.bapits.labs.sample.aws.terraform.service.MyOtherService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MyLambdaHandler implements RequestHandler<SQSEvent, Boolean> {

  private static final Logger logger = LogManager.getLogger(MyLambdaHandler.class);

  private MapperService mapperService = new MapperService();
  private MyOtherService myOtherService = new MyOtherService();

  /**
   * Handle Events received from SQS
   */
  @Override
  public Boolean handleRequest(SQSEvent event, Context context) {

    boolean bResult = false;

    logger.info("Input SQSEvent = {}", event.toString());
    long startTime = System.currentTimeMillis();
    logger.info("------ File Processing Request Started: {} ------",
        getCurrentTimeDisplay(startTime));

    if (event.getRecords() == null) {
      logger.info("------ My Other Service Request from ------");
      try {
        bResult = myOtherService.processS3File();

      } catch (Exception e) {
        logger.error("------ exception ------", e);
      }

    } else {
      logger.info("------ File Handle Request ------");
      try {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        // iterate all the Messages received form SQS
        for (SQSMessage msg : event.getRecords()) {
          logger.info("Processing Message with id <{}>, Message Content:{}", msg.getMessageId(),
              msg);

          // FIXME: temporary solution to remove the string quotes.
          String sValue = msg.getBody().replace("\"{", "{").replace("}\"", "}");
          sValue = sValue.replace("\\\"", "\"");
          logger.info("Body after double quotes replacement : {}", sValue);

          Body messageBody = objectMapper.readValue(sValue, Body.class);

          if (messageBody == null || messageBody.getMessage() == null
              || messageBody.getMessage().getRecords() == null
              || messageBody.getMessage().getRecords().isEmpty()) {
            logger.error("Not a valid Notification Message. Check the logs for more details");
          } else {

            // iterate all the records in the message
            for (Record bodyRecord : messageBody.getMessage().getRecords()) {

              if (bodyRecord.getS3() == null || bodyRecord.getS3().getBucket() == null) {
                logger.error("Not a valid Message Body Recprd. Check the logs for more details");
              } else {

                // read the new incoming file
                String sBucketName =
                    messageBody.getMessage().getRecords().get(0).getS3().getBucket().getName();
                String sFileName =
                    messageBody.getMessage().getRecords().get(0).getS3().getS3Object().getKey();

                logger.info("s3Filename keyName = {}, bucket = {}", sFileName, sBucketName);

                bResult = mapperService.processS3File(sBucketName, sFileName, context);
              }

              if (bResult) {
                logger.info("------ File Successfully Processed ------");
              } else {
                logger.info("------ Error while Processing File ------");
              }
            }
          }
        }
      } catch (Exception excp) {
        logger.error("------ Exception occurred. ------", excp);
      }
    }
    long diff = System.currentTimeMillis() - startTime;
    logger.info("------ File Processing Request Finished. ended at:{}, timetaken:{} ------",
        getCurrentTimeDisplay(System.currentTimeMillis()), diff);

    return bResult;
  }


  private String getCurrentTimeDisplay(long currentTimeMillis) {
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss.SSS z");
    Date date = new Date(currentTimeMillis);
    return formatter.format(date);
  }

}
