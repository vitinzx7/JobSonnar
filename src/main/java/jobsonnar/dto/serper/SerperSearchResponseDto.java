package jobsonnar.dto.serper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SerperSearchResponseDto {
    private List<SerperOrganicResultDto> organic;

    public List<SerperOrganicResultDto> getOrganic() {
        return organic;
    }

    public void setOrganic(List<SerperOrganicResultDto> organic) {
        this.organic = organic;
    }
}
