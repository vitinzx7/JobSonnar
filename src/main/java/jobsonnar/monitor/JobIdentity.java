package jobsonnar.monitor;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

public final class JobIdentity {
    private static final Pattern LINKEDIN_ID = Pattern.compile("/(?:comm/)?jobs/view/(?:[^/?#]*-)?(\\d+)(?:/|$)");
    private JobIdentity() { }

    public static String canonicalUrl(String input) {
        try {
            URI uri = URI.create(input.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host.isBlank() || uri.getUserInfo() != null) {
                throw new IllegalArgumentException("A vaga precisa de um link HTTPS válido");
            }
            var linkedInId = LINKEDIN_ID.matcher(uri.getPath());
            if ((host.equals("linkedin.com") || host.endsWith(".linkedin.com")) && linkedInId.find()) {
                return "https://www.linkedin.com/jobs/view/" + linkedInId.group(1);
            }
            String query = uri.getRawQuery();
            if (query != null) {
                if (host.equals("indeed.com") || host.endsWith(".indeed.com")) {
                    for (String part : query.split("&")) {
                        if (part.matches("(?:jk|vjk)=[a-zA-Z0-9]+")) {
                            return "https://www.indeed.com/viewjob?jk=" + part.substring(part.indexOf('=') + 1);
                        }
                    }
                }
                query = String.join("&", Arrays.stream(query.split("&"))
                        .filter(part -> !part.toLowerCase(Locale.ROOT).matches("(?:utm_[^=]*|trackingid|refid|trk|jobboardsource)=.*"))
                        .toList());
            }
            return "https://" + host + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                    + (uri.getRawPath().isEmpty() ? "/" : uri.getRawPath())
                    + (query == null || query.isEmpty() ? "" : "?" + query);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("A vaga precisa de um link HTTPS válido", exception);
        }
    }

    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static String postingId(String input) {
        String url = canonicalUrl(input);
        URI uri = URI.create(url);
        if (uri.getHost().endsWith(".gupy.io")) {
            var numeric = Pattern.compile("/(?:candidates/)?jobs/(\\d+)(?:/|$)").matcher(uri.getPath());
            if (numeric.find()) { return "gupy:" + numeric.group(1); }
            if (uri.getPath().startsWith("/job/")) {
                try {
                    String encoded = uri.getPath().substring(5);
                    byte[] bytes;
                    try { bytes = java.util.Base64.getDecoder().decode(encoded); }
                    catch (IllegalArgumentException ignored) { bytes = java.util.Base64.getUrlDecoder().decode(encoded); }
                    var id = Pattern.compile("\"jobId\"\\s*:\\s*(\\d+)").matcher(new String(bytes, StandardCharsets.UTF_8));
                    if (id.find()) { return "gupy:" + id.group(1); }
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return url;
    }

    public static String normalize(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
