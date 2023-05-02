package com.example.googledrive;

import com.example.googledrive.service.DriveQuickstart;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoogleDriveApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoogleDriveApplication.class, args);

	}

}