package com.project.jobsonnar.dto;

public class JobResponseDto {
    private String name;
    private String city;
    private String jobUrl;
    private String publishedDate;


    public JobResponseDto(String name, String city, String jobUrl, String publishedDate) {
            this.name = name;
            this.city = city;
            this.jobUrl = jobUrl;
            this.publishedDate = publishedDate;
        }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public String getPublishedDate() {
        return publishedDate;
    }
}
