package com.example.googledrive.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.people.v1.PeopleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class DriveQuickstart {

    @Value("${google.app.client.id}")
    private String APPLICATION_CLIENT_ID;
    @Value("${google.app.client.secret}")
    private String APPLICATION_CLIENT_SECRET;
    private String REFRESH_TOKEN;

    private static final String APPLICATION_NAME = "googleDrive2";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES =
            List.of(DriveScopes.DRIVE_METADATA_READONLY, DriveScopes.DRIVE,
                    "https://www.googleapis.com/auth/userinfo.profile",
                    DriveScopes.DRIVE_FILE);

    public Credential getCredentials(NetHttpTransport HTTP_TRANSPORT) {
        return new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(APPLICATION_CLIENT_ID, APPLICATION_CLIENT_SECRET)
                .build()
                .setRefreshToken(REFRESH_TOKEN);
    }

    private Drive getDriveService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void getUser() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        PeopleService peopleService = new PeopleService.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).build();
        peopleService.people().get("people/me").setPersonFields("names,emailAddresses").execute();
    }


    private void deleteFile(Drive service, String fileId) {
        try {
            service.files().delete(fileId).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
        }
    }

    private void createFile(Drive service, String filePathName, String fileName, String contentType) throws IOException {
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        java.io.File filePath = new java.io.File(filePathName);
        FileContent mediaContent = new FileContent(contentType, filePath);
        try {
            File file = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + file.getId());
        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to upload file: " + e.getDetails());
            throw e;
        }
    }

    public void performBackup(java.io.File file, String fileName) throws GeneralSecurityException, IOException {
        Drive service = getDriveService();
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        FileContent mediaContent = new FileContent("application/octet-stream", file);
        try {
            File fileR = service.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();
            System.out.println("File ID: " + fileR.getId());
        } catch (GoogleJsonResponseException e) {
            System.err.println("Unable to upload file: " + e.getDetails());
            throw e;
        }
    }

    private void getList(Drive service) throws IOException {
        FileList result = service.files().list()
                .setPageSize(40)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (File a : files) {
                System.out.printf("%s (%s)\n", a.getName(), a.getId());
            }
        }
    }

    public String callback(String code) throws GeneralSecurityException, IOException {
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, APPLICATION_CLIENT_ID, APPLICATION_CLIENT_SECRET, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        try {
            TokenResponse response = flow.newTokenRequest(code)
                    .setRedirectUri("http://localhost:8080/Callback") // The same redirect URI that was used in the authorization request
                    .execute();

            Credential credential = flow.createAndStoreCredential(response, "user");
            System.out.println("Access token: " + credential.getAccessToken());
            System.out.println("Refresh token: " + credential.getRefreshToken());
            REFRESH_TOKEN = credential.getRefreshToken();
        } catch (TokenResponseException e) {
            System.err.println("Error exchanging authorization code for token");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "Success";
    }
}