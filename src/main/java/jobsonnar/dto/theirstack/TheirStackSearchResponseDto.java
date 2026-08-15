package jobsonnar.dto.theirstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TheirStackSearchResponseDto {
    private List<TheirStackJobDto> data;

    public List<TheirStackJobDto> getData() {
        return data;
    }

    public void setData(List<TheirStackJobDto> data) {
        this.data = data;
    }
}
