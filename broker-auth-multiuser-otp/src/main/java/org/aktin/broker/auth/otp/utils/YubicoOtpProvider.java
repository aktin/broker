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
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

// docs: https://developers.yubico.com/OTP/Specifications/OTP_validation_protocol.html
public class YubicoOtpProvider implements OtpProvider {

  private static final Logger log = Logger.getLogger(YubicoOtpProvider.class.getName());

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

  public YubicoOtpProvider() {
    this.clientId = System.getProperty(PROPERTY_CLIENT_ID);
    this.secretKey = System.getProperty(PROPERTY_SECRET_KEY);
    if (clientId == null || secretKey == null) {
      throw new IllegalStateException("Missing Yubico clientId or secretKey system properties");
    }
    log.fine("YubicoVerificationService initialized");
  }

  @Override
  public Optional<String> deriveBinding(String token) {
    if (token == null || token.length() < 12) {
      return Optional.empty();
    }
    return Optional.of(token.substring(0, 12));
  }

  @Override
  public boolean verify(String token) {
    if (!isValidOtp(token)) {
      return false;
    }
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
          log.fine("OTP successfully verified");
          return true;
        }
      } catch (Exception e) {
        log.fine("Error communicating with Yubico server at " + url + ": " + e.getMessage());
      }
    }
    log.warning("OTP verification failed after trying all Yubico servers");
    return false;
  }

  private boolean isValidOtp(String otp) {
    boolean valid = otp != null && !otp.trim().isEmpty();
    if (!valid) {
      log.warning("OTP cannot be null or empty");
    }
    return valid;
  }

  private String buildValidationUrl(String baseUrl, String token, String nonce) {
    String query = String.format("id=%s&nonce=%s&otp=%s",
        URLEncoder.encode(clientId, StandardCharsets.UTF_8),
        URLEncoder.encode(nonce, StandardCharsets.UTF_8),
        URLEncoder.encode(token, StandardCharsets.UTF_8));
    String signature = createQuerySignature(query);
    String fullUrl = baseUrl + "?" + query + "&h=" + signature;
    log.fine("Created Yubico Verification Request: " + fullUrl);
    return fullUrl;
  }

  private String createQuerySignature(String query) {
    try {
      log.fine("Signing query...");
      byte[] secretKeyBytes = Base64.getDecoder().decode(secretKey);
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA1"));
      byte[] signatureBytes = mac.doFinal(query.getBytes(StandardCharsets.UTF_8));
      String signature = Base64.getEncoder().encodeToString(signatureBytes);
      log.fine("Query signed successfully");
      return signature;
    } catch (InvalidKeyException | NoSuchAlgorithmException e) {
      log.severe("Failed to create query signature: " + e.getMessage());
      return "";
    }
  }

  private String fetchYubicoResponse(String urlStr) throws Exception {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setConnectTimeout(5000);
    conn.setReadTimeout(5000);
    int responseCode = conn.getResponseCode();
    if (responseCode != HttpURLConnection.HTTP_OK) {
      throw new Exception("Yubico verification failed: " + responseCode);
    }
    StringBuilder response = new StringBuilder();
    try (InputStreamReader isr = new InputStreamReader(conn.getInputStream()); BufferedReader reader = new BufferedReader(isr)) {
      int c;
      while ((c = reader.read()) != -1) {
        response.append((char) c);
      }
    }
    String raw = response.toString();
    log.fine("Yubico Raw Response: " + raw);
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
      if (computedSignature == null || !computedSignature.equals(receivedSignature)) {
        log.warning("Signature mismatch");
        return false;
      }
      log.fine("Signature verification successful");
      return true;
    } catch (Exception e) {
      log.severe("Exception during signature validation: " + e.getMessage());
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
    log.fine("Parsed response parameters");
    return params;
  }

  private String extractAndLogSignature(Map<String, String> params) {
    String signature = params.remove("h");
    if (signature == null) {
      log.warning("Missing signature in response (h=)");
      return null;
    }
    log.fine("Extracted signature");
    return signature;
  }

  private String buildSigningString(Map<String, String> params) {
    List<String> sortedKeys = new ArrayList<>(params.keySet());
    Collections.sort(sortedKeys);
    String signingString = sortedKeys.stream()
        .map(k -> k + "=" + params.get(k))
        .collect(Collectors.joining("&"));
    log.fine("Built signing string for verification");
    return signingString;
  }

  private boolean isSuccessful(String response) {
    boolean ok = response.contains("status=OK");
    if (ok) {
      log.fine("OTP status OK");
    }
    return ok;
  }

  private boolean isNonceValid(String response, String expectedNonce) {
    boolean match = response.contains("nonce=" + expectedNonce);
    if (!match) {
      log.warning("Nonce mismatch! Expected: " + expectedNonce);
    } else {
      log.fine("Nonce verified successfully");
    }
    return match;
  }

  private boolean isFailure(String response) {
    if (response.contains("status=BAD_OTP")) {
      log.warning("OTP status: BAD_OTP - invalid format");
      return true;
    }
    if (response.contains("status=REPLAYED_OTP")) {
      log.warning("OTP status: REPLAYED_OTP - OTP has already been seen");
      return true;
    }
    if (response.contains("status=BAD_SIGNATURE")) {
      log.warning("OTP status: BAD_SIGNATURE - HMAC signature verification failed");
      return true;
    }
    if (response.contains("status=MISSING_PARAMETER")) {
      log.warning("OTP status: MISSING_PARAMETER - request lacks a parameter");
      return true;
    }
    if (response.contains("status=NO_SUCH_CLIENT")) {
      log.warning("OTP status: NO_SUCH_CLIENT - client ID does not exist");
      return true;
    }
    if (response.contains("status=OPERATION_NOT_ALLOWED")) {
      log.warning("OTP status: OPERATION_NOT_ALLOWED - request ID not allowed to verify OTPs");
      return true;
    }
    if (response.contains("status=BACKEND_ERROR")) {
      log.warning("OTP status: BACKEND_ERROR - server error");
      return true;
    }
    if (response.contains("status=NOT_ENOUGH_ANSWERS")) {
      log.warning("OTP status: NOT_ENOUGH_ANSWERS - insufficient syncs during verification");
      return true;
    }
    if (response.contains("status=REPLAYED_REQUEST")) {
      log.warning("OTP status: REPLAYED_REQUEST - combination already seen");
      return true;
    }
    return false;
  }
}