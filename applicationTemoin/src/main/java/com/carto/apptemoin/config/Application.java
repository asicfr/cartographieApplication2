package com.carto.apptemoin.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Configuration
@SpringBootApplication
@ComponentScan("com.carto.apptemoin")
public class Application {
	
	private final static Logger log = LoggerFactory.getLogger(Application.class.getClass());
	
	public static void main(String[] args) {
		log.info(">>> Starting app");
		SpringApplication.run(Application.class, args);
	}

}