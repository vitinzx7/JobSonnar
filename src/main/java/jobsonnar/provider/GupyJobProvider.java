package jobsonnar.provider;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.jsoup.Jsoup;
import tools.jackson.databind.json.JsonMapper;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.model.Job;

@Component
public class GupyJobProvider implements JobProvider {
    private final RestClient restClient;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Override
    public String providerName() {
        return "gupy";
    }

    public GupyJobProvider(RestClient.Builder restClienteBuilder) {

        this.restClient = restClienteBuilder
        .baseUrl("https://portal.gupy.io")
        .build();
    }

    private JobResponseDto toJobResponseDto(Job job) {
        return new JobResponseDto(
                job.getName(),
                job.getCity(),
                job.getJobUrl(),
                job.getPublishedDate(),
                job.getCompany()
        );
    }

    @Override
    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        String query = request.query();

        if (query == null || query.isBlank()) {
            return List.of();
        }

        String html = restClient
        .get()
        .uri(java.net.URI.create("https://portal.gupy.io/job-search/term="
                + URLEncoder.encode(query, StandardCharsets.UTF_8).replace("+", "%20")
                + "&sortBy=publishedDate"))
        .header("User-Agent", "JobSonnar/0.1 personal-job-monitor")
        .retrieve()
        .body(String.class);

        return parsePortal(html);
    }

    public List<JobResponseDto> parsePortal(String html) {
        if (html == null || html.isBlank()) {
            throw new IllegalStateException("Gupy retornou uma página vazia");
        }
        var data = Jsoup.parse(html).selectFirst("script#__NEXT_DATA__");
        if (data == null) { throw new IllegalStateException("Formato do portal Gupy não reconhecido"); }
        var listing = mapper.readTree(data.data()).path("props").path("pageProps").path("initialJobList");
        if (!listing.path("data").isArray()) {
            throw new IllegalStateException("Lista de vagas não encontrada no portal Gupy");
        }
        Job[] jobs = mapper.treeToValue(listing.path("data"), Job[].class);
        return java.util.Arrays.stream(jobs)
            .map(this::toJobResponseDto)
            .toList();
    }
}
