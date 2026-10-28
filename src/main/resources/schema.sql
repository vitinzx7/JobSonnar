CREATE TABLE IF NOT EXISTS saved_search (
    id VARCHAR(64) PRIMARY KEY,
    query_text VARCHAR(100) NOT NULL,
    location_text VARCHAR(200) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_at VARCHAR(40),
    run_status VARCHAR(1000)
);
CREATE TABLE IF NOT EXISTS saved_job (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    company VARCHAR(500),
    city VARCHAR(500),
    job_url VARCHAR(4000) NOT NULL,
    published_date VARCHAR(100),
    source VARCHAR(100) NOT NULL,
    first_seen_at VARCHAR(40) NOT NULL,
    last_seen_at VARCHAR(40) NOT NULL,
    seen BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS saved_job_first_seen ON saved_job(first_seen_at);
CREATE TABLE IF NOT EXISTS search_job (
    search_id VARCHAR(64) NOT NULL REFERENCES saved_search(id) ON DELETE CASCADE,
    job_id VARCHAR(64) NOT NULL REFERENCES saved_job(id),
    PRIMARY KEY(search_id, job_id)
);
