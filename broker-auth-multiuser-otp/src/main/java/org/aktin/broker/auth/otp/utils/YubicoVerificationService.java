package org.aktin.broker.auth.otp.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class YubicoVerificationService implements OtpVerificationService {

  private static final Logger log = Logger.getLogger(YubicoVerificationService.class.getName());

  private static final String PROPERTY_CLIENT_ID = "aktin.broker.auth.yubico.clientId";
  private static final String PROPERTY_SECRET_KEY = "aktin.broker.auth.yubico.secretKey";

  private final String clientId;
  private final String secretKey;

  private static final String[] YUBICO_VALIDATION_URLS = {
      "https://api.yubico.com/wsapi/2.0/verify",
      "https://api2.yubico.com/wsapi/2.0/verify",
      "https://api3.yubico.com/wsapi/2.0/verify",
      "https://api4.yubico.com/wsapi/2.0/verify",
      "https://api5.yubico.com/wsapi/2.0/verify"
  };

  public YubicoVerificationService() {
    this.clientId = System.getProperty(PROPERTY_CLIENT_ID);
    this.secretKey = System.getProperty(PROPERTY_SECRET_KEY);
    if (clientId == null || secretKey == null) {
      throw new IllegalStateException("Missing Yubico clientId or secretKey system properties!");
    }
    log.fine("YubicoVerificationService initialized with clientId and secretKey");
  }

  @Override
  public boolean verify(String token) {
    if (!isValidOtp(token)) {
      return false;
    }
    // Generate random nonce to verify yubikey response
    String nonce = UUID.randomUUID().toString().replace("-", "");
    for (String url : YUBICO_VALIDATION_URLS) {
      try {
        String fullUrl = buildValidationUrl(url, token, nonce);
        String response = fetchYubicoResponse(fullUrl);
        if (!isValidSignature(response)) {
          return false;
        }
        if (isFailure(response)) {
          return false;
        }
        if (isSuccessful(response) && isNonceValid(response, nonce)) {
          return true;
        }
      } catch (Exception e) {
        log.fine("Error communicating with Yubico API at " + url + ": " + e.getMessage());
        // Try the next server in the list if there's an error
      }
    }
    log.warning("OTP verification failed after trying all Yubico servers");
    return false;
  }

  private boolean isValidOtp(String otp) {
    boolean valid = otp != null && !otp.trim().isEmpty();
    if (!valid) log.warning("OTP cannot be null or empty");
    return valid;
  }


  private String buildValidationUrl(String baseUrl, String otp, String nonce) {
    String query = String.format("id=%s&nonce=%s&otp=%s",
        URLEncoder.encode(clientId, StandardCharsets.UTF_8),
        URLEncoder.encode(nonce, StandardCharsets.UTF_8),
        URLEncoder.encode(otp, StandardCharsets.UTF_8));
    String signature = createQuerySignature(query);
    String fullUrl = baseUrl + "?" + query + "&h=" + signature;
    log.fine(String.format("Created Yubico Verification Request:\n%s", fullUrl));
    return fullUrl;
  }

  private String createQuerySignature(String query) {
    String signature = "";
    try {
      log.fine("Signing query...");
      byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA1"));
      byte[] signatureBytes = mac.doFinal(query.getBytes(StandardCharsets.UTF_8));
      signature = Base64.getEncoder().encodeToString(signatureBytes);
      log.fine("Created Signature: " + signature);
    } catch (InvalidKeyException e) {
      log.fine("Invalid key provided: " + e.getMessage());
    } catch (NoSuchAlgorithmException e) {
      log.fine("No such algorithm: " + e.getMessage());
    }
    return signature;
  }

  private String fetchYubicoResponse(String urlStr) throws Exception {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(5000); // 5 seconds
    conn.setReadTimeout(5000);    // 5 seconds
    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new Exception("Yubico verification failed: " + responseCode);
    } else {
      log.fine("Yubico Response Code: " + responseCode);
    }
    StringBuilder response = new StringBuilder();
    try (InputStreamReader isr = new InputStreamReader(conn.getInputStream()); BufferedReader reader = new BufferedReader(isr)) {
      int c;
      while ((c = reader.read()) != -1) {
        response.append((char) c);
      }
    }
    String raw = response.toString();
    log.fine("Yubico Raw Response:\n" + raw);
    return raw;
  }

  private boolean isValidSignature(String response) {
    try {
      Map<String, String> params = parseResponseToMap(response);
      String receivedSignature = extractAndLogSignature(params);
      if (receivedSignature == null) {
        return false;
      }
      String signingString = buildSigningString(params);
      String computedSignature = createQuerySignature(signingString);
      if (computedSignature == null) {
        return false;
      }
      boolean match = computedSignature.equals(receivedSignature);
      if (match) {
        log.fine("Signature verification successful");
      } else {
        log.warning("Signature mismatch");
      }
      return match;
    } catch (Exception e) {
      log.warning("Exception during signature validation: " + e.getMessage());
      return false;
    }
  }

  private Map<String, String> parseResponseToMap(String rawResponse) {
    Map<String, String> params = new HashMap<>();
    for (String line : rawResponse.split("\r\n")) {
      int idx = line.indexOf('=');
      if (idx > 0 && idx < line.length() - 1) {
        String key = line.substring(0, idx);
        String value = line.substring(idx + 1);
        params.put(key, value);
      }
    }
    log.fine("Parsed parameters: " + params);
    return params;
  }

  private String extractAndLogSignature(Map<String, String> params) {
    String signature = params.remove("h");
    if (signature == null) {
      log.warning("Missing signature in response (h=)");
      return null;
    }
    log.info("Extracted signature: " + signature);
    return signature;
  }

  private String buildSigningString(Map<String, String> params) {
    List<String> sortedKeys = new ArrayList<>(params.keySet());
    Collections.sort(sortedKeys);
    String signingString = sortedKeys.stream()
        .map(k -> k + "=" + params.get(k))
        .collect(Collectors.joining("&"));
    log.info("Query String: " + signingString);
    return signingString;
  }

  private boolean isSuccessful(String response) {
    if (response.contains("status=OK")) {
      log.info("OTP status: OK");
      return true;
    }
    return false;
  }

  private boolean isNonceValid(String response, String expectedNonce) {
    boolean match = response.contains("nonce=" + expectedNonce);
    if (match) {
      log.info("Nonce verified: " + expectedNonce);
    } else {
      log.warning("Nonce mismatch! Expected: " + expectedNonce);
    }
    return match;
  }

  private boolean isFailure(String response) {
    if (response.contains("status=REPLAYED_OTP")) {
      log.warning("OTP status: REPLAYED_OTP");
      return true;
    }
    if (response.contains("status=BAD_OTP")) {
      log.warning("OTP status: BAD_OTP");
      return true;
    }
    // Add more status checks as per Yubico API documentation (e.g., NO_SUCH_CLIENT, BAD_SIGNATURE)
    return false;
  }
}