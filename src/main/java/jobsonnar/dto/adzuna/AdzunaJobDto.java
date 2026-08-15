package jobsonnar.dto.adzuna;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdzunaJobDto {
    private String title;

    @JsonProperty("redirect_url")
    private String redirectUrl;

    private String created;

    private AdzunaCompanyDto company;
    private AdzunaLocationDto location;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public AdzunaCompanyDto getCompany() {
        return company;
    }

    public void setCompany(AdzunaCompanyDto company) {
        this.company = company;
    }

    public AdzunaLocationDto getLocation() {
        return location;
    }

    public void setLocation(AdzunaLocationDto location) {
        this.location = location;
    }
}
