package jobsonnar.dto.openwebninja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWebNinjaSearchResponseDto {
    private String status;
    private OpenWebNinjaDataDto data;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OpenWebNinjaDataDto getData() {
        return data;
    }

    public void setData(OpenWebNinjaDataDto data) {
        this.data = data;
    }
}
