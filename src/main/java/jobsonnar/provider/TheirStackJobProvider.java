package jobsonnar.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.dto.theirstack.TheirStackJobDto;
import jobsonnar.dto.theirstack.TheirStackSearchRequestDto;
import jobsonnar.dto.theirstack.TheirStackSearchResponseDto;

public class TheirStackJobProvider implements JobProvider {
    private static final int MAX_AGE_DAYS = 30;
    private static final int LIMIT = 10;

    private final String apiKey;
    private final RestClient restClient;

    public TheirStackJobProvider(
            @Value("${theirstack.api.key:}") String apiKey,
            RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl("https://api.theirstack.com")
                .build();
    }

    @Override
    public String providerName() {
        return "theirstack";
    }

    @Override
    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        if (isBlank(apiKey) || request.query().isBlank()) {
            return List.of();
        }

        TheirStackSearchRequestDto searchRequest = buildSearchRequest(request.query());

        TheirStackSearchResponseDto response = restClient
                .post()
                .uri("/v1/jobs/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(searchRequest)
                .retrieve()
                .body(TheirStackSearchResponseDto.class);

        return response == null || response.getData() == null
                ? List.of()
                : response.getData().stream()
                        .map(this::toJobResponseDto)
                        .toList();
    }

    private TheirStackSearchRequestDto buildSearchRequest(String query) {
        TheirStackSearchRequestDto searchRequest = new TheirStackSearchRequestDto();
        searchRequest.setJobTitleOr(List.of(query));
        searchRequest.setPostedAtMaxAgeDays(MAX_AGE_DAYS);
        searchRequest.setLimit(LIMIT);
        return searchRequest;
    }

    private JobResponseDto toJobResponseDto(TheirStackJobDto job) {
        String city = job.getShortLocation();
        if (city == null || city.isBlank()) {
            city = job.getLocation();
        }
        String company = job.getCompany() == null ? null : job.getCompany().getName();
        return new JobResponseDto(
                job.getJobTitle(),
                city,
                job.getUrl(),
                job.getDatePosted(),
                company
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
