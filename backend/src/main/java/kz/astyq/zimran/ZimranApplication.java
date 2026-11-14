package kz.astyq.zimran;

import kz.astyq.zimran.core.configuration.AiAssistantProperties;
import kz.astyq.zimran.core.configuration.CityProperties;
import kz.astyq.zimran.core.configuration.CorsProperties;
import kz.astyq.zimran.core.configuration.DemoProperties;
import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.configuration.FrontendProperties;
import kz.astyq.zimran.core.configuration.GuestProperties;
import kz.astyq.zimran.core.configuration.IngestionProperties;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.configuration.PairProperties;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.core.configuration.RateLimitProperties;
import kz.astyq.zimran.core.configuration.SubscriptionProperties;
import kz.astyq.zimran.core.configuration.WebProperties;
import kz.astyq.zimran.core.model.AppSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
		AppSettings.class,
		CityProperties.class,
		CorsProperties.class,
		WebProperties.class,
		EventSourceProperties.class,
		OpenAiProperties.class,
		PlannerProperties.class,
		GuestProperties.class,
		SubscriptionProperties.class,
		DemoProperties.class,
		RateLimitProperties.class,
		IngestionProperties.class,
		AiAssistantProperties.class,
		FrontendProperties.class,
		PairProperties.class
})
public class ZimranApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZimranApplication.class, args);
	}

}
