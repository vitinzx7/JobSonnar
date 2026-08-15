package jobsonnar.dto.openwebninja;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWebNinjaJobDto {
    @JsonProperty("job_title")
    private String jobTitle;

    @JsonProperty("job_apply_link")
    private String jobApplyLink;

    @JsonProperty("job_city")
    private String jobCity;

    @JsonProperty("job_posted_at_datetime_utc")
    private String jobPostedAtDatetimeUtc;

    @JsonProperty("employer_name")
    private String employerName;

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getJobApplyLink() {
        return jobApplyLink;
    }

    public void setJobApplyLink(String jobApplyLink) {
        this.jobApplyLink = jobApplyLink;
    }

    public String getJobCity() {
        return jobCity;
    }

    public void setJobCity(String jobCity) {
        this.jobCity = jobCity;
    }

    public String getJobPostedAtDatetimeUtc() {
        return jobPostedAtDatetimeUtc;
    }

    public void setJobPostedAtDatetimeUtc(String jobPostedAtDatetimeUtc) {
        this.jobPostedAtDatetimeUtc = jobPostedAtDatetimeUtc;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }
}
