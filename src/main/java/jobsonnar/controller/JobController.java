package jobsonnar.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.service.JobService;


@RestController
@RequestMapping
public class JobController {
    
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping(value = "/jobs")
    public List<JobResponseDto> getJobs(@RequestParam String query) {
        return jobService.searchJobs(query);
    }
}
