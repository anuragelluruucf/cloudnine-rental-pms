package dev.stayledger.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "cloudnine")
public class CloudNineProperties {

    private Jwt jwt = new Jwt();
    private Seed seed = new Seed();
    private Ical ical = new Ical();
    private Llm llm = new Llm();

    @Getter
    @Setter
    public static class Jwt {
        /**
         * HS256 secret. At least 32 bytes. Override with JWT_SECRET outside local use.
         */
        private String secret = "dev-only-cloudnine-local-secret-key";
        private long ttlMinutes = 720;
    }

    @Getter
    @Setter
    public static class Seed {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Ical {
        private boolean schedulingEnabled = true;
        private long syncDelayMs = 600_000;
        private long syncInitialDelayMs = 120_000;
    }

    @Getter
    @Setter
    public static class Llm {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-4o-mini";
    }
}
