/*
 * Copyright (C) 2021 University Hospital Bonn - All Rights Reserved You may use, distribute and
 * modify this code under the GPL 3 license. THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT
 * PERMITTED BY APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR
 * OTHER PARTIES PROVIDE THE PROGRAM “AS IS” WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH
 * YOU. SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
 * OR CORRECTION. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING WILL ANY
 * COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE,
 * BE LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES
 * ARISING OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA
 * OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD PARTIES OR A FAILURE OF THE
 * PROGRAM TO OPERATE WITH ANY OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGES. You should have received a copy of the GPL 3 license with *
 * this file. If not, visit http://www.gnu.de/documents/gpl-3.0.en.html
 */

package de.ukbonn.mwtek.dashboard.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.ukbonn.mwtek.dashboard.configuration.AbstractRestConfiguration;
import de.ukbonn.mwtek.dashboard.configuration.AbstractRestConfiguration.TokenBasedAuth;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class TokenAuthHelper {

  public static final String CLIENT_CREDENTIALS = "client_credentials";
  public static final String PASSWORD = "password";
  public static final String UME_SPECIFIC = "ume_specific";
  public static final String USERNAME = "username";
  private String accessToken;
  private String refreshToken;
  private long accessTokenExpiryTime;

  private static final long TOKEN_EXPIRY_BUFFER_MS = 30000;

  private static final String GRANT_TYPE = "grant_type";
  private static final String REFRESH_TOKEN = "refresh_token";
  private static final String CLIENT_ID = "client_id";
  private static final String CLIENT_SECRET = "client_secret";
  private static final String ACCESS_TOKEN = "access_token";
  private static final String EXPIRES_IN = "expires_in";

  private final AbstractRestConfiguration restConfiguration;

  public TokenAuthHelper(AbstractRestConfiguration restConfiguration) {
    this.restConfiguration = restConfiguration;
  }

  /**
   * Helper for REST calls that use Bearer Token Authentication.
   *
   * @return a pre-configured Spring RestTemplate object with token-based authentication.
   */
  protected RestTemplate getRestTemplateTokenAuth() {
    if (isAccessTokenExpired()) {
      refreshAccessToken();
    }
    return createRestTemplateWithToken();
  }

  /**
   * Checks if the access token is expired or nearing expiration. Applies a buffer to account for
   * network delays.
   *
   * @return true if the access token is expired or nearing expiration, false otherwise.
   */
  private boolean isAccessTokenExpired() {
    return System.currentTimeMillis() >= (accessTokenExpiryTime - TOKEN_EXPIRY_BUFFER_MS);
  }

  /**
   * Refreshes the access token. The used method differs is based on the <code>grant_type</code>
   * since <code>grant_type</code> = <code>password</code> gives back a refresh token and <code>
   * grant_type</code> = <code>client_credentials</code> not.
   */
  private void refreshAccessToken() {
    try {
      if (isRefreshTokenExpired()) {
        log.info("Refresh token is expired or missing. Fetching new tokens via password grant.");
        fetchNewTokens();
      } else {
        log.debug("Using refresh token to obtain new access token.");
        fetchTokensUsingRefreshToken();
      }
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
        log.error("Refresh token invalid. Falling back to password grant.", e);
        fetchNewTokens();
      } else {
        throw new TokenRefreshException("Failed to refresh token: " + e.getMessage(), e);
      }
    } catch (Exception e) {
      throw new TokenRefreshException("Unexpected error while refreshing token", e);
    }
  }

  /**
   * Checks if the refresh token is expired or not available. Some OAuth servers have a shorter
   * expiration time for refresh tokens.
   */
  private boolean isRefreshTokenExpired() {
    // Check if refresh token is null or if access token expiration is very close (suggests refresh
    // token might be expired too)
    return refreshToken == null
        || System.currentTimeMillis() >= (accessTokenExpiryTime - TOKEN_EXPIRY_BUFFER_MS);
  }

  /** Fetches new tokens using the appropriate grant type (client_credentials or password). */
  private void fetchNewTokens() {
    Map<String, Object> tokenResponse;
    String grantType = restConfiguration.getTokenBasedAuth().getGrantType();
    log.debug("Fetching new tokens via grant_type={}", grantType);
    if (grantType.equalsIgnoreCase(PASSWORD)) {
      // Fetch using password flow
      tokenResponse = performTokenRequest(getPasswordAuthRequestEntity());
    } else if (grantType.equalsIgnoreCase(CLIENT_CREDENTIALS)) {
      // Fetch using client_credentials flow (client_id + client_secret only)
      tokenResponse = performTokenRequest(getClientCredentialsRequestEntity());
    } else if (grantType.equalsIgnoreCase(UME_SPECIFIC)) {
      // Fetch using UME-specific flow
      log.debug("Fetching new tokens via grant_type=ume_specific.");
      tokenResponse =
          performUmeSpecificTokenRequest(
              getUmeSpecificPasswordAuthRequestEntity(),
              restConfiguration.getTokenBasedAuth().getRealm(),
              HttpMethod.GET,
              false);
    } else {
      throw new IllegalArgumentException("Unsupported grant type");
    }
    updateTokens(tokenResponse);
  }

  /** Fetches new tokens using the refresh token grant type. */
  private void fetchTokensUsingRefreshToken() throws Exception {
    Map<String, Object> tokenResponse;
    if (restConfiguration.getTokenBasedAuth().getGrantType().equalsIgnoreCase(UME_SPECIFIC)) {
      tokenResponse =
          performUmeSpecificTokenRequest(
              getUmeSpecificRefreshTokenRequestEntity(),
              restConfiguration.getTokenBasedAuth().getRealm(),
              HttpMethod.POST,
              true);
    } else {
      tokenResponse = performTokenRequest(getRefreshTokenRequestEntity());
    }
    updateTokens(tokenResponse);
  }

  /**
   * Performs the token request (either for password flow, client_credentials flow, or refresh
   * token) and returns the response.
   *
   * @param entity the HttpEntity containing the request parameters.
   * @return the response body as a map of token details.
   */
  private Map<String, Object> performTokenRequest(
      HttpEntity<MultiValueMap<String, String>> entity) {

    try {
      SSLContext sslContext;
      // Check if the keystore and truststore are provided
      if (restConfiguration.getTokenBasedAuth().getUseSsl()) {
        String keystore = restConfiguration.getKeyStore();
        String truststore = restConfiguration.getTrustStore();
        char[] truststorePassword = restConfiguration.getTrustStorePassword().toCharArray();
        char[] keystorePassword = restConfiguration.getKeyStorePassword().toCharArray();
        // resolve key and trust store locations
        File keyStoreFile = ResourceUtils.getFile(keystore);
        File trustStoreFile = ResourceUtils.getFile(truststore);

        // Load key and trust material
        sslContext =
            SSLContexts.custom()
                .loadKeyMaterial(keyStoreFile, keystorePassword, keystorePassword)
                .loadTrustMaterial(trustStoreFile, truststorePassword)
                .build();
      } else {
        // If no keystore/truststore, create a default SSLContext
        sslContext = SSLContexts.createDefault();
      }

      // Set up the HTTP client with the created SSLContext
      CloseableHttpClient httpClient =
          HttpClients.custom()
              .setSSLContext(sslContext)
              .setSSLHostnameVerifier(
                  NoopHostnameVerifier
                      .INSTANCE) // Disable hostname verification (can be adjusted in production)
              .build();

      // Create the RestTemplate using the configured HTTP client
      RestTemplate restTemplate =
          new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

      // Perform the token request
      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              restConfiguration.getTokenBasedAuth().getRealm(),
              HttpMethod.POST,
              entity,
              new ParameterizedTypeReference<>() {});

      return response.getBody();

    } catch (Exception ex) {
      // Basic error handling if something goes wrong with the SSL context setup
      log.error("Couldn't set up client SSL context: {}", ex.getMessage(), ex);
      throw new RuntimeException("Error occurred while performing the token request", ex);
    }
  }

  /**
   * Creates the request entity for the password flow, which includes the username and password.
   *
   * @return an HttpEntity containing the password flow request parameters.
   */
  private HttpEntity<MultiValueMap<String, String>> getPasswordAuthRequestEntity() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    TokenBasedAuth tokenBasedAuth = restConfiguration.getTokenBasedAuth();
    params.add(GRANT_TYPE, PASSWORD);
    params.add(CLIENT_ID, tokenBasedAuth.getClientId());
    params.add(CLIENT_SECRET, tokenBasedAuth.getClientSecret());
    params.add(USERNAME, tokenBasedAuth.getUsername());
    params.add(PASSWORD, tokenBasedAuth.getPassword());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return new HttpEntity<>(params, headers);
  }

  /**
   * Creates the request entity for the client_credentials flow, which includes only the client_id
   * and client_secret.
   *
   * @return an HttpEntity containing the client_credentials request parameters.
   */
  private HttpEntity<MultiValueMap<String, String>> getClientCredentialsRequestEntity() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    TokenBasedAuth tokenBasedAuth = restConfiguration.getTokenBasedAuth();
    params.add(GRANT_TYPE, CLIENT_CREDENTIALS);
    params.add(CLIENT_ID, tokenBasedAuth.getClientId());
    params.add(CLIENT_SECRET, tokenBasedAuth.getClientSecret());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return new HttpEntity<>(params, headers);
  }

  /**
   * Creates the request entity for the refresh_token grant type.
   *
   * @return an HttpEntity containing the refresh_token request parameters.
   */
  private HttpEntity<MultiValueMap<String, String>> getRefreshTokenRequestEntity() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    TokenBasedAuth tokenBasedAuth = restConfiguration.getTokenBasedAuth();
    params.add(GRANT_TYPE, REFRESH_TOKEN);
    params.add(REFRESH_TOKEN, refreshToken);
    params.add(CLIENT_ID, tokenBasedAuth.getClientId());
    params.add(CLIENT_SECRET, tokenBasedAuth.getClientSecret());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return new HttpEntity<>(params, headers);
  }

  /**
   * Updates the stored access token and refresh token using the provided response data.
   *
   * @param tokenResponse the response body containing the access token and refresh token.
   */
  private void updateTokens(Map<String, Object> tokenResponse) {
    if (!tokenResponse.containsKey(ACCESS_TOKEN) || !tokenResponse.containsKey(EXPIRES_IN)) {
      throw new IllegalArgumentException("Invalid token response: " + tokenResponse);
    }
    accessToken = tokenResponse.get(ACCESS_TOKEN).toString();
    // Update refresh Token just if needed.
    if (!restConfiguration.getTokenBasedAuth().getGrantType().equalsIgnoreCase(CLIENT_CREDENTIALS))
      refreshToken = tokenResponse.getOrDefault(REFRESH_TOKEN, refreshToken).toString();
    int expiresIn = (Integer) tokenResponse.get(EXPIRES_IN);
    // Calculate expiration time in milliseconds
    accessTokenExpiryTime = System.currentTimeMillis() + (expiresIn * 1000L);
  }

  /**
   * Creates a RestTemplate that includes the access token in the Authorization header for each
   * request.
   *
   * @return a RestTemplate instance configured with the Bearer token.
   */
  private RestTemplate createRestTemplateWithToken() {
    RestTemplate restTemplate = new RestTemplate();
    restTemplate
        .getInterceptors()
        .add(
            (request, body, execution) -> {
              request.getHeaders().setBearerAuth(accessToken);
              return execution.execute(request, body);
            });
    return restTemplate;
  }

  /** Custom exception for token refresh errors. */
  public static class TokenRefreshException extends RuntimeException {
    public TokenRefreshException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  private Map<String, Object> performUmeSpecificTokenRequest(
      HttpEntity<MultiValueMap<String, String>> entity,
      String url,
      HttpMethod httpMethod,
      boolean refreshToken) {
    Map<String, Object> resultMap = new HashMap<>();

    CloseableHttpClient httpClient = HttpClients.custom().build();
    RestTemplate restTemplate =
        new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

    // Check if "basicAuth" exists in the URL; log a warning if not found
    if (!url.contains("basicAuth")) {
      log.warn("Warning: The provided URL does not contain 'basicAuth'. No replacement was made.");
    }
    // Modify the URL to replace "basicAuth" with "refresh" for the token refresh endpoint
    if (refreshToken) url = url.replace("basicAuth", "refresh");

    // Change the suffix of the realm endpoint to call the specific refresh endpoint
    ResponseEntity<String> response = restTemplate.exchange(url, httpMethod, entity, String.class);
    String responseBody = response.getBody();
    resultMap.put(ACCESS_TOKEN, responseBody);
    resultMap.put(REFRESH_TOKEN, responseBody);
    try {
      resultMap.put(EXPIRES_IN, extractExpiryFromJwt(responseBody));
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while performing the token request", e);
    }
    return resultMap;
  }

  /**
   * Creates the request entity for the UME-specific password flow, which includes the username and
   * password.
   *
   * @return an HttpEntity containing the password flow request parameters.
   */
  private HttpEntity<MultiValueMap<String, String>> getUmeSpecificPasswordAuthRequestEntity() {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    TokenBasedAuth tokenBasedAuth = restConfiguration.getTokenBasedAuth();

    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth(tokenBasedAuth.getUsername(), tokenBasedAuth.getPassword());
    return new HttpEntity<>(params, headers);
  }

  /**
   * Creates the request entity for the refresh_token grant type.
   *
   * @return an HttpEntity containing the refresh_token request parameters.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private HttpEntity<MultiValueMap<String, String>> getUmeSpecificRefreshTokenRequestEntity() {
    return new HttpEntity(this.refreshToken);
  }

  private int extractExpiryFromJwt(String jwtToken) {
    try {
      String[] tokenParts = jwtToken.split("\\.");
      if (tokenParts.length < 2) {
        throw new IllegalArgumentException("Invalid JWT token format");
      }

      // Decode the payload part of the JWT (second part)
      String payloadJson =
          new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
      ObjectMapper objectMapper = new ObjectMapper();

      // Safe JSON deserialization to avoid unchecked warnings
      Map<String, Object> payloadMap =
          objectMapper.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});

      // Check if the "exp" (expiration) claim exists
      if (payloadMap.containsKey("exp")) {
        long exp = ((Number) payloadMap.get("exp")).longValue();
        return (int) (exp - Instant.now().getEpochSecond());
      } else {
        throw new IllegalArgumentException("Token does not contain an expiration claim");
      }
    } catch (Exception e) {
      throw new RuntimeException("Error parsing JWT", e);
    }
  }
}
