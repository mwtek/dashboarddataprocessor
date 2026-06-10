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

import de.ukbonn.mwtek.dashboard.configuration.AbstractRestConfiguration;
import java.io.File;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.ResourceUtils;
import org.springframework.web.client.RestTemplate;

/** Base class for all rest consumers in this project. */
@Slf4j
public class RestConsumer {

  protected AbstractRestConfiguration restConfiguration;

  public RestConsumer(AbstractRestConfiguration restConfiguration) {
    this.restConfiguration = restConfiguration;
  }

  public boolean isUsed() {
    return restConfiguration.isUse();
  }

  /**
   * Provides a {@link RestTemplate} depending on the chosen method.
   *
   * @return {@link RestTemplate} initialized with the settings from the runtime configuration
   */
  public synchronized RestTemplate getRestTemplate() {
    String method = restConfiguration.getAuthMethod();

    if ((method == null) || (method.isEmpty())) {
      // default
      return getRestTemplateBasicAuth();
    } // if

    switch (method.toUpperCase()) {
      case "BASIC" -> {
        return getRestTemplateBasicAuth();
      } // case
      case "SSL" -> {
        return getRestTemplateCertificateAuth();
      } // case
      case "TOKEN" -> {
        return getRestTemplateTokenAuth();
      }
      case "NONE" -> {
        return getRestTemplateNone();
      } // case
      default -> {
        return getRestTemplateBasicAuth();
      } // default
    } // switch
  }

  /**
   * helper for REST calls without Auth
   *
   * @return a pre configured spring RestTemplate object
   */
  protected RestTemplate getRestTemplateNone() {
    RestTemplate result = new RestTemplate();
    result.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    return result;
  }

  /**
   * helper for REST calls that use Basic Authentication
   *
   * @return a pre configured spring RestTemplate object
   */
  protected RestTemplate getRestTemplateBasicAuth() {
    RestTemplate result = new RestTemplate();
    result
        .getInterceptors()
        .add(
            new BasicAuthenticationInterceptor(
                restConfiguration.getRestUser(), restConfiguration.getRestPassword()));
    result.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
    return result;
  }

  private TokenAuthHelper tokenAuthHelper;

  /**
   * Helper for REST calls that use Token Authentication.
   *
   * @return {@link RestTemplate} initialized with the settings from the runtime configuration
   */
  protected RestTemplate getRestTemplateTokenAuth() {
    if (tokenAuthHelper == null) tokenAuthHelper = new TokenAuthHelper(restConfiguration);
    return tokenAuthHelper.getRestTemplateTokenAuth();
  }

  /**
   * Helper for rest calls that use x509 cert authentication
   *
   * @return {@link RestTemplate} initialized with the settings from the runtime configuration
   */
  protected RestTemplate getRestTemplateCertificateAuth() {

    try {
      File keyStoreFile = ResourceUtils.getFile(restConfiguration.getKeyStore());
      File trustStoreFile = ResourceUtils.getFile(restConfiguration.getTrustStore());

      char[] keyStorePassword = restConfiguration.getKeyStorePassword().toCharArray();
      char[] trustStorePassword = restConfiguration.getTrustStorePassword().toCharArray();

      // Build SSLContext
      SSLContext sslContext =
          SSLContextBuilder.create()
              .loadKeyMaterial(keyStoreFile, keyStorePassword, keyStorePassword)
              .loadTrustMaterial(trustStoreFile, trustStorePassword)
              .build();

      // Hostname verifier (HttpClient 5)
      DefaultHostnameVerifier hostnameVerifier = new DefaultHostnameVerifier();

      // SSL socket factory (classic client, deprecated but REQUIRED)
      SSLConnectionSocketFactory sslSocketFactory =
          new SSLConnectionSocketFactory(
              sslContext, new String[] {"TLSv1.3", "TLSv1.2"}, null, hostnameVerifier);

      // Socket factory registry
      Registry<ConnectionSocketFactory> socketFactoryRegistry =
          RegistryBuilder.<ConnectionSocketFactory>create()
              .register("https", sslSocketFactory)
              .register("http", PlainConnectionSocketFactory.getSocketFactory())
              .build();

      // Connection manager
      PoolingHttpClientConnectionManager connectionManager =
          new PoolingHttpClientConnectionManager(socketFactoryRegistry);

      // HttpClient
      CloseableHttpClient httpClient =
          HttpClients.custom().setConnectionManager(connectionManager).build();

      RestTemplate restTemplate =
          new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));

      restTemplate
          .getMessageConverters()
          .addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));

      return restTemplate;

    } catch (Exception ex) {
      log.error("Couldn't set up client SSL context", ex);
      return new RestTemplate();
    }
  }
}
