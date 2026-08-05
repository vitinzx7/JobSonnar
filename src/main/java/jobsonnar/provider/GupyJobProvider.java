package jobsonnar.provider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jobsonnar.JobResponse;
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

    public List<JobResponseDto> searchJobs(String query) {
        JobResponse response = restClient 
        .get()
        .uri("/api/v1/jobs?jobName=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
        .retrieve()
        .body(JobResponse.class);

      return toJobResponseDtos(response);
    }

    private List<JobResponseDto> toJobResponseDtos(JobResponse response) {
        if (response == null || response.getData() == null) {
            return List.of();
        }
        return response.getData().stream()
            .map(this::toJobResponseDto)
            .toList();
    }
}
