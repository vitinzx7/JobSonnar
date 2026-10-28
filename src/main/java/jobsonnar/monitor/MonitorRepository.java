package jobsonnar.monitor;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import jobsonnar.dto.JobResponseDto;

@Repository
public class MonitorRepository {
    // Fixed fractional precision keeps VARCHAR timestamps chronologically sortable.
    private static final DateTimeFormatter TIME = new DateTimeFormatterBuilder().appendInstant(3).toFormatter();
    private final JdbcTemplate jdbc;
    public MonitorRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record SavedSearch(String id, String query, String location, boolean enabled,
            String lastRunAt, String runStatus) { }
    public record SavedJob(String id, String name, String company, String city, String jobUrl,
            String publishedDate, String source, String firstSeenAt, String lastSeenAt, boolean seen) { }

    public List<SavedSearch> searches() {
        return jdbc.query("SELECT * FROM saved_search ORDER BY query_text, location_text", (rs, row) ->
                new SavedSearch(rs.getString("id"), rs.getString("query_text"), rs.getString("location_text"),
                        rs.getBoolean("enabled"), rs.getString("last_run_at"), rs.getString("run_status")));
    }

    public SavedSearch addSearch(String query, String location) {
        String term = trim(query, 100);
        String place = trim(location, 200);
        if (term.isBlank()) { throw new IllegalArgumentException("Informe um cargo ou palavra-chave"); }
        String id = JobIdentity.hash(JobIdentity.normalize(term) + "|" + JobIdentity.normalize(place));
        try {
            jdbc.update("INSERT INTO saved_search(id, query_text, location_text) VALUES (?, ?, ?)", id, term, place);
        } catch (DuplicateKeyException ignored) { /* Same subscription is idempotent. */ }
        return searches().stream().filter(search -> search.id().equals(id)).findFirst().orElseThrow();
    }

    public int deleteSearch(String id) { return jdbc.update("DELETE FROM saved_search WHERE id = ?", id); }
    public int setEnabled(String id, boolean enabled) {
        return jdbc.update("UPDATE saved_search SET enabled = ? WHERE id = ?", enabled, id);
    }
    public void recordRun(String id, String status) {
        jdbc.update("UPDATE saved_search SET last_run_at = ?, run_status = ? WHERE id = ?", now(), trim(status, 1000), id);
    }

    public boolean store(JobResponseDto job, String searchId) {
        if (job.getName() == null || job.getName().isBlank()) { return false; }
        String url = JobIdentity.canonicalUrl(job.getJobUrl());
        String id = JobIdentity.hash(JobIdentity.postingId(url));
        String timestamp = now();
        boolean inserted = false;
        int updated = jdbc.update("UPDATE saved_job SET last_seen_at = ? WHERE id = ?", timestamp, id);
        if (updated == 0) {
            try {
                jdbc.update("""
                        INSERT INTO saved_job(id, name, company, city, job_url, published_date, source, first_seen_at, last_seen_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """, id, trim(job.getName(), 500), trim(job.getCompany(), 500), trim(job.getCity(), 500),
                        trim(url, 4000), trim(job.getPublishedDate(), 100), trim(job.getSource(), 100), timestamp, timestamp);
                inserted = true;
            } catch (DuplicateKeyException ignored) {
                jdbc.update("UPDATE saved_job SET last_seen_at = ? WHERE id = ?", timestamp, id);
            }
        }
        if (searchId != null) {
            try {
                jdbc.update("INSERT INTO search_job(search_id, job_id) VALUES (?, ?)", searchId, id);
            } catch (DuplicateKeyException ignored) { }
        }
        return inserted;
    }

    public List<SavedJob> jobs(boolean unseenOnly, String source, int limit, int offset) {
        return jdbc.query("""
                SELECT * FROM saved_job WHERE (? = FALSE OR seen = FALSE) AND (? = '' OR source = ?)
                ORDER BY first_seen_at DESC, id LIMIT ? OFFSET ?
                """, (rs, row) -> new SavedJob(rs.getString("id"), rs.getString("name"), rs.getString("company"),
                        rs.getString("city"), rs.getString("job_url"), rs.getString("published_date"), rs.getString("source"),
                        rs.getString("first_seen_at"), rs.getString("last_seen_at"), rs.getBoolean("seen")),
                unseenOnly, source, source, Math.clamp(limit, 1, 200), Math.max(0, offset));
    }

    public int setSeen(String id, boolean seen) {
        return jdbc.update("UPDATE saved_job SET seen = ? WHERE id = ?", seen, id);
    }
    private static String now() { return TIME.format(Instant.now()); }
    private static String trim(String value, int max) {
        String text = value == null ? "" : value.trim();
        return text.substring(0, Math.min(text.length(), max));
    }
}
