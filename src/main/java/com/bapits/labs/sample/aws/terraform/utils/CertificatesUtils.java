package com.bapits.labs.sample.aws.terraform.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import okhttp3.OkHttpClient.Builder;

public class CertificatesUtils {

  private static final Logger logger = LogManager.getLogger(CertificatesUtils.class);

  private CertificatesUtils() {}

  // import private key pkcs12
  public static void importP12Certificate(String filePathP12, String passwordP12,
      String filePathJks, String passwordJKS) throws Exception {

    KeyStore p12Store = loadKeystore(filePathP12, passwordP12, "PKCS12");

    KeyStore jksStore = loadKeystore(filePathJks, passwordJKS, "jks");

    Enumeration aliases = p12Store.aliases();

    while (aliases.hasMoreElements()) {

      String alias = (String) aliases.nextElement();

      if (p12Store.isKeyEntry(alias)) {
        logger.info("Adding private key of alias:{} to keystore", alias);
        Key key = p12Store.getKey(alias, passwordP12.toCharArray());

        Certificate[] chain = p12Store.getCertificateChain(alias);

        jksStore.setKeyEntry(alias, key, passwordJKS.toCharArray(), chain);
      }
    }

    storeKeystore(jksStore, filePathJks, passwordJKS);

  }

  public static KeyStore loadKeystore(String keyStoreFile, String keystorePassword,
      String keystoreType)
      throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {

    KeyStore keystore = null;

    File file = new File(keyStoreFile);
    try (InputStream inputStream = new FileInputStream(file)) {
      if (keystoreType == null) {
        keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      } else {
        keystore = KeyStore.getInstance(keystoreType);
      }
      keystore.load(inputStream, keystorePassword != null ? keystorePassword.toCharArray() : null);
    }

    return keystore;
  }

  public static void storeKeystore(KeyStore keystore, String keyStoreFile, String keystorePassword)
      throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {

    File file = new File(keyStoreFile);
    FileOutputStream out = new FileOutputStream(file);
    keystore.store(out, keystorePassword != null ? keystorePassword.toCharArray() : null);
    out.close();
  }

  /**
   * Add certificate to the keystore
   * 
   * @param certName
   * @param keyStore
   * @param certPath
   * @param certEntry
   * @return
   * @throws CertificateException
   * @throws KeyStoreException
   * @throws NoSuchAlgorithmException
   * @throws IOException
   */
  public static boolean addCertificate(String newCertificatePath, String certAlias,
      String newStorePath, String keyStorePass)
      throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException {

    boolean bResult = false;

    // Input stream to cert file
    File fileCertificate = new File(newCertificatePath);
    if (fileCertificate.exists()) {
      logger.info("Loading certificate:{} to path:{}", newCertificatePath, newStorePath);
      try (InputStream targetStreamCert = new FileInputStream(fileCertificate)) {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate caCert = cf.generateCertificate(targetStreamCert);

        KeyStore keyStore = CertificatesUtils.loadKeystore(newStorePath, keyStorePass, "jks");
        keyStore.setCertificateEntry(certAlias, caCert);

        try (FileOutputStream out = new FileOutputStream(newStorePath)) {
          keyStore.store(out, keyStorePass.toCharArray());
        }

        bResult = true;
      }
    } else {
      logger.info("certificate File:{} not found", newCertificatePath);
    }

    return bResult;

  }

  /**
   * Copy the JRE original keystore, and add the certificate in the new keystore. Then update the
   * keystore path so from now on the JRE will use the new keystore to connect with SG Connect and
   * Concur.
   * 
   * @param certificateList
   * @return
   */
  public static boolean addCertificates(List<String> certificatesList, String newKeyStorePath,
      String certPass) {
    boolean bResult = false;
    try {

      logger.info("Adding Certificates to local KeyStore: {}", Paths.get(newKeyStorePath));

      KeyStore newKeyStore = CertificatesUtils.loadKeystore(newKeyStorePath, certPass, "jks");

      int keyStoreSize = newKeyStore.size();
      logger.debug("Keystore size before adding: {}", keyStoreSize);
      CertificatesUtils.printCertificateAlias(newKeyStore);

      int certificatesSuccessfullyAdded = 0;
      for (String sCertificateToAdd : certificatesList) {
        bResult = CertificatesUtils.addCertificate(sCertificateToAdd,
            sCertificateToAdd.replace(".cer", ""), newKeyStorePath, certPass);
        if (!bResult) {
          logger.error("Error while adding certificate:{}", sCertificateToAdd);
          break;
        }
        certificatesSuccessfullyAdded++;
      }

      // if new store is created successfully, update the path of truststore so that jre could
      // start using the new store.
      System.setProperty("javax.net.ssl.trustStore", newKeyStorePath);
      System.setProperty("javax.net.ssl.trustStorePassword", certPass);
      System.setProperty("javax.net.ssl.keyStore", newKeyStorePath);
      System.setProperty("javax.net.ssl.keyStorePassword", certPass);

      // get latest reference to store to get the correct size
      KeyStore newKeyStore1 = CertificatesUtils.loadKeystore(newKeyStorePath, certPass, "jks");

      logger.info("{} Certificates of total:{} successfully added. Keystore size old:{}, new:{}",
          certificatesSuccessfullyAdded, certificatesList.size(), keyStoreSize,
          newKeyStore1.size());

      if (logger.isDebugEnabled()) {
        CertificatesUtils.printCertificateAlias(newKeyStore);
      }

    } catch (Exception e) {
      logger.error("Error loading certificate:{}", e.getMessage(), e);
    }

    return bResult;
  }

