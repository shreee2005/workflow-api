package com.workflow.demo;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRabbit
public class WorkflowApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorkflowApiApplication.class, args);
	}

}
