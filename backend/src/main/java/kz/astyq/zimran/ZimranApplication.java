package kz.astyq.zimran;

import kz.astyq.zimran.core.configuration.CorsProperties;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.core.configuration.TicketmasterProperties;
import kz.astyq.zimran.core.model.AppSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		AppSettings.class,
		CorsProperties.class,
		TicketmasterProperties.class,
		OpenAiProperties.class,
		PlannerProperties.class
})
public class ZimranApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZimranApplication.class, args);
	}

}
