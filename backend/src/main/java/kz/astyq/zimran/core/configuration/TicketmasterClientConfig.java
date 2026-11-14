package kz.astyq.zimran.core.configuration;

import kz.astyq.zimran.ticketmaster.util.RequestThrottle;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class TicketmasterClientConfig {

    /** Discovery API quota is 5 requests/second per key. */
    public static final int PERMITS_PER_SECOND = 5;
    public static final Duration MAX_THROTTLE_WAIT = Duration.ofSeconds(1);

    /**
     * RestClient for the Discovery API. Uses the application's {@code RestClient.Builder} when one is
     * defined (cloned, so customizations here do not leak), otherwise a plain builder.
     */
    @Bean
    public RestClient ticketmasterRestClient(ObjectProvider<RestClient.Builder> builderProvider,
                                             TicketmasterProperties properties) {
        RestClient.Builder builder = builderProvider.getIfAvailable(RestClient::builder).clone();
        return ticketmasterRestClient(builder, properties);
    }

    @Bean
    public RequestThrottle ticketmasterRequestThrottle() {
        return new RequestThrottle(PERMITS_PER_SECOND, MAX_THROTTLE_WAIT);
    }

    public static RestClient ticketmasterRestClient(RestClient.Builder builder, TicketmasterProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return builder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
