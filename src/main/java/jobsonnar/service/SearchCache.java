package jobsonnar.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jobsonnar.dto.JobResponseDto;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SearchCache {

    private static final class Entry {
        private final List<JobResponseDto> jobs;
        private final long createdAtMillis;

        private Entry(List<JobResponseDto> jobs, long createdAtMillis) {
            this.jobs = jobs;
            this.createdAtMillis = createdAtMillis;
        }
    }

    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final long ttlMillis;

    @Autowired
    public SearchCache(@Value("${search.cache.ttl-seconds:120}") long ttlSeconds) {
        this(Duration.ofSeconds(ttlSeconds));
    }

    public SearchCache(Duration ttl) {
        this.ttlMillis = ttl.toMillis();
    }

    public List<JobResponseDto> get(String key) {
        Entry entry = entries.get(key);
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() - entry.createdAtMillis >= ttlMillis) {
            entries.remove(key, entry);
            return null;
        }
        return new ArrayList<>(entry.jobs);
    }

    public void put(String key, List<JobResponseDto> jobs) {
        entries.put(key, new Entry(List.copyOf(jobs), System.currentTimeMillis()));
    }
}
