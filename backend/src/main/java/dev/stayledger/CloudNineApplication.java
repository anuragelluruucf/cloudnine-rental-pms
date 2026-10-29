package dev.stayledger;

import dev.stayledger.config.CloudNineProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(CloudNineProperties.class)
public class CloudNineApplication {

    public static void main(String[] args) {
        relaxIcalParsing();
        SpringApplication.run(CloudNineApplication.class, args);
    }

    public static void relaxIcalParsing() {
        System.setProperty("ical4j.parsing.relaxed", "true");
        System.setProperty("ical4j.unfolding.relaxed", "true");
        System.setProperty("ical4j.compatibility.outlook", "true");
        System.setProperty("ical4j.validation.relaxed", "true");
    }
}
