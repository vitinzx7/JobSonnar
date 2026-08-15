package jobsonnar.dto.theirstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TheirStackSearchRequestDto {
    @JsonProperty("job_title_or")
    private List<String> jobTitleOr;

    @JsonProperty("posted_at_max_age_days")
    private Integer postedAtMaxAgeDays;

    private Integer limit;

    public List<String> getJobTitleOr() {
        return jobTitleOr;
    }

    public void setJobTitleOr(List<String> jobTitleOr) {
        this.jobTitleOr = jobTitleOr;
    }

    public Integer getPostedAtMaxAgeDays() {
        return postedAtMaxAgeDays;
    }

    public void setPostedAtMaxAgeDays(Integer postedAtMaxAgeDays) {
        this.postedAtMaxAgeDays = postedAtMaxAgeDays;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }
}
