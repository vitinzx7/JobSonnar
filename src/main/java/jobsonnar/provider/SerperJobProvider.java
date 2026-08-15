package jobsonnar.provider;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.dto.serper.SerperOrganicResultDto;
import jobsonnar.dto.serper.SerperSearchRequestDto;
import jobsonnar.dto.serper.SerperSearchResponseDto;

@Component
public class SerperJobProvider implements JobProvider {
    private static final String COUNTRY_CODE = "br";
    private static final String LANGUAGE_CODE = "pt-br";
    private static final int NUM_RESULTS = 10;

    private final String apiKey;
    private final RestClient restClient;

    public SerperJobProvider(
            @Value("${serper.api.key:}") String apiKey,
            RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl("https://google.serper.dev")
                .build();
    }

    @Override
    public String providerName() {
        return "serper";
    }

    @Override
    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        if (isBlank(apiKey) || request.query().isBlank()) {
            return List.of();
        }

        SerperSearchRequestDto searchRequest = buildSearchRequest(request);

        SerperSearchResponseDto response = restClient
                .post()
                .uri("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-API-KEY", apiKey)
                .body(searchRequest)
                .retrieve()
                .body(SerperSearchResponseDto.class);

        return response == null || response.getOrganic() == null
                ? List.of()
                : response.getOrganic().stream()
                        .map(this::toJobResponseDto)
                        .toList();
    }

    private SerperSearchRequestDto buildSearchRequest(JobSearchRequest request) {
        SerperSearchRequestDto searchRequest = new SerperSearchRequestDto();
        searchRequest.setQ(buildQuery(request));
        searchRequest.setGl(COUNTRY_CODE);
        searchRequest.setHl(LANGUAGE_CODE);
        searchRequest.setNum(NUM_RESULTS);
        return searchRequest;
    }

    private String buildQuery(JobSearchRequest request) {
        String location = request.location();
        if (location == null || location.isBlank()) {
            return request.query() + " jobs";
        }
        return request.query() + " jobs in " + location;
    }

    private JobResponseDto toJobResponseDto(SerperOrganicResultDto result) {
        return new JobResponseDto(
                result.getTitle(),
                null,
                result.getLink(),
                result.getDate(),
                null
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
