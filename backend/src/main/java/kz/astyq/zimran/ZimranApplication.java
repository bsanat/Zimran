package kz.astyq.zimran;

import kz.astyq.zimran.core.configuration.CityProperties;
import kz.astyq.zimran.core.configuration.CorsProperties;
import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.core.configuration.OpenAiProperties;
import kz.astyq.zimran.core.configuration.PlannerProperties;
import kz.astyq.zimran.core.configuration.WebProperties;
import kz.astyq.zimran.core.model.AppSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
		AppSettings.class,
		CityProperties.class,
		CorsProperties.class,
		WebProperties.class,
		EventSourceProperties.class,
		OpenAiProperties.class,
		PlannerProperties.class
})
public class ZimranApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZimranApplication.class, args);
	}

}
