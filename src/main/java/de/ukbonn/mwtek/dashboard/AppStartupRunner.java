package de.ukbonn.mwtek.dashboard;

import de.ukbonn.mwtek.dashboard.configuration.FhirServerRestConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class AppStartupRunner implements ApplicationRunner {

  FhirServerRestConfiguration fhirServerConfig;

  public AppStartupRunner(FhirServerRestConfiguration fhirServerConfig) {
    this.fhirServerConfig = fhirServerConfig;
  }


  @Override
  public void run(ApplicationArguments args) throws Exception {
    if (fhirServerConfig.isUse() && fhirServerConfig.getAuthMethod().equals("TOKEN") && fhirServerConfig.isTokenRefresh()) {
      log.info("Token refresh time configured to " + this.fhirServerConfig.getTokenRefreshInterval() + " minutes.");
      final Runnable tokenRefreshRunner = () -> {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(this.fhirServerConfig.getTokenRefreshUrl()))
            .POST(HttpRequest.BodyPublishers.ofString(this.fhirServerConfig.getToken()))
            .build();
        try {
          HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
          if (response.statusCode() != 200) {
            throw new IOException("Could not fetch token: " + response.statusCode() + " " + response.body());
          }
          log.info("Token refreshed");
          log.debug("Token: " + response.body());
          this.fhirServerConfig.setToken(response.body());
        } catch (Exception e) {
          e.printStackTrace();
        }
      };

      final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      scheduler.scheduleAtFixedRate(
          tokenRefreshRunner,
          0,
          fhirServerConfig.getTokenRefreshInterval(),
          TimeUnit.MINUTES
      );
    }
  }
}
