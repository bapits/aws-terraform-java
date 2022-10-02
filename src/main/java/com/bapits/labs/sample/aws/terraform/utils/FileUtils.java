package com.bapits.labs.sample.aws.terraform.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FileUtils {

  private static final Logger logger = LogManager.getLogger(FileUtils.class);

  public static final int DEFAULT_BUFFER_SIZE = 8192;

  // private constructor to keep this class static
  private FileUtils() {}

  public static String getFileExtensions(String sFileName) {
    int iIndexOf = sFileName.lastIndexOf(".");
    if (iIndexOf > -1) {
      return sFileName.substring(iIndexOf + 1, sFileName.length());
    } else {
      return "";
    }
  }

  public static String getFileNameWithoutExtensions(String sFileName) {
    int iIndexOf = sFileName.lastIndexOf(".");
    if (iIndexOf > -1) {
      return sFileName.substring(0, iIndexOf);
    } else {
      return "";
    }
  }

  /**
   * Create file from zip entry
   *
   * @param destinationDir parent destination directory
   * @param zipEntry zip entry
   */
  public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
    File destFile = new File(destinationDir, zipEntry.getName());
    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();
    if (!destFilePath.startsWith(destDirPath + File.separator)) {
      throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }
    return destFile;
  }

  public static boolean deleteDir(Path dirToDelete) throws IOException {
    try (Stream<Path> walk = Files.walk(dirToDelete)) {
      walk.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    return true;
  }

  public static List<String> findFiles(Path path, String fileExtension) throws IOException {

    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("Path must be a directory!");
    }

    List<String> result;

    try (Stream<Path> walk = Files.walk(path)) {
      result = walk.filter(p -> !Files.isDirectory(p))
          // this is a path, not string,
          .map(p -> p.toString()).filter(f -> f.toLowerCase().endsWith(fileExtension))
          .collect(Collectors.toList());
    }

    return result;
  }

  /**
   * inside the zip file sent by Source find the relevant xml 
   *
   * @param path
   * @param sFileExtension
   * @param filePrefix
   * @return
   * @throws IOException
   */
  public static List<String> findSourceFile(Path path, String sFileExtension, String filePrefix,
      boolean findWithPrefix) throws IOException {

    if (!Files.isDirectory(path)) {
      throw new IllegalArgumentException("Path must be a directory!");
    }

    List<String> result;

    try (Stream<Path> walk = Files.walk(path)) {
      result = walk.filter(p -> !Files.isDirectory(p))
          // this is a path, not string,
          .map(p -> p.toString()).filter(f -> {
            if (f.toLowerCase().endsWith(sFileExtension)) {
              if (findWithPrefix)
                return f.substring(f.lastIndexOf(File.separator) + 1).startsWith(filePrefix);
              else
                return !f.substring(f.lastIndexOf(File.separator) + 1).startsWith(filePrefix);
            } else
              return false;
          }).collect(Collectors.toList());
    }
    return result;
  }

  public static String getFileNameFromPath(String sFilePath) {
    // check if the file is a key(path with slashes) or just a file name
    int ilastIndex = sFilePath.lastIndexOf("/");
    if (ilastIndex != -1) {
      return sFilePath.substring(ilastIndex + 1);
    }
    return sFilePath;
  }

  /**
   * Generate File Hash Code using SHA 256
   *
   * @param file file object to hash
   * @return String
   */
  public static String generateFileHashCodeSHA256(File file) {
    StringBuilder sb = new StringBuilder();

    try (FileInputStream fis = new FileInputStream(file)) {
      MessageDigest shaDigest = MessageDigest.getInstance("SHA-256");

      // Create byte array to read data in chunks
      byte[] byteArray = new byte[1024];
      int bytesCount = 0;

      // Read file data and update in message digest
      while ((bytesCount = fis.read(byteArray)) != -1) {
        shaDigest.update(byteArray, 0, bytesCount);
      }

      // Get the hash's bytes
      // This bytes[] has bytes in decimal format
      byte[] bytes = shaDigest.digest();

      for (int i = 0; i < bytes.length; i++) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
    } catch (FileNotFoundException | NoSuchAlgorithmException e) {
      logger.error(
          "------- Error while generating File Hash Code (FileNotFoundException || NoSuchAlgorithmException) {} : {} -------",
          file.getName(), e.getMessage());
    } catch (IOException e) {
      logger.error("------- Error while generating File Hash Code (IOException) {} : {} -------",
          file.getName(), e.getMessage());
    }

    return sb.toString();
  }

  public static File copyInputStreamToFile(InputStream inputStream, File file) throws IOException {

    // append = false
    try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
      int read;
      byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
      while ((read = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
    }
    return file;

  }

}
