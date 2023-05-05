package com.example.googledrive.service;

import com.example.googledrive.connection.H2DataBaseConnection;
import com.example.googledrive.dto.DataBaseDto;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriveQuickstart {
    private final H2DataBaseConnection connection;

    @Value("${google.app.client.id}")
    private String APPLICATION_CLIENT_ID;
    @Value("${google.app.client.secret}")
    private String APPLICATION_CLIENT_SECRET;
    private static final String APPLICATION_NAME = "googleDrive2";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";

    private static final List<String> SCOPES =
            List.of(DriveScopes.DRIVE_METADATA_READONLY, DriveScopes.DRIVE,
                    "https://www.googleapis.com/auth/userinfo.profile",
                    DriveScopes.DRIVE_FILE);

    public Credential getCredentials(NetHttpTransport HTTP_TRANSPORT) {
        String refreshToken = connection.getRefreshToken();
        return new GoogleCredential.Builder()
                .setTransport(HTTP_TRANSPORT)
                .setJsonFactory(JSON_FACTORY)
                .setClientSecrets(APPLICATION_CLIENT_ID, APPLICATION_CLIENT_SECRET)
                .build()
                .setRefreshToken(refreshToken);
    }

    private Drive getDriveService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public void performBackup(java.io.File file, String fileName, DataBaseDto dataBaseDto) throws GeneralSecurityException, IOException {
        Drive service = getDriveService();
        File root = service.files().get("root").execute();
        String folderId = createFolders(service, dataBaseDto, root.getId());

//create file
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(folderId));
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
            connection.addRefreshToken(credential.getRefreshToken());
        } catch (TokenResponseException e) {
            System.err.println("Error exchanging authorization code for token");
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return "Success";
    }

    public boolean refreshTokenIsValid() throws IOException {
        String refreshToken = connection.getRefreshToken();
        if (refreshToken == null) {
            return false;
        }
        String url = "https://oauth2.googleapis.com/token";
        String params = "grant_type=refresh_token" +
                "&client_id=" + APPLICATION_CLIENT_ID +
                "&client_secret=" + APPLICATION_CLIENT_SECRET +
                "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        con.getOutputStream().write(params.getBytes(StandardCharsets.UTF_8));

        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        if (status == HttpURLConnection.HTTP_OK) {
            System.out.println("Refresh token is valid.");
            return true;
        }
        System.out.println("Refresh token is invalid.");
        return false;
    }

    private String createFolder(Drive service, String folderName, String parentFolderId) throws IOException {
        String mimeType = "application/vnd.google-apps.folder";
        File directory = new File();
        directory.setName(folderName);
        if (parentFolderId != null) {
            directory.setParents(Collections.singletonList(parentFolderId));
        }
        directory.setMimeType(mimeType);
        File createdDirectory = service.files().create(directory).setFields("id").execute();
        return createdDirectory.getId();
    }

    private static List<File> getSubFolders(Drive service, String parentId) throws IOException {
        String query = "mimeType='application/vnd.google-apps.folder' and trashed = false and '" + parentId + "' in parents";

        FileList result = service.files().list().setQ(query).setFields("nextPageToken, files(id, name)").execute();

        List<File> subFolders = new ArrayList<>(result.getFiles());

        while (result.getNextPageToken() != null) {
            result = service.files().list().setQ(query).setFields("nextPageToken, files(id, name)").setPageToken(result.getNextPageToken()).execute();
            subFolders.addAll(result.getFiles());
        }

        return subFolders;
    }

    private String createFolders(Drive service, DataBaseDto dto, String parentFolderId) throws IOException {
        if (dto.getFolderName() == null) {
            return createOrGetFolder(service, dto.getDataBaseName(), parentFolderId);
        }
        if (dto.getFolderName().contains("/")) {
            String folderName = dto.getFolderName() + "/" + dto.getDataBaseName();
            String[] split = folderName.split("/");
            for (String s : split) {
                parentFolderId = createOrGetFolder(service, s, parentFolderId);
            }
        } else {
            String folderId = createOrGetFolder(service, dto.getFolderName(), parentFolderId);
            createOrGetFolder(service, dto.getDataBaseName(), folderId);
        }
        return parentFolderId;
    }

    private String createOrGetFolder(Drive service, String folderName, String parentFolderId) throws IOException {
        List<File> subFolders = getSubFolders(service, parentFolderId);
        for (File subFolder : subFolders) {
            if (subFolder.getName().equals(folderName)) {
                return subFolder.getId();
            }
        }
        return createFolder(service, folderName, parentFolderId);
    }

}