package jobsonnar.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import jobsonnar.dto.jooble.JoobleSearchResponseDto;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.jooble.JoobleJobDto;
import jobsonnar.dto.jooble.JoobleSearchRequestDto;
import jobsonnar.dto.JobSearchRequest;

@Component
public class JoobleJobProvider implements JobProvider {
    private final String apiKey;
    private final RestClient restClient;


    public JoobleJobProvider(@Value("${jooble.api.key}") String apiKey, RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
            .baseUrl("https://jooble.org")
            .build();
    }

    private JoobleSearchRequestDto buildJoobleSearchRequestDto(JobSearchRequest request) {
        JoobleSearchRequestDto searchRequest = new JoobleSearchRequestDto();

        searchRequest.setKeywords(request.query());
        searchRequest.setLocation(request.location());
        searchRequest.setPage("1");
        searchRequest.setResultOnPage("5");

        return searchRequest;
    }

    private JobResponseDto toJobResponseDto(JoobleJobDto job) {
        return new JobResponseDto(
                job.getTitle(),
                job.getLocation(),
                job.getLink(),
                job.getUpdated()
        );
    }


    @Override
    public List<JobResponseDto> searchJobs(JobSearchRequest searchRequest) {
        if (apiKey == null || apiKey.isBlank() || searchRequest.query() == null || searchRequest.query().isBlank()) {
            return List.of();
        }

        JoobleSearchRequestDto request = buildJoobleSearchRequestDto(searchRequest);

        JoobleSearchResponseDto response = restClient

            .post()
            .uri("/api/{apiKey}", apiKey)
            .body(request)
            .retrieve()
            .body(JoobleSearchResponseDto.class);

       return toJobResponseDtos(response);
    }

    private List<JobResponseDto> toJobResponseDtos(JoobleSearchResponseDto response) {
        if (response == null || response.getJobs() == null) {
            return List.of();
        }
        return response.getJobs().stream()
                .map(this::toJobResponseDto)
                .toList();
    }
}
