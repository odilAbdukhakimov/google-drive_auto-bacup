package com.example.googledrive;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GoogleDriveApplication {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(GoogleDriveApplication.class, args);
    }

    public static void restart() {
        if (context == null) {
            SpringApplication.run(GoogleDriveApplication.class);
        } else {
            ApplicationArguments args = context.getBean(ApplicationArguments.class);

            Thread thread = new Thread(() -> {
                context.close();
                context = SpringApplication.run(GoogleDriveApplication.class, args.getSourceArgs());
            });

            thread.setDaemon(false);
            thread.start();
        }
    }


}
