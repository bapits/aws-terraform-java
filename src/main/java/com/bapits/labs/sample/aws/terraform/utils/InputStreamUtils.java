package com.bapits.labs.sample.aws.terraform.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InputStreamUtils {

  private static final Logger logger = LogManager.getLogger(InputStreamUtils.class);

  private InputStreamUtils() {}

  public static InputStream cloneInputStream(InputStream inputStream) {
    InputStream newIStream = null;

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      inputStream.transferTo(baos);
      inputStream = new ByteArrayInputStream(baos.toByteArray());
      newIStream = new ByteArrayInputStream(baos.toByteArray());

    } catch (IOException e) {
      logger.error("Cannot clone InputStream. {}", e.getMessage(), e);
    }

    return newIStream;
  }

  /**
   * After a InputStream is read, it becomes empty Copy the input stream, the passed parameter won't
   * be empty.
   *
   * @param inputStream source of data that will be emptied after reading
   * @param number of clones in the List
   * @return a list of clone InputStream
   */
  public static List<InputStream> cloneInputStream(InputStream inputStream, int number) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      inputStream.transferTo(baos);
    } catch (IOException e) {
      logger.error("Cannot clone InputStream. {}", e.getMessage(), e);
    }
    List<InputStream> list = new LinkedList<>();

    // https://stackoverflow.com/a/5924132/311420
    for (int i = 0; i < number; i++) {
      list.add(new ByteArrayInputStream(baos.toByteArray()));
    }
    return list;
  }

  public static String convert(InputStream inputStream) {
    try {
      return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger.error("Cannot convert InputStream to string. {}", e.getMessage(), e);
    }
    return null;
  }

  public static boolean hasContent(InputStream inputStream) {
    try {
      return inputStream.available() > 0;
    } catch (IOException e) {
      return false;
    }
  }
}
