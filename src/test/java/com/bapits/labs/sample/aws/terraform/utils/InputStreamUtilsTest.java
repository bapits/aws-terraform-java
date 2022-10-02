package com.bapits.labs.sample.aws.terraform.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputStreamUtilsTest {

  @Test
  void testCloneInputStream() throws IOException {
    File inputFile = FileResourcesUtilsTest.getFileFromResource("xml/test_sample_in.xml");
    InputStream targetStream = new FileInputStream(inputFile);

    // before reading, the input stream can be read
    assertTrue(InputStreamUtils.hasContent(targetStream));

    List<InputStream> listOfClonedInputStream = InputStreamUtils.cloneInputStream(targetStream, 2);

    // once read, the input has no more content
    assertFalse(InputStreamUtils.hasContent(targetStream));

    String expectedResult = InputStreamUtils.convert(listOfClonedInputStream.get(0));

    for (int i = 1; i < listOfClonedInputStream.size(); i++) {
      assertEquals(expectedResult, InputStreamUtils.convert(listOfClonedInputStream.get(i)));
      assertFalse(InputStreamUtils.hasContent(listOfClonedInputStream.get(i)));
    }
  }

}
