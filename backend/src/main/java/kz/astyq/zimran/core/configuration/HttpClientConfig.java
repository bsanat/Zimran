package kz.astyq.zimran.core.configuration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Outbound {@link RestClient}s, each with its own connect and read timeouts. Built from the application's
 * {@code RestClient.Builder} when there is one (cloned, so customizations do not leak), else a plain builder.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient eventSourceRestClient(ObjectProvider<RestClient.Builder> builderProvider,
                                            EventSourceProperties properties) {
        return restClient(builderProvider.getIfAvailable(RestClient::builder).clone(),
                properties.getConnectTimeout(), properties.getReadTimeout(), null);
    }

    @Bean
    public RestClient geminiRestClient(ObjectProvider<RestClient.Builder> builderProvider,
                                       GeminiProperties properties) {
        return restClient(builderProvider.getIfAvailable(RestClient::builder).clone(),
                properties.getConnectTimeout(), properties.getReadTimeout(), properties.getBaseUrl());
    }

    /**
     * A client that does not follow redirects, with the given timeouts and optional base URL.
     */
    public static RestClient restClient(RestClient.Builder builder, Duration connectTimeout, Duration readTimeout,
                                        String baseUrl) {
        // HTTP/1.1: avoids the h2c upgrade on plain-http POSTs, which some servers (and WireMock) drop.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(connectTimeout)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        RestClient.Builder configured = builder.requestFactory(requestFactory);
        if (baseUrl != null) {
            configured = configured.baseUrl(baseUrl);
        }
        return configured.build();
    }
}
