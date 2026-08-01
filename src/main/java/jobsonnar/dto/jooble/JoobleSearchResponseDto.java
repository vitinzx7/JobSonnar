package jobsonnar.dto.jooble;

import java.util.List;

public class JoobleSearchResponseDto {
    private Long totalCount;
    private List<JoobleJobDto> jobs;

    public Long getTotalCount() {
        return totalCount;
    }
    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public List<JoobleJobDto> getJobs() {
        return jobs;
    }
    public void setJobs(List<JoobleJobDto> jobs) {
        this.jobs = jobs;
    }
}
