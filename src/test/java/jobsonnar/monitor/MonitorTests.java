package jobsonnar.monitor;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.web.client.RestClient;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;
import jobsonnar.monitor.MonitorConfiguration.CareerPage;
import jobsonnar.monitor.MonitorConfiguration.CareerProperties;
import jobsonnar.provider.GupyJobProvider;

class MonitorTests {
    @Test
    void historySurvivesReopeningDatabaseAndDoesNotResurfaceSeenJobs(@TempDir Path directory) {
        String url = "jdbc:h2:file:" + directory.resolve("history") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE";
        var first = repository(url);
        var search = first.addSearch("Java", "Brasília");
        var job = job("Java Developer", "https://empresa.example/jobs/123?utm_source=email");
        assertTrue(first.store(job, search.id()));
        var original = first.jobs(true, "", 50, 0).get(0);
        first.setSeen(original.id(), true);

        var reopened = repository(url);
        job.setJobUrl("https://empresa.example/jobs/123?utm_source=portal");
        assertFalse(reopened.store(job, search.id()));
        assertEquals(0, reopened.jobs(true, "", 50, 0).size());
        var saved = reopened.jobs(false, "", 50, 0);
        assertEquals(1, saved.size());
        assertEquals(original.firstSeenAt(), saved.get(0).firstSeenAt());
        assertTrue(saved.get(0).seen());
        assertEquals(search.id(), reopened.addSearch("  JAVA  ", "Brasilia").id());
    }

