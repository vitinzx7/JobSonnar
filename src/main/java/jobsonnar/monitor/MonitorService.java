package jobsonnar.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.monitor.MonitorConfiguration.CareerProperties;
import jobsonnar.provider.GupyJobProvider;

@Service
public class MonitorService {
    private static final Logger LOGGER = Logger.getLogger(MonitorService.class.getName());
    private final MonitorRepository repository;
    private final GupyJobProvider gupy;
    private final CareerPageCollector careers;
    private final CareerProperties properties;
    private final AlertMailboxCollector mailbox;
    private final boolean enabled;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile String mailboxStatus = "Não configurado";

    public MonitorService(MonitorRepository repository, GupyJobProvider gupy, CareerPageCollector careers,
            CareerProperties properties, AlertMailboxCollector mailbox,
            @Value("${monitor.enabled:true}") boolean enabled) {
        this.repository = repository;
        this.gupy = gupy;
        this.careers = careers;
        this.properties = properties;
        this.mailbox = mailbox;
        this.enabled = enabled;
    }

    public record Status(boolean enabled, boolean running, boolean emailConfigured,
            String emailStatus, int careerPages) { }
    public Status status() {
        return new Status(enabled, running.get(), mailbox.enabled(), mailboxStatus, properties.careerPages().size());
    }

    @Scheduled(fixedDelayString = "${monitor.interval-ms:3600000}", initialDelayString = "${monitor.initial-delay-ms:10000}")
    public void collect() {
        if (!enabled || !running.compareAndSet(false, true)) { return; }
        try {
            var searches = repository.searches().stream().filter(MonitorRepository.SavedSearch::enabled).toList();
            var careerJobs = new ArrayList<JobResponseDto>();
            var careerErrors = new ArrayList<String>();
            if (!searches.isEmpty()) {
                // Each company page is downloaded once per run, shared across subscriptions.
                for (var page : properties.careerPages()) {
                    try { careerJobs.addAll(careers.collect(page)); }
                    catch (Exception exception) {
                        careerErrors.add("Página de " + page.name() + " indisponível ou sem formato reconhecido");
                        LOGGER.log(Level.WARNING, "Career page collection failed for {0}", page.name());
                    }
                }
            }
            for (var search : searches) {
                int added = 0;
                var errors = new ArrayList<>(careerErrors);
                try {
                    var request = new JobSearchRequest(search.query(), search.location(), null);
                    for (var job : gupy.searchJobs(request)) {
                        job.setSource("gupy");
                        if (matchesLocation(job, search.location())) { added += store(job, search.id()) ? 1 : 0; }
                    }
                } catch (Exception exception) {
                    errors.add("Gupy indisponível");
                    LOGGER.log(Level.WARNING, "Gupy collection failed");
                }
                for (var job : careerJobs) {
                    if (matchesTitle(job, search.query()) && matchesLocation(job, search.location())) {
                        added += store(job, search.id()) ? 1 : 0;
                    }
                }
                repository.recordRun(search.id(), added + " novas vagas guardadas"
                        + (errors.isEmpty() ? "" : "; " + String.join("; ", errors)));
            }
            if (mailbox.enabled()) {
                try {
                    int added = 0;
                    for (var job : mailbox.collect()) { added += store(job, null) ? 1 : 0; }
                    mailboxStatus = added + " novas vagas importadas dos alertas";
                } catch (Exception exception) {
                    mailboxStatus = "Falha ao importar: confira conexão, credenciais, pasta e limite de 1000 mensagens";
                    LOGGER.log(Level.WARNING, "Alert mailbox collection failed ({0})", exception.getClass().getSimpleName());
                }
            }
        } finally { running.set(false); }
    }

    private boolean store(JobResponseDto job, String searchId) {
        try { return repository.store(job, searchId); }
        catch (IllegalArgumentException exception) { return false; }
    }
    static boolean matchesTitle(JobResponseDto job, String query) {
        return containsTokens(job.getName(), query);
    }
    static boolean matchesLocation(JobResponseDto job, String location) {
        // Missing locality is retained rather than silently losing relevant jobs.
        return job.getCity() == null || job.getCity().isBlank() || containsTokens(job.getCity(), location);
    }
    private static boolean containsTokens(String text, String query) {
        String normalized = " " + JobIdentity.normalize(text) + " ";
        String wanted = JobIdentity.normalize(query);
        return wanted.isBlank() || Arrays.stream(wanted.split(" "))
                .allMatch(token -> normalized.contains(" " + token + " "));
    }
}
