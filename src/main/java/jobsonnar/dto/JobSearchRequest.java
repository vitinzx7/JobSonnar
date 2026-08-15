package jobsonnar.dto;

public record JobSearchRequest(String query, String location, Integer radiusKm) {
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_RADIUS_KM = 50;

    public JobSearchRequest {
        query = query == null ? "" : query.trim();
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH);
        }
        location = location == null ? "" : location.trim();
        radiusKm = radiusKm != null && radiusKm > 0 ? Math.min(radiusKm, MAX_RADIUS_KM) : null;
    }
}