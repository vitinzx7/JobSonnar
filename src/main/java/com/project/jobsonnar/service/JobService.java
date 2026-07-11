package com.project.jobsonnar.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.project.jobsonnar.JobResponse;
import com.project.jobsonnar.dto.JobResponseDto;
import com.project.jobsonnar.model.Job;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class JobService {

    public List<JobResponseDto> searchJobs(String query) {

        RestClient client = RestClient.create();

		JobResponse response = client.get()
				.uri("https://employability-portal.gupy.io/api/v1/jobs?jobName=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
				.retrieve()
				.body(JobResponse.class);

        return safeJobs(response).stream()
        .limit(5)
        .map(job -> new JobResponseDto(
                job.getName(),
                job.getCity(),
                job.getJobUrl(),
                job.getPublishedDate()
        ))
        .toList();
	}

	public static List<Job> safeJobs(JobResponse response) {
		if (response.getData() == null) {
			return List.of();
		}
		return response.getData();
	}
    
}
