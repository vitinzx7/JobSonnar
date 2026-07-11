package com.project.jobsonnar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.project.jobsonnar.dto.JobResponseDto;
import com.project.jobsonnar.service.JobService;


@RestController
@RequestMapping
public class JobController {
    
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping(value = "/jobs")
    public List<JobResponseDto> getJobs(@RequestParam String query) {
        return jobService.searchJobs(query);
    }
}
