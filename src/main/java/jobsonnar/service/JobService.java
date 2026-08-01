package jobsonnar.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jobsonnar.JobResponse;
import jobsonnar.dto.JobResponseDto;
import jobsonnar.model.Job;
import jobsonnar.provider.JoobleJobProvider;
import java.net.URLEncoder;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class JobService {
    private final JoobleJobProvider joobleJobProvider;

    public JobService(JoobleJobProvider joobleJobProvider) {
        this.joobleJobProvider = joobleJobProvider;
    }

    public List<JobResponseDto> searchJobs(String query) {

        RestClient client = RestClient.create();

		JobResponse response = client.get()
				.uri("https://employability-portal.gupy.io/api/v1/jobs?jobName=" + URLEncoder.encode(query, StandardCharsets.UTF_8))
				.retrieve()
				.body(JobResponse.class);

        List<JobResponseDto> gupyJobs = safeJobs(response).stream()
        .limit(5)
        .map(job -> new JobResponseDto(
                job.getName(),
                job.getCity(),
                job.getJobUrl(),
                job.getPublishedDate()
        ))
        .toList();

        List<JobResponseDto> joobleJobs = joobleJobProvider.searchJobs(query);
        List<JobResponseDto> allJobs = new ArrayList<>(gupyJobs);

        allJobs.addAll(joobleJobs);
        return allJobs;
	}

	public static List<Job> safeJobs(JobResponse response) {
		if (response.getData() == null) {
			return List.of();
		}
		return response.getData();
	}

    
}
