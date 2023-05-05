package com.example.googledrive.service;

import com.example.googledrive.dto.DataBaseDto;
import com.example.googledrive.connection.H2DataBaseConnection;
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
    private final H2DataBaseConnection connection;

    @Scheduled(cron = "#{h2DataBaseConnection.cronTime}")
    public void backUp() {
        connection.getAllSelectedDataBase().forEach(this::createBackUp);
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

    private void createBackUp(DataBaseDto dataBaseDto) {
        try {
            String backupDirectoryPath = "./backup/" + dataBaseDto.getDataBaseName();
            String dateTimeFormat = "yyyy-MM-dd_HH-mm_";
            LocalDateTime now = LocalDateTime.now();
            int sequencer = 0;
            String fileName;

            File backupDirectory = new File(backupDirectoryPath);
            if (!backupDirectory.exists()) {
                backupDirectory.mkdirs();
            }
            File backupFile;
            do {
                fileName = String.format("%s_%s%d.sql", dataBaseDto.getDataBaseName(), now.format(DateTimeFormatter.ofPattern(dateTimeFormat)), sequencer);
                backupFile = new File(backupDirectory, fileName);
                sequencer++;
            } while (backupFile.exists());

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "pg_dump",
                    String.format("--dbname=postgresql://%s:%s@localhost:5432/%s", dataBaseDto.getUsername(),
                            dataBaseDto.getPassword(), dataBaseDto.getDataBaseName()),
                    "--file=" + backupFile
            );
            Process process = processBuilder.start();

            String errorOutput = read(process.getErrorStream());

            if (errorOutput.isEmpty()) {
                File file = new File(backupDirectoryPath, fileName);
                quickstart.performBackup(file, fileName, dataBaseDto);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}