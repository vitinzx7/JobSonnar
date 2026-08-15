package jobsonnar.service;

import org.springframework.stereotype.Service;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.provider.JobProvider;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class JobService {
    private static final Logger LOGGER = Logger.getLogger(JobService.class.getName());

    private static final List<String> BRASILIA_RADIUS_TERMS = List.of(
            "brasilia",
            "distrito federal",
            "df",
            "plano piloto",
            "asa sul",
            "asa norte",
            "sudoeste",
            "octogonal",
            "noroeste",
            "cruzeiro",
            "lago sul",
            "lago norte",
            "guara",
            "setor comercial sul",
            "setor comercial norte"
    );

    private static final double TITLE_SIMILARITY_THRESHOLD = 0.6;
    private static final double CITY_SIMILARITY_THRESHOLD = 0.6;
    private static final double COMPANY_SIMILARITY_THRESHOLD = 0.6;

    private final List<JobProvider> jobProviders;
    private final ExecutorService providerExecutor;
    private final SearchCache searchCache;

    public JobService(List<JobProvider> jobProviders) {
        this(jobProviders, new SearchCache(120L));
    }

    @Autowired
    public JobService(List<JobProvider> jobProviders, SearchCache searchCache) {
        this.jobProviders = jobProviders;
        this.searchCache = searchCache;
        this.providerExecutor = Executors.newFixedThreadPool(
                Math.max(1, jobProviders.size()),
                runnable -> {
                    Thread thread = new Thread(runnable, "jobsonnar-provider");
                    thread.setDaemon(true);
                    return thread;
                });
    }

    @PreDestroy
    void shutdown() {
        providerExecutor.shutdown();
    }

    public List<JobResponseDto> searchJobs(String query) {
        return searchJobs(new JobSearchRequest(query, "", null), null);
    }

    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        return searchJobs(request, null);
    }

    public List<JobResponseDto> searchJobs(JobSearchRequest request, String provider) {
        JobSearchRequest normalizedRequest = request == null
                ? new JobSearchRequest("", "", null)
                : request;

        if (normalizedRequest.query().isBlank()) {
            return List.of();
        }

        String cacheKey = buildCacheKey(normalizedRequest, provider);

        List<JobResponseDto> cached = searchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<JobResponseDto> result = runSearch(normalizedRequest, provider);
        searchCache.put(cacheKey, result);
        return result;
    }

    private List<JobResponseDto> runSearch(JobSearchRequest normalizedRequest, String provider) {
        List<JobProvider> matchingProviders = jobProviders.stream()
                .filter(jobProvider -> matchesProvider(jobProvider, provider))
                .toList();

        List<CompletableFuture<List<JobResponseDto>>> futures = matchingProviders.stream()
                .map(jobProvider -> CompletableFuture.supplyAsync(
                        () -> safeSearch(jobProvider, normalizedRequest),
                        providerExecutor))
                .toList();

        Map<String, JobResponseDto> jobsByKey = new LinkedHashMap<>();

        for (int i = 0; i < matchingProviders.size(); i++) {
            List<JobResponseDto> jobs;
            try {
                jobs = futures.get(i).join();
            } catch (CompletionException exception) {
                LOGGER.log(Level.WARNING, "Failed to fetch jobs from provider {0}",
                        matchingProviders.get(i).getClass().getSimpleName());
                jobs = List.of();
            }
            for (JobResponseDto job : jobs) {
                job.setSource(matchingProviders.get(i).providerName());
                if (matchesSearchArea(job, normalizedRequest) && !isDuplicate(job, jobsByKey.values())) {
                    jobsByKey.put(buildDedupKey(job), job);
                }
            }
        }

        List<JobResponseDto> result = new ArrayList<>(jobsByKey.values());
        sortByPublishedDate(result);
        return result;
    }

    private void sortByPublishedDate(List<JobResponseDto> jobs) {
        jobs.sort((first, second) -> {
            long firstTime = publishedTime(first.getPublishedDate());
            long secondTime = publishedTime(second.getPublishedDate());
            return Long.compare(secondTime, firstTime);
        });
    }

    private long publishedTime(String publishedDate) {
        if (publishedDate == null || publishedDate.isBlank()) {
            return Long.MIN_VALUE;
        }
        String candidate = publishedDate.trim();
        try {
            return Instant.parse(candidate).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(candidate).toEpochSecond(ZoneOffset.UTC) * 1000L;
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(candidate).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000L;
        } catch (DateTimeParseException ignored) {
        }
        return Long.MIN_VALUE;
    }

    private boolean isDuplicate(JobResponseDto candidate, Collection<JobResponseDto> accepted) {
        for (JobResponseDto existing : accepted) {
            if (isSameJob(candidate, existing)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSameJob(JobResponseDto first, JobResponseDto second) {
        String firstUrl = normalize(first.getJobUrl());
        String secondUrl = normalize(second.getJobUrl());
        if (!firstUrl.isBlank() && firstUrl.equals(secondUrl)) {
            return true;
        }

        if (tokenSimilarity(first.getName(), second.getName()) < TITLE_SIMILARITY_THRESHOLD) {
            return false;
        }

        return areCitiesCompatible(first.getCity(), second.getCity())
                && areCompaniesCompatible(first.getCompany(), second.getCompany());
    }

    private boolean areCompaniesCompatible(String firstCompany, String secondCompany) {
        String first = normalize(firstCompany);
        String second = normalize(secondCompany);
        if (first.isBlank() || second.isBlank()) {
            return true;
        }
        if (tokenSimilarity(first, second) >= COMPANY_SIMILARITY_THRESHOLD) {
            return true;
        }
        return first.length() >= 3 && (first.contains(second) || second.contains(first));
    }

    private boolean areCitiesCompatible(String firstCity, String secondCity) {
        String first = normalize(firstCity);
        String second = normalize(secondCity);
        if (first.isBlank() || second.isBlank()) {
            return true;
        }
        return tokenSimilarity(first, second) >= CITY_SIMILARITY_THRESHOLD;
    }

    private double tokenSimilarity(String first, String second) {
        Set<String> firstTokens = tokenSet(first);
        Set<String> secondTokens = tokenSet(second);
        if (firstTokens.isEmpty() && secondTokens.isEmpty()) {
            return 1.0;
        }

        Set<String> intersection = new HashSet<>(firstTokens);
        intersection.retainAll(secondTokens);

        Set<String> union = new HashSet<>(firstTokens);
        union.addAll(secondTokens);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenSet(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(List.of(normalized.split(" ")));
    }

    private boolean matchesProvider(JobProvider jobProvider, String requestedProvider) {
        if (requestedProvider == null || requestedProvider.isBlank()) {
            return true;
        }

        String expected = normalize(jobProvider.providerName()).replaceAll("\\s+", "");
        String requested = normalize(requestedProvider).replaceAll("\\s+", "");
        return expected.equals(requested);
    }

    private String buildCacheKey(JobSearchRequest request, String provider) {
        String providerKey = provider == null || provider.isBlank()
                ? "all"
                : normalize(provider).replaceAll("\\s+", "");
        return String.join("|",
                request.query(),
                request.location(),
                String.valueOf(request.radiusKm()),
                providerKey);
    }

    private List<JobResponseDto> safeSearch(JobProvider provider, JobSearchRequest request) {
        try {
            return provider.searchJobs(request);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to fetch jobs from provider {0}", provider.getClass().getSimpleName());
            LOGGER.log(Level.FINE, exception.getMessage(), exception);
            return List.of();
        }
    }

    private boolean matchesSearchArea(JobResponseDto job, JobSearchRequest request) {
        String haystack = normalize(joinFields(job.getName(), job.getCity(), job.getPublishedDate(), job.getJobUrl()));
        String requestedLocation = normalize(request.location());
        boolean hasLocation = requestedLocation != null && !requestedLocation.isBlank();
        boolean hasRadius = request.radiusKm() != null && request.radiusKm() > 0;

        if (!hasLocation && !hasRadius) {
            return true;
        }

        if (hasLocation && !hasRadius) {
            return haystack.contains(requestedLocation);
        }

        if (!hasLocation && hasRadius) {
            return containsAny(haystack, BRASILIA_RADIUS_TERMS);
        }

        return haystack.contains(requestedLocation) || containsAny(haystack, BRASILIA_RADIUS_TERMS);
    }

    private String buildDedupKey(JobResponseDto job) {
        return normalize(joinFields(job.getJobUrl(), job.getName(), job.getCity()));
    }

    private String joinFields(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(' ');
                }
                builder.append(value);
            }
        }
        return builder.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        return Objects.requireNonNullElse(normalized, "");
    }

    private boolean containsAny(String text, List<String> terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
