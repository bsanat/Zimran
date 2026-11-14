package kz.astyq.zimran;

import kz.astyq.zimran.core.model.AppSettings;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppSettings.class)
public class ZimranApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZimranApplication.class, args);
	}

}
