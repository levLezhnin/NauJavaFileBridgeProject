package ru.LevLezhnin.NauJava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import ru.LevLezhnin.NauJava.ui.console.ConsoleFlowRunner;

@SpringBootApplication
@EnableConfigurationProperties
public class NauJavaApplication {

	public static void main(String[] args) {
		ApplicationContext applicationContext = SpringApplication.run(NauJavaApplication.class, args);
		ConsoleFlowRunner consoleFlowRunner = applicationContext.getBean(ConsoleFlowRunner.class);
		consoleFlowRunner.start();
	}

}
