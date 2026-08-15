package jobsonnar.dto;

public record JobSearchRequest(String query, String location, Integer radiusKm) {
    public JobSearchRequest {
        query = query == null ? "" : query.trim();
        location = location == null ? "" : location.trim();
        radiusKm = radiusKm != null && radiusKm > 0 ? radiusKm : null;
    }
}