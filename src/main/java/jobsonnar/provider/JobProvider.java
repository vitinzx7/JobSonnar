package jobsonnar.provider;

import java.util.List;

import jobsonnar.dto.JobResponseDto;
import jobsonnar.dto.JobSearchRequest;

public interface JobProvider {
    List<JobResponseDto> searchJobs(JobSearchRequest request);
}