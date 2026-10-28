package jobsonnar.monitor;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MonitorConfiguration.CareerProperties.class)
public class MonitorConfiguration {
    @ConfigurationProperties("monitor")
    public record CareerProperties(List<CareerPage> careerPages) {
        public CareerProperties {
            careerPages = careerPages == null ? List.of() : List.copyOf(careerPages);
        }
    }

    // Selectors are only needed when the page has no JobPosting JSON-LD.
    public record CareerPage(String name, String url, String itemSelector,
            String titleSelector, String linkSelector, String locationSelector,
            String dateSelector) { }
}
