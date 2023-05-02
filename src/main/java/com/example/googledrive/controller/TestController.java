package com.example.googledrive.controller;

import com.example.googledrive.entity.DbEntity;
import com.example.googledrive.service.DataBaseService;
import com.example.googledrive.service.DriveQuickstart;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/")
@RequiredArgsConstructor
public class TestController {
    private final DataBaseService dataBaseService;
    private final DriveQuickstart driveQuickstart;

    @GetMapping("all-dbList")
    public List<String> getDbList() {
        return dataBaseService.getAllDatabaseList();
    }

    @GetMapping("select-dbList")
    public List<DbEntity> getDbListSelected() {
        return dataBaseService.getSelectedDataBase();
    }

    @SneakyThrows
    @PostMapping("add-database")
    public void addDataBase(@RequestBody Map<String, List<String>> dataBases, HttpServletResponse response) {
        checkAuth(response);
        dataBaseService.addDataBase(dataBases);
    }

    @GetMapping("auth")
    public String authenticateUser(HttpServletResponse response) throws Exception {
        response.sendRedirect("https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=1063621039655-jplll5pmunep9fdra21ukikbq1psk3c9.apps.googleusercontent.com&redirect_uri=http://localhost:8080/Callback&response_type=code&scope=https://www.googleapis.com/auth/drive.metadata.readonly%20https://www.googleapis.com/auth/drive%20https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/drive.file&approval_prompt=force");
        return "Already authenticated";
    }

    @GetMapping("Callback")
    public String callback(@RequestParam String code) throws GeneralSecurityException, IOException {
        return driveQuickstart.callback(code);
    }

    @DeleteMapping("del/{id}")
    public void deleteDataBase(@PathVariable Integer id) {
        dataBaseService.deleteDataBase(id);
    }

    private void checkAuth(HttpServletResponse response) throws IOException, GeneralSecurityException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = driveQuickstart.getCredentials(HTTP_TRANSPORT);
        if (credential == null)
            response.sendRedirect("https://accounts.google.com/o/oauth2/auth?access_type=offline&client_id=1063621039655-jplll5pmunep9fdra21ukikbq1psk3c9.apps.googleusercontent.com&redirect_uri=http://localhost:8080/Callback&response_type=code&scope=https://www.googleapis.com/auth/drive.metadata.readonly%20https://www.googleapis.com/auth/drive%20https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/drive.file");

    }
}
