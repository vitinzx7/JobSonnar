package jobsonnar.monitor;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import jobsonnar.dto.JobResponseDto;

@Component
public class AlertEmailParser {
    private static final Pattern HTTPS_URL = Pattern.compile("https://[^\\s<>\"']+");

    public List<JobResponseDto> parse(Part part) throws MessagingException, IOException {
        var jobs = new LinkedHashMap<String, JobResponseDto>();
        parsePart(part, jobs, 0);
        return List.copyOf(jobs.values());
    }

    private void parsePart(Part part, LinkedHashMap<String, JobResponseDto> jobs, int depth)
            throws MessagingException, IOException {
        if (depth > 10 || Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) { return; }
        if (part.isMimeType("multipart/*")) {
            Multipart content = (Multipart) part.getContent();
            for (int i = 0; i < content.getCount(); i++) { parsePart(content.getBodyPart(i), jobs, depth + 1); }
        } else if (part.isMimeType("text/html")) {
            String html = (String) part.getContent();
            if (html.length() > 2_000_000) { throw new IOException("Alerta maior que 2 MB"); }
            for (var anchor : Jsoup.parse(html).select("a[href]")) {
                add(jobs, anchor.attr("href"), anchor.text());
            }
        } else if (part.isMimeType("text/plain")) {
            String text = (String) part.getContent();
            if (text.length() > 2_000_000) { throw new IOException("Alerta maior que 2 MB"); }
            var matcher = HTTPS_URL.matcher(text);
            while (matcher.find()) { add(jobs, matcher.group(), "Vaga recebida por alerta"); }
        }
    }

    private void add(LinkedHashMap<String, JobResponseDto> jobs, String input, String title) {
        try {
            String url = JobIdentity.canonicalUrl(input);
            URI uri = URI.create(url);
            String host = uri.getHost();
            String source;
            if (host.equals("www.linkedin.com") && uri.getPath().matches("/jobs/view/\\d+")) {
                source = "linkedin";
            } else if (host.equals("www.indeed.com") && uri.getPath().equals("/viewjob")
                    && uri.getQuery() != null && uri.getQuery().matches("jk=[a-zA-Z0-9]+")) {
                source = "indeed";
            } else { return; }
            String label = title == null || title.isBlank() ? "Vaga recebida por alerta" : title;
            var job = new JobResponseDto(label, "", url, "", "");
            job.setSource(source);
            // Prefer a descriptive HTML title over a plain-text fallback in multipart messages.
            if (!jobs.containsKey(url) || jobs.get(url).getName().equals("Vaga recebida por alerta")) {
                jobs.put(url, job);
            }
        } catch (IllegalArgumentException ignored) { }
    }
}
