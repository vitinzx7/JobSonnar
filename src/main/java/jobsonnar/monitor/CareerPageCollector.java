package jobsonnar.monitor;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.monitor.MonitorConfiguration.CareerPage;

@Component
public class CareerPageCollector {
    private final JsonMapper mapper = JsonMapper.builder().build();

    public List<JobResponseDto> collect(CareerPage page) throws java.io.IOException {
        JobIdentity.canonicalUrl(page.url());
        Document document = Jsoup.connect(page.url()).userAgent("JobSonnar/0.1 personal-job-monitor")
                .timeout(15000).maxBodySize(2_000_000).get();
        List<JobResponseDto> jobs = parse(document, page);
        if (jobs.isEmpty()) {
            throw new java.io.IOException("Nenhuma vaga reconhecida; confira o HTML, JSON-LD ou seletores da página");
        }
        return jobs;
    }

    public List<JobResponseDto> parse(Document document, CareerPage page) {
        List<JobResponseDto> result = new ArrayList<>();
        for (Element script : document.select("script[type=application/ld+json]")) {
            try { readPosting(mapper.readTree(script.data()), page, document.baseUri(), result); }
            catch (RuntimeException ignored) { /* Other JSON-LD scripts can be unrelated or malformed. */ }
        }
        if (page.itemSelector() != null && !page.itemSelector().isBlank()) {
            for (Element item : document.select(page.itemSelector())) {
                Element link = select(item, page.linkSelector());
                if (link == null) { continue; }
                String url = link.absUrl("href");
                String title = text(item, page.titleSelector());
                if (title.isBlank()) { title = link.text(); }
                add(result, title, page.name(), text(item, page.locationSelector()), url,
                        text(item, page.dateSelector()));
            }
        }
        var unique = new LinkedHashMap<String, JobResponseDto>();
        result.forEach(job -> unique.putIfAbsent(job.getJobUrl(), job));
        return List.copyOf(unique.values());
    }

    private void readPosting(JsonNode node, CareerPage page, String base, List<JobResponseDto> result) {
        if (node == null) { return; }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) { readPosting(node.get(i), page, base, result); }
        } else if (node.isObject()) {
            JsonNode type = node.path("@type");
            boolean posting = type.asText("").equals("JobPosting");
            if (type.isArray()) {
                for (int i = 0; i < type.size(); i++) { posting |= type.get(i).asText("").equals("JobPosting"); }
            }
            if (posting) {
                JsonNode location = node.path("jobLocation");
                if (location.isArray() && !location.isEmpty()) { location = location.get(0); }
                JsonNode address = location.path("address");
                String city = (address.path("addressLocality").asText("") + " "
                        + address.path("addressRegion").asText("")).trim();
                if (node.path("jobLocationType").asText("").equals("TELECOMMUTE")) { city = "Remoto " + city; }
                String url = node.path("url").asText("");
                // A listing URL is not an identifier for every vacancy on that listing.
                if (!url.isBlank()) {
                    try { url = java.net.URI.create(base).resolve(url).toString(); }
                    catch (IllegalArgumentException ignored) { return; }
                    add(result, node.path("title").asText(node.path("name").asText("")),
                            node.path("hiringOrganization").path("name").asText(page.name()),
                            city, url, node.path("datePosted").asText(""));
                }
            }
            for (String field : List.of("@graph", "itemListElement", "item", "mainEntity")) {
                readPosting(node.get(field), page, base, result);
            }
        }
    }

    private void add(List<JobResponseDto> result, String title, String company, String city, String url, String date) {
        if (title == null || title.isBlank()) { return; }
        try {
            var job = new JobResponseDto(title, city, JobIdentity.canonicalUrl(url), date, company);
            job.setSource("careers");
            result.add(job);
        } catch (IllegalArgumentException ignored) { }
    }
    private static Element select(Element element, String selector) {
        return selector == null || selector.isBlank() ? element : element.selectFirst(selector);
    }
    private static String text(Element element, String selector) {
        if (selector == null || selector.isBlank()) { return ""; }
        Element selected = select(element, selector);
        return selected == null ? "" : selected.text();
    }
}
