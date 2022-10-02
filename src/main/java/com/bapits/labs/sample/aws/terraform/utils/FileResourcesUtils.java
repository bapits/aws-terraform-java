package com.bapits.labs.sample.aws.terraform.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class FileResourcesUtils {

  private static final Logger logger = LogManager.getLogger(FileResourcesUtils.class);

  // private constructor not to allow the creation of object.
  private FileResourcesUtils() {}

  public static String readFile(String pathname, Charset utf8) {

    File file = getFileFromResource(pathname);
    StringBuilder fileContents = new StringBuilder((int) file.length());

    try (Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        fileContents.append(scanner.nextLine() + System.lineSeparator());
      }
      return fileContents.toString();
    } catch (FileNotFoundException e) {
      logger.error("file not found! " + e.getMessage());
      return null;
    }
  }

  public static File getFileFromResource(String fileName) {
    File file = null;
    try {
      file = getFileFromResourceFolder(fileName);
    } catch (URISyntaxException exception) {
      logger.error("file not found! " + fileName);
    }
    return file;
  }

  /*
   * The resource URL is not working in the JAR If we try to access a file that is inside a JAR, It
   * throws NoSuchFileException (linux), InvalidPathException (Windows)
   * 
   * Resource URL Sample: file:java-io.jar!/json/file1.json
   */
  private static File getFileFromResourceFolder(String fileName) throws URISyntaxException {

    ClassLoader classLoader = FileResourcesUtils.class.getClassLoader();
    URL resource = classLoader.getResource(fileName);
    if (resource == null) {
      throw new IllegalArgumentException("file not found! " + fileName);
    } else {

      // failed if files have whitespaces or special characters
      // return new File(resource.getFile());

      return new File(resource.toURI());
    }
  }


}
