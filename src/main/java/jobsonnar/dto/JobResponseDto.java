package jobsonnar.dto;

public class JobResponseDto {
    private String name;
    private String city;
    private String jobUrl;
    private String publishedDate;
    private String company;
    private String source;


    public JobResponseDto(String name, String city, String jobUrl, String publishedDate, String company) {
            this.name = name;
            this.city = city;
            this.jobUrl = jobUrl;
            this.publishedDate = publishedDate;
            this.company = company;
        }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }
}
