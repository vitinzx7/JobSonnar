package jobsonnar.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {
    private Long id;
    private String name;
    private String description;
    private String city;

    @JsonAlias({"job_url", "url", "link", "applyUrl", "apply_url"})
    private String jobUrl;

    @JsonAlias({"published_date", "publishedDate", "created_at", "createdAt", "updated_at", "updatedAt"})
    private String publishedDate;


    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public String getJobUrl() {
        return jobUrl;
    }
    public void setJobUrl(String jobUrl) {
        this.jobUrl = jobUrl;
    }
    public String getPublishedDate() {
        return publishedDate;
    }
    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }


}
