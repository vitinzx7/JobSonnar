package jobsonnar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.provider.JobProvider;
import jobsonnar.service.JobService;
import jobsonnar.service.SearchCache;
import org.junit.jupiter.api.Test;

class JobSonnarApplicationTests {

    @Test
    void searchJobsAggregatesProvidersAndFiltersBrasiliaMatches() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10"),
                job("Java Developer", "São Paulo/SP", "https://gupy.io/jobs/2", "2026-08-11")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "Plano Piloto - Brasília/DF", "https://jooble.org/jobs/1", "2026-08-12")
        ));
        JobProvider adzunaProvider = provider("adzuna", List.of(
                job("Java Developer", "Rio de Janeiro/RJ", "https://adzuna.com/jobs/1", "2026-08-13")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider, adzunaProvider));

        List<JobResponseDto> result = service.searchJobs(new JobSearchRequest("java", "Brasília/DF", 5));

        assertEquals(2, result.size());
    }

    @Test
    void searchJobsKeepsPartialResultsWhenOneProviderFails() {
        JobProvider failingProvider = new JobProvider() {
            @Override
            public String providerName() {
                return "failing";
            }

            @Override
            public List<JobResponseDto> searchJobs(JobSearchRequest request) {
                throw new IllegalStateException("provider unavailable");
            }
        };
        JobProvider workingProvider = provider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));

        JobService service = new JobService(List.of(failingProvider, workingProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(1, result.size());
    }

    @Test
    void searchJobsFiltersByProvider() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "Brasília/DF", "https://jooble.org/jobs/1", "2026-08-11")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider));

        List<JobResponseDto> result = service.searchJobs(new JobSearchRequest("java", "Brasília/DF", 5), "jooble");

        assertEquals(1, result.size());
        assertEquals("https://jooble.org/jobs/1", result.get(0).getJobUrl());
    }

    @Test
    void searchJobsDeduplicatesIdenticalJobsFromDifferentProviders() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "Brasília/DF", "https://jooble.org/jobs/1", "2026-08-11")
        ));
        JobProvider adzunaProvider = provider("adzuna", List.of(
                job("Frontend Developer", "Brasília/DF", "https://adzuna.com/jobs/1", "2026-08-12")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider, adzunaProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(2, result.size());
        assertEquals(List.of("https://adzuna.com/jobs/1", "https://gupy.io/jobs/1"),
                result.stream().map(JobResponseDto::getJobUrl).sorted().toList());
        assertEquals("gupy", result.stream()
                .filter(job -> job.getJobUrl().equals("https://gupy.io/jobs/1"))
                .findFirst().orElseThrow().getSource());
    }

    @Test
    void searchJobsKeepsSameTitleInDifferentCities() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "Acme Corp", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "Acme Corp", "Rio de Janeiro/RJ", "https://jooble.org/jobs/1", "2026-08-11")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(2, result.size());
    }

    @Test
    void searchJobsKeepsSameTitleAndCityFromDifferentCompanies() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "TechCorp", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "FinTech Brasil", "Brasília/DF", "https://jooble.org/jobs/1", "2026-08-11")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(2, result.size());
    }

    @Test
    void searchJobsDeduplicatesSameJobWithVariantCompanyName() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "Cognizant", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "Cognizant Technology Solutions", "Brasília/DF", "https://jooble.org/jobs/1", "2026-08-11")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(1, result.size());
        assertEquals("https://gupy.io/jobs/1", result.get(0).getJobUrl());
    }

    @Test
    void searchJobsRunsProvidersConcurrentlyAndPreservesOrder() {
        JobProvider firstProvider = slowProvider("first", 300,
                job("First Job", "Brasília/DF", "https://first.example/jobs/1", "2026-08-10"));
        JobProvider secondProvider = slowProvider("second", 300,
                job("Second Job", "Brasília/DF", "https://second.example/jobs/1", "2026-08-10"));

        JobService service = new JobService(List.of(firstProvider, secondProvider));

        long startedAt = System.nanoTime();
        List<JobResponseDto> result = service.searchJobs("java");
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);

        assertEquals(2, result.size());
        assertEquals("https://first.example/jobs/1", result.get(0).getJobUrl());
        assertEquals("https://second.example/jobs/1", result.get(1).getJobUrl());
        assertTrue(elapsedMillis < 550,
                "providers should run in parallel, took " + elapsedMillis + "ms for two 300ms providers");
    }

    @Test
    void searchJobsFirstProviderWinsEvenWhenItIsTheSlowest() {
        JobProvider slowProvider = slowProvider("slow", 300,
                job("Java Developer", "Brasília/DF", "https://slow.example/jobs/1", "2026-08-10"));
        JobProvider fastProvider = provider("fast", List.of(
                job("Java Developer", "Brasília/DF", "https://fast.example/jobs/1", "2026-08-11")));

        JobService service = new JobService(List.of(slowProvider, fastProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(1, result.size());
        assertEquals("https://slow.example/jobs/1", result.get(0).getJobUrl());
    }

    @Test
    void searchJobsSortsNewestFirstByPublishedDate() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Gupy Job", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10T14:30:00Z")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Jooble Job", "Brasília/DF", "https://jooble.org/jobs/1", "2026-08-11T00:00:00Z")
        ));
        JobProvider adzunaProvider = provider("adzuna", List.of(
                job("Adzuna Job", "Brasília/DF", "https://adzuna.com/jobs/1", null)
        ));
        JobProvider openwebninjaProvider = provider("openwebninja", List.of(
                job("Open Web Ninja Job", "Brasília/DF", "https://openwebninja.com/jobs/1", "2026-08-12")
        ));

        JobService service = new JobService(
                List.of(gupyProvider, joobleProvider, adzunaProvider, openwebninjaProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(List.of(
                        "https://openwebninja.com/jobs/1",
                        "https://jooble.org/jobs/1",
                        "https://gupy.io/jobs/1",
                        "https://adzuna.com/jobs/1"),
                result.stream().map(JobResponseDto::getJobUrl).toList());
    }

    @Test
    void searchJobsSetsSourceFromSupplyingProvider() {
        JobProvider gupyProvider = provider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")
        ));
        JobProvider joobleProvider = provider("jooble", List.of(
                job("Java Developer", "Brasília/DF", "https://jooble.org/jobs/1", "2026-08-11")
        ));

        JobService service = new JobService(List.of(gupyProvider, joobleProvider));

        List<JobResponseDto> result = service.searchJobs("java");

        assertEquals(1, result.size());
        assertEquals("gupy", result.get(0).getSource());
    }

    @Test
    void searchJobsBlankQueryReturnsEmptyWithoutCallingProviders() {
        CountingProvider provider = new CountingProvider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")));
        JobService service = new JobService(List.of(provider));

        List<JobResponseDto> result = service.searchJobs("   ");

        assertTrue(result.isEmpty());
        assertEquals(0, provider.invocationCount());
    }

    @Test
    void searchJobsClampsRadiusKmAndTreatsNonPositiveAsNull() {
        AtomicReference<JobSearchRequest> captured = new AtomicReference<>();
        JobProvider capturingProvider = new JobProvider() {
            @Override
            public String providerName() {
                return "capturing";
            }

            @Override
            public List<JobResponseDto> searchJobs(JobSearchRequest request) {
                captured.set(request);
                return List.of();
            }
        };
        JobService service = new JobService(List.of(capturingProvider));

        service.searchJobs(new JobSearchRequest("java", "", 100));
        assertEquals(Integer.valueOf(50), captured.get().radiusKm());

        service.searchJobs(new JobSearchRequest("java", "", 0));
        assertNull(captured.get().radiusKm());

        service.searchJobs(new JobSearchRequest("java", "", 30));
        assertEquals(Integer.valueOf(30), captured.get().radiusKm());
    }

    @Test
    void searchJobsTruncatesQueryToHundredChars() {
        AtomicReference<JobSearchRequest> captured = new AtomicReference<>();
        JobProvider capturingProvider = new JobProvider() {
            @Override
            public String providerName() {
                return "capturing";
            }

            @Override
            public List<JobResponseDto> searchJobs(JobSearchRequest request) {
                captured.set(request);
                return List.of();
            }
        };
        JobService service = new JobService(List.of(capturingProvider));

        service.searchJobs(new JobSearchRequest("x".repeat(150), "", null));

        assertEquals(100, captured.get().query().length());
        assertEquals("x".repeat(100), captured.get().query());
    }

    @Test
    void searchJobsCachesIdenticalRequests() {
        CountingProvider provider = new CountingProvider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")));
        JobService service = new JobService(
                List.of(provider), new SearchCache(Duration.ofSeconds(60)));

        List<JobResponseDto> first = service.searchJobs(new JobSearchRequest("java", "Brasília/DF", 5));
        List<JobResponseDto> second = service.searchJobs(new JobSearchRequest("java", "Brasília/DF", 5));

        assertEquals(1, provider.invocationCount());
        assertEquals(first, second);
        assertEquals(1, first.size());
    }

    @Test
    void searchJobsDifferentQueriesAreNotCached() {
        CountingProvider provider = new CountingProvider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")));
        JobService service = new JobService(
                List.of(provider), new SearchCache(Duration.ofSeconds(60)));

        service.searchJobs(new JobSearchRequest("java", "", null));
        service.searchJobs(new JobSearchRequest("frontend", "", null));

        assertEquals(2, provider.invocationCount());
    }

    @Test
    void searchJobsRefetchesAfterTtlExpiry() throws InterruptedException {
        CountingProvider provider = new CountingProvider("gupy", List.of(
                job("Java Developer", "Brasília/DF", "https://gupy.io/jobs/1", "2026-08-10")));
        JobService service = new JobService(
                List.of(provider), new SearchCache(Duration.ofMillis(50)));

        service.searchJobs("java");
        assertEquals(1, provider.invocationCount());

        service.searchJobs("java");
        assertEquals(1, provider.invocationCount());

        Thread.sleep(80);

        service.searchJobs("java");
        assertEquals(2, provider.invocationCount());
    }

    private JobProvider slowProvider(String name, long sleepMillis, JobResponseDto job) {
        return new JobProvider() {
            @Override
            public String providerName() {
                return name;
            }

            @Override
            public List<JobResponseDto> searchJobs(JobSearchRequest request) {
                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
                return List.of(job);
            }
        };
    }

    private JobProvider provider(String name, List<JobResponseDto> jobs) {
        return new JobProvider() {
            @Override
            public String providerName() {
                return name;
            }

            @Override
            public List<JobResponseDto> searchJobs(JobSearchRequest request) {
                return jobs;
            }
        };
    }

    private JobResponseDto job(String name, String city, String jobUrl, String publishedDate) {
        return new JobResponseDto(name, city, jobUrl, publishedDate, null);
    }

    private JobResponseDto job(String name, String company, String city, String jobUrl, String publishedDate) {
        return new JobResponseDto(name, city, jobUrl, publishedDate, company);
    }

    private static final class CountingProvider implements JobProvider {
        private final String name;
        private final List<JobResponseDto> jobs;
        private final AtomicInteger invocations = new AtomicInteger();

        private CountingProvider(String name, List<JobResponseDto> jobs) {
            this.name = name;
            this.jobs = jobs;
        }

        @Override
        public String providerName() {
            return name;
        }

        @Override
        public List<JobResponseDto> searchJobs(JobSearchRequest request) {
            invocations.incrementAndGet();
            return jobs;
        }

        private int invocationCount() {
            return invocations.get();
        }
    }
}
