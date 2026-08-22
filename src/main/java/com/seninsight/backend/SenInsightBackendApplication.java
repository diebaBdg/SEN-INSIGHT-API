package com.seninsight.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SenInsightBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(SenInsightBackendApplication.class, args);
		displayStartupInfo();
	}

	private static void displayStartupInfo() {
		System.out.println("\n" +
				"SenInsight Backend API démarrée !\n" +
				"API Docs: http://localhost:8080/api/swagger-ui.html");
	}
}