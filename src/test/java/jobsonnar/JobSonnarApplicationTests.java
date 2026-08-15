package jobsonnar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.provider.JobProvider;
import jobsonnar.service.JobService;
import org.junit.jupiter.api.Test;

class JobSonnarApplicationTests {

    @Test
    void searchJobsAggregatesProvidersAndFiltersBrasiliaMatches() {
        JobProvider gupyProvider = request -> List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10"),
                job("Java Developer", "São Paulo/SP", "https://gupy.io/jobs/2", "2026-08-11")
        );
        JobProvider joobleProvider = request -> List.of(
                job("Java Developer", "Plano Piloto - Brasília/DF", "https://jooble.org/jobs/1", "2026-08-12")
        );
        JobProvider adzunaProvider = request -> List.of(
                job("Java Developer", "Rio de Janeiro/RJ", "https://adzuna.com/jobs/1", "2026-08-13")
        );

        JobService service = new JobService(List.of(gupyProvider, joobleProvider, adzunaProvider));

        List<JobResponseDto> result = service.searchJobs(new JobSearchRequest("java", "Brasília/DF", 5));

        assertEquals(2, result.size());
    }

    @Test
    void searchJobsKeepsPartialResultsWhenOneProviderFails() {
        JobProvider failingProvider = request -> {
            throw new IllegalStateException("provider unavailable");
        };
        JobProvider workingProvider = request -> List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        );

        JobService service = new JobService(List.of(failingProvider, workingProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(1, result.size());
    }

    private JobResponseDto job(String name, String city, String jobUrl, String publishedDate) {
        return new JobResponseDto(name, city, jobUrl, publishedDate);
    }
}
