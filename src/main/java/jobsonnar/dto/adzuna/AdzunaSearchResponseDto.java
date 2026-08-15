package jobsonnar.dto.adzuna;

import java.util.List;

public class AdzunaSearchResponseDto {
    private List<AdzunaJobDto> results;

    public List<AdzunaJobDto> getResults() {
        return results;
    }

    public void setResults(List<AdzunaJobDto> results) {
        this.results = results;
    }
}
