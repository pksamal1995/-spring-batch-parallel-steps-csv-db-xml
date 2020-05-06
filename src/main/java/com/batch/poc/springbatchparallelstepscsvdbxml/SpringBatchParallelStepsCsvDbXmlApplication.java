package com.batch.poc.springbatchparallelstepscsvdbxml;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.batch.poc.springbatchparallelstepscsvdbxml")
@EnableJpaRepositories(basePackages = "com.batch.poc.springbatchparallelstepscsvdbxml.repo")
@EnableTransactionManagement
public class SpringBatchParallelStepsCsvDbXmlApplication {

	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private Job job;

	public static void main(String[] args) {
		SpringApplication.run(SpringBatchParallelStepsCsvDbXmlApplication.class, args);

	}

	@Bean
	public CommandLineRunner runner() {

		return args -> {
			Map<String, JobParameter> parameters = new HashMap<>();
			parameters.put("time", new JobParameter(System.currentTimeMillis()));
			JobParameters jobParameters = new JobParameters(parameters);

			JobExecution jobExecution = jobLauncher.run(job, jobParameters);

			System.out.println(jobExecution.getStatus().toString());

		};
	}

}
