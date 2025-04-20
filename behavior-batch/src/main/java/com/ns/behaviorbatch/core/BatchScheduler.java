package com.ns.behaviorbatch.core;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@Configuration
public class BatchScheduler {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job analyzeInappropriateBehaviorJob;

    @Scheduled(cron = "0 0 0 * * ?")  // 매일 자정
    public void runBatchJob() throws JobExecutionException {
        jobLauncher.run(analyzeInappropriateBehaviorJob, new JobParameters());
    }
}

