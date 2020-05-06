package com.batch.poc.springbatchparallelstepscsvdbxml.config;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.batch.poc.springbatchparallelstepscsvdbxml.entity.Insurance;
import com.batch.poc.springbatchparallelstepscsvdbxml.repo.InsuranceRepo;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Autowired
	private DataSource datasource;

	@Value("${csv1}")
	private String csv1;
	
	@Value("${csv2}")
	private String csv2;
	
	@Value("${insurance}")
	private String insurance;
	
	@Autowired
	private InsuranceRepo repo;
	
	@Bean
	public Job job() {
		return jobBuilderFactory.get("PARALLEL").incrementer(new RunIdIncrementer()).start(splitFlow()).next(step3())
				.build().build();
	}

	@Bean
	public Flow splitFlow() {

		return new FlowBuilder<SimpleFlow>("SPLIT FLOW").split(taskExecutor()).add(flow1(), flow2()).build();
	}

	@Bean
	public Flow flow2() {

		return new FlowBuilder<SimpleFlow>("flow2").start(step2()).build();
	}

	@Bean
	public Flow flow1() {

		return new FlowBuilder<SimpleFlow>("flow1").start(step1()).build();
	}

	@Bean
	public TaskExecutor taskExecutor() {
		SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
		taskExecutor.setConcurrencyLimit(5);
		return taskExecutor;
	}

	@Bean
	public Step step1() {

		return stepBuilderFactory.get("CSV1 TO DB").<Insurance, Insurance>chunk(100).reader(reader(csv1)).writer(writer())
				.build();
	}

	@Bean
	public Step step2() {

		return stepBuilderFactory.get("CSV2 TO DB").<Insurance, Insurance>chunk(100).reader(reader(csv2)).writer(writer())
				.build();
	}

	@Bean
	public Step step3() {

		return stepBuilderFactory.get("DB TO XML")
				.<Insurance, Insurance>chunk(100)
				.reader(dbReader())
				.writer(xmlWriter())
				.build();
	}

	
	@Bean
	@Transactional(isolation = Isolation.SERIALIZABLE)
	public ItemWriter<Insurance> writer() {
		
		return (insurances)->{
			System.out.println(insurances);
			repo.saveAll(insurances);
		};
	}

	@Bean
	public FlatFileItemReader<Insurance> reader(String file) {
		FlatFileItemReader<Insurance> itemReader = new FlatFileItemReader<>();
		itemReader.setName("CSV READER");
		itemReader.setResource(new ClassPathResource(file));
		itemReader.setLinesToSkip(1);
		itemReader.setLineMapper(lineMapper());
		return itemReader;
	}

	@Bean
	public LineMapper<Insurance> lineMapper() {
		DefaultLineMapper<Insurance> lineMapper = new DefaultLineMapper<>();
		lineMapper.setLineTokenizer(tokenizer());
		lineMapper.setFieldSetMapper(fieldSetMapper());
		return lineMapper;
	}

	@Bean
	public BeanWrapperFieldSetMapper<Insurance> fieldSetMapper() {
		BeanWrapperFieldSetMapper<Insurance> fieldSetMapper = new BeanWrapperFieldSetMapper<Insurance>();
		fieldSetMapper.setTargetType(Insurance.class);
		return fieldSetMapper;
	}

	@Bean
	public LineTokenizer tokenizer() {
		DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
		tokenizer.setDelimiter(",");
		tokenizer.setStrict(false);
		tokenizer.setNames(new String[] {"policyID", "statecode", "county", "line", "construction", "point_granularity"});
		return tokenizer;
	}
	
	@Bean
	@StepScope
	public JdbcCursorItemReader<Insurance> dbReader(){
		JdbcCursorItemReader<Insurance> itemReader = new JdbcCursorItemReader<>();
		itemReader.setDataSource(datasource);
		itemReader.setSql("select policy_id, statecode, county, line, construction, point_granularity from insurance");
		itemReader.setRowMapper(new RowMapper<Insurance>() {		
			@Override
			public Insurance mapRow(ResultSet rs, int rowNum) throws SQLException {
				Insurance insurance = new Insurance();
				insurance.setPolicyID(rs.getLong("policy_id"));
				insurance.setStatecode(rs.getString("statecode"));
				insurance.setCounty(rs.getString("county"));
				insurance.setLine(rs.getString("line"));
				insurance.setConstruction(rs.getString("construction"));
				insurance.setPoint_granularity(rs.getInt("point_granularity"));
				
				return insurance;
			}
		});
		
		return itemReader;
	}
	
	
	@Bean
	public StaxEventItemWriter<Insurance> xmlWriter() {
		return new StaxEventItemWriterBuilder<Insurance>()
				.name("Insurance-Writer")
				.marshaller(insuranceMarshaller())
				.resource(new ClassPathResource(insurance))
				.rootTagName("insurance")
				.overwriteOutput(true)
				.build();

	}
	
	@Bean
	public Jaxb2Marshaller insuranceMarshaller() {	
		Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
		jaxb2Marshaller.setClassesToBeBound(Insurance.class);
		return jaxb2Marshaller;
	}
	

}
