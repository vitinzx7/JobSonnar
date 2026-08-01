package jobsonnar.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

import jobsonnar.dto.jooble.JoobleSearchResponseDto;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.jooble.JoobleJobDto;
import jobsonnar.dto.jooble.JoobleSearchRequestDto;

@Component
public class JoobleJobProvider {
    private final String apiKey;
    private final RestClient restClient;


    public JoobleJobProvider(@Value("${jooble.api.key}") String apiKey, RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
            .baseUrl("https://jooble.org")
            .build();
    }

    private JoobleSearchRequestDto buildJoobleSearchRequestDto(String query) {
        JoobleSearchRequestDto request = new JoobleSearchRequestDto();

        request.setKeywords(query);
        request.setLocation("Brazil");
        request.setPage("1");
        request.setResultOnPage("5");

        return request;
    }

    private JobResponseDto toJobResponseDto(JoobleJobDto job) {
        return new JobResponseDto(
                job.getTitle(),
                job.getLocation(),
                job.getLink(),
                job.getUpdated()
        );
    }


    public List<JobResponseDto> searchJobs(String query) {
        JoobleSearchRequestDto request = buildJoobleSearchRequestDto(query);

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
