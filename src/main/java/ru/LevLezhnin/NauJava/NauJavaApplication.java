package ru.LevLezhnin.NauJava;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableBatchProcessing
@EnableScheduling
public class NauJavaApplication {

	public static void main(String[] args) {
		SpringApplication.run(NauJavaApplication.class, args);
	}

}
