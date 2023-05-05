package com.example.googledrive.controller;

import com.example.googledrive.dto.CronTimeDto;
import com.example.googledrive.dto.DataBaseDto;
import com.example.googledrive.connection.H2DataBaseConnection;
import com.example.googledrive.service.DriveQuickstart;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;


@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class TestController {
    private final DriveQuickstart driveQuickstart;
    private final H2DataBaseConnection connection;

    @GetMapping("db-list")
    public List<String> getDbListSelected() {
        return connection.getDatabaseNameList();
    }

    @SneakyThrows
    @PostMapping("add-database")
    public void addDataBase(@RequestBody DataBaseDto dataBaseDto, HttpServletResponse response) {
        if (driveQuickstart.refreshTokenIsValid()) {
            connection.addDataBase(dataBaseDto);
        } else {
            response.sendRedirect("https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=495598571641-9uufvvkr8sh1qhkd4ghtu6vfr3mhditl.apps.googleusercontent.com&redirect_uri=http://localhost:8080/Callback&response_type=code&scope=https://www.googleapis.com/auth/drive.metadata.readonly%20https://www.googleapis.com/auth/drive%20https://www.googleapis.com/auth/drive.file&approval_prompt=force");
        }
    }

    @GetMapping("auth")
    public String authenticateUser(HttpServletResponse response) throws Exception {
        if (!driveQuickstart.refreshTokenIsValid())
            response.sendRedirect("https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=495598571641-9uufvvkr8sh1qhkd4ghtu6vfr3mhditl.apps.googleusercontent.com&redirect_uri=http://localhost:8080/Callback&response_type=code&scope=https://www.googleapis.com/auth/drive.metadata.readonly%20https://www.googleapis.com/auth/drive%20https://www.googleapis.com/auth/drive.file&approval_prompt=force");
        return "Already authenticated";
    }

    @GetMapping("Callback")
    public String callback(@RequestParam String code) throws GeneralSecurityException, IOException {
        return driveQuickstart.callback(code);
    }

    @DeleteMapping("del/{dbName}")
    public void deleteDataBase(@PathVariable String dbName) {
        connection.deleteDataBase(dbName);
    }

    @PutMapping("update/cron-time")
    public void updateCronTime(@RequestBody CronTimeDto cronTimeDto) {
        connection.updateCronTime(cronTimeDto);
    }

    @GetMapping("cron")
    public String getCron() {
        return connection.getCronTime();
    }

}
