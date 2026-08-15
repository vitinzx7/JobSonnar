package jobsonnar.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.dto.openwebninja.OpenWebNinjaDataDto;
import jobsonnar.dto.openwebninja.OpenWebNinjaJobDto;
import jobsonnar.dto.openwebninja.OpenWebNinjaSearchResponseDto;

public class OpenWebNinjaJobProvider implements JobProvider {
    private final String apiKey;
    private final RestClient restClient;

    @Override
    public String providerName() {
        return "openwebninja";
    }

    public OpenWebNinjaJobProvider(
            @Value("${openwebninja.api.key:}") String apiKey,
            RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl("https://api.openwebninja.com")
                .build();
    }

    @Override
    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        if (isBlank(apiKey) || request.query().isBlank()) {
            return List.of();
        }

        OpenWebNinjaSearchResponseDto response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/jsearch/search-v2")
                        .queryParam("query", buildQuery(request))
                        .build())
                .header("X-API-Key", apiKey)
                .retrieve()
                .body(OpenWebNinjaSearchResponseDto.class);

        return response == null || response.getData() == null
                ? List.of()
                : toJobResponseDtos(response.getData());
    }

    private String buildQuery(JobSearchRequest request) {
        String location = request.location();
        if (location == null || location.isBlank()) {
            return request.query();
        }
        return request.query() + " jobs in " + location;
    }

    private List<JobResponseDto> toJobResponseDtos(OpenWebNinjaDataDto data) {
        return data.getJobs() == null
                ? List.of()
                : data.getJobs().stream()
                        .map(this::toJobResponseDto)
                        .toList();
    }

    private JobResponseDto toJobResponseDto(OpenWebNinjaJobDto job) {
        return new JobResponseDto(
                job.getJobTitle(),
                job.getJobCity(),
                job.getJobApplyLink(),
                job.getJobPostedAtDatetimeUtc(),
                job.getEmployerName()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
