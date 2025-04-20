package com.ns.behaviorbatch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.ns.behaviorbatch.core.ElasticsearchReader;
import com.ns.behaviorbatch.core.InappropriateBehaviorProcessor;
import com.ns.behaviorbatch.core.InappropriateBehaviorWriter;
import com.ns.behaviorbatch.domain.Alert;
import com.ns.behaviorbatch.domain.LogDocument;
import com.ns.behaviorbatch.service.InappropriateBehaviorService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final PlatformTransactionManager transactionManager;
    private final JobRepository jobRepository;
    private final ElasticsearchClient elasticsearchClient;
    private final InappropriateBehaviorService inappropriateBehaviorService;

    @Bean
    public Step analyzeInappropriateBehaviorStep() {
        return new StepBuilder("analyzeInappropriateBehaviorStep", jobRepository)
                .<LogDocument, Alert>chunk(100, transactionManager)
                .reader(elasticsearchReader())
                .processor(inappropriateBehaviorProcessor())
                .writer(inappropriateBehaviorWriter())
                .build();
    }

    @Bean
    public Job analyzeInappropriateBehaviorJob() {
        return new JobBuilder("analyzeInappropriateBehaviorJob", jobRepository)
                .start(analyzeInappropriateBehaviorStep())
                .build();
    }

    @Bean
    public ItemReader<LogDocument> elasticsearchReader() {
        return new ElasticsearchReader(elasticsearchClient);
    }

    @Bean
    public ItemProcessor<LogDocument, Alert> inappropriateBehaviorProcessor() {
        return new InappropriateBehaviorProcessor(inappropriateBehaviorService);
    }

    @Bean
    public ItemWriter<Alert> inappropriateBehaviorWriter() {
        return new InappropriateBehaviorWriter(elasticsearchClient);
    }
}
