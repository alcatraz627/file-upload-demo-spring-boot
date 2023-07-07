package com.alcatraz.fileuploaddemo;

import com.alcatraz.fileuploaddemo.storage.StorageProperties;
import com.alcatraz.fileuploaddemo.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class FileuploaddemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileuploaddemoApplication.class, args);
	}

	@Bean
	CommandLineRunner init(StorageService storageService) {
		return (args) -> {
//			storageService.deleteAll();
			storageService.init();
		};
	}

}
