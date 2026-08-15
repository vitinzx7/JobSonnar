package jobsonnar.dto.theirstack;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TheirStackJobDto {
    @JsonProperty("job_title")
    private String jobTitle;

    private String url;

    @JsonProperty("short_location")
    private String shortLocation;

    private String location;

    @JsonProperty("date_posted")
    private String datePosted;

    private TheirStackCompanyDto company;

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getShortLocation() {
        return shortLocation;
    }

    public void setShortLocation(String shortLocation) {
        this.shortLocation = shortLocation;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDatePosted() {
        return datePosted;
    }

    public void setDatePosted(String datePosted) {
        this.datePosted = datePosted;
    }

    public TheirStackCompanyDto getCompany() {
        return company;
    }

    public void setCompany(TheirStackCompanyDto company) {
        this.company = company;
    }
}
