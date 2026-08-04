package jobsonnar.provider;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.model.Job;

@Component
public class GupyJobProvider {
    private final RestClient restClient;

    public GupyJobProvider(RestClient.Builder restClienteBuilder) {

        this.restClient = restClienteBuilder
        .baseUrl("https://employability-portal.gupy.io")
        .build();
    }

        private JobResponseDto toJobResponseDto(Job job) {
        return new JobResponseDto(
                job.getName(),
                job.getCity(),
                job.getJobUrl(),
                job.getPublishedDate()
        );
    }


}
