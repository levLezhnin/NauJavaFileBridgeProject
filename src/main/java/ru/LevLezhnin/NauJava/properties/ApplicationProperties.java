package ru.LevLezhnin.NauJava.properties;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {

    private String name;
    private String version;

    @PostConstruct
    public void displayAppProperties() {
        System.out.printf("::%s::", name);
        System.out.printf("v%s", version);
        System.out.println();
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setVersion(String version) {
        this.version = version;
    }
}
