package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringbootRekognitionApplication {

	public static void main(String[] args) {
		//		SpringApplication.run(SpringbootRekognitionApplication.class, args);

		try (ConfigurableApplicationContext ctx = SpringApplication.run(SpringbootRekognitionApplication.class, args)) {
			SpringbootRekognitionApplication app = ctx.getBean(SpringbootRekognitionApplication.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
