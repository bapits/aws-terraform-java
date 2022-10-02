package com.bapits.labs.sample.aws.terraform.utils;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.bapits.labs.sample.aws.terraform.model.target.sampledata.SampleData;
import com.bapits.labs.sample.aws.terraform.service.MapperService;

public class XmlUtils {

  private static final Logger logger = LogManager.getLogger(XmlUtils.class);

  // private constructor to keep this class static
  private XmlUtils() {}

  public static SampleData unmarshalFileResourceToTargetXml(String resourcePath) {

    URL fileResourceUrl = MapperService.class.getResource(resourcePath);

    // Disable XXE
    SAXParserFactory spf = SAXParserFactory.newInstance();
    try {
      spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      spf.setFeature("http://xml.org/sax/features/validation", false);

      // unmarshall operation
      Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(),
          new InputSource(new FileReader(fileResourceUrl.getPath())));

      Unmarshaller jaxbUnmarshaller =
          JAXBContext.newInstance(SampleData.class).createUnmarshaller();

      return (SampleData) jaxbUnmarshaller.unmarshal(xmlSource);

    } catch (ParserConfigurationException | SAXException | JAXBException
        | FileNotFoundException e) {
      logger.error("Error while unmarshalling file{}", resourcePath, e);
    }
    return null;
  }

  public static boolean marshalTargetXmlToFileResource(String outputFilePath, SampleData targetXML,
      List<String> header) {

    boolean bResult = false;
    try {

      // creating the JAXB context
      JAXBContext jContext = JAXBContext.newInstance(SampleData.class);
      // creating the marshaller object
      Marshaller marshallObj = jContext.createMarshaller();
      // setting the property to show xml format output
      marshallObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      marshallObj.setProperty("com.sun.xml.bind.xmlDeclaration", false);
      if (header != null && header.size() > 0) {
        marshallObj.setProperty("com.sun.xml.bind.xmlHeaders",
            header.stream().map(Object::toString).collect(Collectors.joining(" ")));
      }
      FileOutputStream fos = new FileOutputStream(outputFilePath);

      // calling the marshall method
      marshallObj.marshal(targetXML, fos);

      bResult = true;
    } catch (Exception e) {
      logger.error("Error while marshalling object to file {}", outputFilePath, e);
    }
    return bResult;
  }
}
