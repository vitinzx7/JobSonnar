package jobsonnar.dto.jooble;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JoobleSearchRequestDto {
    private String keywords;
    private String location;
    private String page;

    @JsonProperty("ResultOnPage")
    private String resultOnPage;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPage() {
        return page;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public String getResultOnPage() {
        return resultOnPage;
    }

    public void setResultOnPage(String resultOnPage) {
        this.resultOnPage = resultOnPage;
    }
}
