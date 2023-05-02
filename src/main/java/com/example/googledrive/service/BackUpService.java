package com.example.googledrive.service;

import com.example.googledrive.repository.DataBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class BackUpService {
    private final DriveQuickstart quickstart;
    private final DataBaseRepository dataBaseRepository;

    @Scheduled(cron = "0 * * * * *")
    public void backUp() {
        dataBaseRepository.findAll().forEach((t) ->
                createBackUp(t.getDatabaseName()));
    }

    private String read(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder stringBuilder = new StringBuilder();
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line);
        }
        return stringBuilder.toString();
    }

    private void createBackUp(String dataBaseName) {
        try {
            String backupDirectoryPath = "./backup";
            String dateTimeFormat = "yyyy-MM-dd_HH-mm_";
            LocalDateTime now = LocalDateTime.now();
            int sequencer = 0;
            String fileName;

            File backupDirectory = new File(backupDirectoryPath);
            if (!backupDirectory.exists()) {
                backupDirectory.mkdir();
            }
            File backupFile;
            do {
                fileName = String.format("%s_%s%d.sql", dataBaseName, now.format(DateTimeFormatter.ofPattern(dateTimeFormat)), sequencer);
                backupFile = new File(backupDirectory, fileName);
                sequencer++;
            } while (backupFile.exists());

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "pg_dump",
                    "--dbname=postgresql://postgres:0887@localhost:5432/" + dataBaseName,
                    "--file=" + backupFile
            );
            Process process = processBuilder.start();

            String errorOutput = read(process.getErrorStream());

            if (errorOutput.isEmpty()) {
                File file = new File(backupDirectoryPath, fileName);
                quickstart.performBackup(file, fileName);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}