    @Test
    void differentOpeningsWithSameTitleAreKeptAndDeletingSearchKeepsHistory() {
        var repository = repository("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        var search = repository.addSearch("Java", "");
        repository.store(job("Java Developer", "https://empresa.example/jobs/1"), search.id());
        repository.store(job("Java Developer", "https://empresa.example/jobs/2"), search.id());
        repository.deleteSearch(search.id());
        assertEquals(2, repository.jobs(false, "gupy", 50, 0).size());
        assertEquals(1, repository.jobs(false, "", 1, 1).size());
    }

    @Test
    void canonicalUrlsKeepJobIdentifiersAndRejectUnsafeSchemes() {
        assertEquals("https://www.linkedin.com/jobs/view/1234", JobIdentity.canonicalUrl(
                "https://www.linkedin.com/comm/jobs/view/java-junior-1234?trackingId=abc"));
        assertEquals("https://www.indeed.com/viewjob?jk=abc123", JobIdentity.canonicalUrl(
                "https://br.indeed.com/rc/clk?jk=abc123&from=email"));
        assertEquals("https://example.com/job?id=12", JobIdentity.canonicalUrl(
                "https://example.com/job?id=12&utm_campaign=test#apply"));
        assertThrows(IllegalArgumentException.class, () -> JobIdentity.canonicalUrl("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class, () -> JobIdentity.canonicalUrl("https://user:pass@example.com/jobs"));
        assertEquals(JobIdentity.postingId("https://empresa.gupy.io/jobs/12764871"), JobIdentity.postingId(
                "https://empresa.gupy.io/job/eyJqb2JJZCI6MTI3NjQ4NzEsInNvdXJjZSI6Imd1cHlfcG9ydGFsIn0=?jobBoardSource=gupy_portal"));
    }

    @Test
    void emailParserReadsMultipartWithoutFollowingTrackingOrUnrelatedLinks() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        MimeBodyPart plain = new MimeBodyPart();
        plain.setText("https://www.linkedin.com/comm/jobs/view/1234?trk=alert");
        MimeBodyPart html = new MimeBodyPart();
        html.setContent("""
                <a href="https://www.linkedin.com/jobs/view/java-1234?trackingId=x">Java júnior</a>
                <a href="https://br.indeed.com/viewjob?jk=abc123&amp;from=email">Suporte TI</a>
                <a href="https://linkedin.com.evil.example/jobs/view/9876">Invasor</a>
                <a href="https://www.linkedin.com/jobs/search/">Ver todas</a>
                <a href="https://click.example/redirect">Rastreamento opaco</a>
                """, "text/html; charset=UTF-8");
        MimeMultipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(plain);
        multipart.addBodyPart(html);
        message.setContent(multipart);
        message.saveChanges();
        var jobs = new AlertEmailParser().parse(message);
        assertEquals(2, jobs.size());
        assertEquals("Java júnior", jobs.get(0).getName());
        assertEquals("indeed", jobs.get(1).getSource());
        assertEquals("", jobs.get(0).getPublishedDate()); // Email reception is not a posting date.
    }

    @Test
    void careerParserReadsJsonLdGraphsAndConfiguredHtmlCards() {
        var page = new CareerPage("Empresa", "https://empresa.example/carreiras", ".vacancy", "h2", "a", ".city", null);
        var document = Jsoup.parse("""
                <script type="application/ld+json">{"@graph":[{"@type":"JobPosting","title":"Java júnior",
                "url":"/jobs/1","hiringOrganization":{"name":"Empresa"},"datePosted":"2026-10-28",
                "jobLocation":{"address":{"addressLocality":"Brasília","addressRegion":"DF"}}}]}</script>
                <article class="vacancy"><h2>Estágio TI</h2><a href="/jobs/2">Ver vaga</a><p class="city">Remoto</p></article>
                """, page.url());
        var jobs = new CareerPageCollector().parse(document, page);
        assertEquals(2, jobs.size());
        assertEquals("https://empresa.example/jobs/1", jobs.get(0).getJobUrl());
        assertEquals("Brasília DF", jobs.get(0).getCity());
        assertEquals("careers", jobs.get(1).getSource());
        assertTrue(MonitorService.matchesTitle(jobs.get(1), "estágio TI"));
        assertFalse(MonitorService.matchesTitle(job("Administrativo", "https://example.com/1"), "TI"));
    }

    @Test
    void gupyPortalFormatIsRecognizedAndChangedPagesFailExplicitly() {
        var provider = new GupyJobProvider(RestClient.builder());
        var jobs = provider.parsePortal("""
                <script id="__NEXT_DATA__" type="application/json">{"props":{"pageProps":{"initialJobList":{
                "data":[{"id":1,"name":"Java","jobUrl":"https://empresa.gupy.io/jobs/1","careerPageName":"Empresa",
                "city":"Brasília","publishedDate":"2026-10-28","description":"Descrição"}],
                "pagination":{"total":1,"limit":12,"offset":0}}}}}</script>
                """);
        assertEquals(1, jobs.size());
        assertEquals("Empresa", jobs.get(0).getCompany());
        assertThrows(IllegalStateException.class, () -> provider.parsePortal("<html>Captcha</html>"));
        assertEquals(0, provider.parsePortal("""
                <script id="__NEXT_DATA__">{"props":{"pageProps":{"initialJobList":{"data":[]}}}}</script>
                """).size());
    }

    @Test
    void collectionKeepsGupyResultsWhenCompanyPageFailsAndSkipsPausedSearches() {
        var repository = repository("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL");
        repository.addSearch("Java", "");
        var paused = repository.addSearch("Python", "");
        repository.setEnabled(paused.id(), false);
        var gupy = new GupyJobProvider(RestClient.builder()) {
            @Override public List<JobResponseDto> searchJobs(JobSearchRequest request) {
                assertEquals("Java", request.query());
                return List.of(job("Java", "https://example.com/jobs/1"));
            }
        };
        var career = new CareerPageCollector() {
            @Override public List<JobResponseDto> collect(CareerPage page) throws java.io.IOException {
                throw new java.io.IOException("offline");
            }
        };
        var mailbox = new AlertMailboxCollector(new AlertEmailParser(), false, "", "", "", "JobSonnar");
        var props = new CareerProperties(List.of(new CareerPage("Empresa", "https://example.com", null, null, null, null, null)));
        var service = new MonitorService(repository, gupy, career, props, mailbox, true);
        service.collect();
        service.collect();
        assertEquals(1, repository.jobs(false, "", 50, 0).size());
        assertTrue(repository.searches().stream().filter(s -> s.query().equals("Java")).findFirst().orElseThrow()
                .runStatus().contains("Página de Empresa indisponível"));
        assertFalse(service.status().running());
    }

    private static MonitorRepository repository(String url) {
        var dataSource = new DriverManagerDataSource(url, "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        return new MonitorRepository(new JdbcTemplate(dataSource));
    }
    private static JobResponseDto job(String title, String url) {
        var job = new JobResponseDto(title, "", url, "2026-10-28", "Empresa");
        job.setSource("gupy");
        return job;
    }
}
