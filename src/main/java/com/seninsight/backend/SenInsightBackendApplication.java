package com.seninsight.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SenInsightBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(SenInsightBackendApplication.class, args);
		displayStartupInfo();
	}

	private static void displayStartupInfo() {
		System.out.println("\n" +
				"SEN-INSIGHT  Backend API démarrée !\n" +
				"API Docs: http://localhost:9292/seninsightbackend/swagger-ui/index.html");
	}
}