  /**
   * Print the alias of the certificates
   * 
   * @param keystore
   * @throws KeyStoreException
   */
  public static void printCertificateAlias(KeyStore keystore) throws KeyStoreException {

    List<String> sList = new ArrayList<>();

    Enumeration<String> enumeration = keystore.aliases();
    while (enumeration.hasMoreElements()) {
      String alias = enumeration.nextElement();
      sList.add(alias);
    }

    logger.debug("certificate of alias list: {}",
        (!sList.isEmpty()) ? Arrays.toString(sList.toArray()) : "");
  }

  /**
   * Ignore the SSL certificates check. this should be used only for testing purpose.
   * 
   * @param httpBuilder
   */
  public static void ignoreCertificates(Builder httpBuilder) {
    logger.info(
        "SSL is disabled, connection with the client will be without ssl certificate validation.");
    TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {

      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        logger.debug("Check Client Trusted values: authType:{}", authType);
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
        logger.debug("Checking Server Trusted: authType:{}", authType);
      }

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        logger.debug("Using empty Certificate list.");
        return new java.security.cert.X509Certificate[] {};
      }
    }};
    try {
      SSLContext sslContext = SSLContext.getInstance("SSL");

      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      httpBuilder.sslSocketFactory(sslContext.getSocketFactory(),
          (X509TrustManager) trustAllCerts[0]);
      httpBuilder.hostnameVerifier(new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
          logger.debug("Verify HostName:{} ", hostname);
          return true;
        }
      });

    } catch (Exception e) {
      logger.error("Error while disabling ssl. ", e.getMessage(), e);
    }
  }

  public static void addCustomKeyManager(Builder httpBuilder, String keyStorePath,
      String keyStorePass) {
    try {

      KeyStore st = CertificatesUtils.loadKeystore(keyStorePath, keyStorePass, "PKCS12");

      KeyManagerFactory kf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kf.init(st, keyStorePass.toCharArray());

      KeyManager[] km = kf.getKeyManagers();

      TrustManagerFactory tf =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tf.init(st);
      TrustManager[] tm = tf.getTrustManagers();

      X509ExtendedKeyManager orig = (X509ExtendedKeyManager) km[0]; // exception if wrong type
      km[0] = new X509ExtendedKeyManager() {

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
          logger.debug("My chooseClientAlias Called");
          return orig.chooseClientAlias(keyType, null, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
          logger.debug("My getCertificateChain Called");
          return orig.getCertificateChain(alias);
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
          logger.debug("My getClientAliases Called");
          // shouldn't actually be used AFAICT but just in case
          return orig.getClientAliases(keyType, issuers);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
          logger.debug("My getPrivateKey Called");
          return orig.getPrivateKey(alias);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
          logger.debug("My getServerAliases Called");
          return null;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers,
            SSLEngine engine) {
          logger.debug("My chooseEngineClientAlias Called");
          return orig.chooseEngineClientAlias(keyType, null, engine);
          // could just forward to chooseClientAlias(socket=null), that's what underlying does
        }

        @Override
        public String chooseEngineServerAlias(String keyType, Principal[] issuers,
            SSLEngine engine) {
          logger.debug("My chooseEngineServerAlias Called");
          return null;
        }

        @Override
        public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
          logger.debug("My chooseServerAlias Called");
          return null;
        }
      };

      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(km, tm, null);

      httpBuilder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) tm[0]);

    } catch (Exception e) {
      logger.error("Error: {}", e.getMessage(), e);
    }
  }
}
