package jobsonnar.dto.openwebninja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWebNinjaDataDto {
    private List<OpenWebNinjaJobDto> jobs;

    public List<OpenWebNinjaJobDto> getJobs() {
        return jobs;
    }

    public void setJobs(List<OpenWebNinjaJobDto> jobs) {
        this.jobs = jobs;
    }
}
