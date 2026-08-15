package jobsonnar.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.dto.adzuna.AdzunaJobDto;
import jobsonnar.dto.adzuna.AdzunaSearchResponseDto;

@Component
public class AdzunaJobProvider implements JobProvider {
    private final String appId;
    private final String appKey;
    private final RestClient restClient;

    public AdzunaJobProvider(
            @Value("${adzuna.app.id:}") String appId,
            @Value("${adzuna.app.key:}") String appKey,
            RestClient.Builder restClientBuilder) {
        this.appId = appId;
        this.appKey = appKey;
        this.restClient = restClientBuilder
                .baseUrl("https://api.adzuna.com")
                .build();
    }

    @Override
    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        if (isBlank(appId) || isBlank(appKey) || request.query().isBlank()) {
            return List.of();
        }

        AdzunaSearchResponseDto response = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/jobs/br/search/1")
                        .queryParam("app_id", appId)
                        .queryParam("app_key", appKey)
                        .queryParam("what", request.query())
                        .queryParam("where", request.location())
                        .queryParam("results_per_page", "10")
                        .build())
                .retrieve()
                .body(AdzunaSearchResponseDto.class);

        return response == null || response.getResults() == null
                ? List.of()
                : response.getResults().stream()
                        .map(this::toJobResponseDto)
                        .toList();
    }

    private JobResponseDto toJobResponseDto(AdzunaJobDto job) {
        String city = job.getLocation() == null ? null : job.getLocation().getDisplayName();
        return new JobResponseDto(
                job.getTitle(),
                city,
                job.getRedirectUrl(),
                job.getCreated()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}