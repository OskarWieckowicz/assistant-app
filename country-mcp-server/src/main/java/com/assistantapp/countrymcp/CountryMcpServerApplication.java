package com.assistantapp.countrymcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CountryMcpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CountryMcpServerApplication.class, args);
	}
}
