package jobsonnar.service;

import org.springframework.stereotype.Service;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.provider.JobProvider;

import java.text.Normalizer;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private final List<JobProvider> jobProviders;

    public JobService(List<JobProvider> jobProviders) {
        this.jobProviders = jobProviders;
    }

    public List<JobResponseDto> searchJobs(String query) {
        return searchJobs(new JobSearchRequest(query, "", null));
    }

    public List<JobResponseDto> searchJobs(JobSearchRequest request) {
        JobSearchRequest normalizedRequest = request == null
                ? new JobSearchRequest("", "", null)
                : request;

        Map<String, JobResponseDto> jobsByKey = new LinkedHashMap<>();

        for (JobProvider provider : jobProviders) {
            for (JobResponseDto job : safeSearch(provider, normalizedRequest)) {
                if (matchesSearchArea(job, normalizedRequest)) {
                    jobsByKey.putIfAbsent(buildDedupKey(job), job);
                }
            }
        }

        return new ArrayList<>(jobsByKey.values());
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
