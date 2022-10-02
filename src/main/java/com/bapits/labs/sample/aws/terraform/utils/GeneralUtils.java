package com.bapits.labs.sample.aws.terraform.utils;

import java.io.IOException;
import okhttp3.Request;
import okio.Buffer;

public class GeneralUtils {

  // private constructor to keep this class static
  private GeneralUtils() {}

  public static String bodyToString(final Request request) {

    try {
      final Request copy = request.newBuilder().build();
      final Buffer buffer = new Buffer();
      copy.body().writeTo(buffer);
      return buffer.readUtf8();
    } catch (final IOException e) {
      return "did not work";
    }
  }
}
