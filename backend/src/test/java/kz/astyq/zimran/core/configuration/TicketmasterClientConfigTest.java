package kz.astyq.zimran.core.configuration;

import kz.astyq.zimran.ticketmaster.client.TicketmasterClient;
import kz.astyq.zimran.ticketmaster.client.impl.TicketmasterClientImpl;
import kz.astyq.zimran.ticketmaster.util.RequestThrottle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterClientConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Wiring.class);

    @Test
    @DisplayName("wires the client with the Ticketmaster RestClient and throttle, with or without a RestClient.Builder bean")
    void wiresClient() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(TicketmasterClient.class);
            assertThat(context).hasBean("ticketmasterRestClient");
            assertThat(context).hasSingleBean(RequestThrottle.class);
        });
        runner.withBean(RestClient.Builder.class, RestClient::builder)
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(TicketmasterClient.class));
    }

    @Configuration
    @EnableConfigurationProperties(TicketmasterProperties.class)
    @Import({TicketmasterClientConfig.class, TicketmasterClientImpl.class})
    static class Wiring {
    }
}
