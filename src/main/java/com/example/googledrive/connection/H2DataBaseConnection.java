package com.example.googledrive.connection;

import com.example.googledrive.dto.CronTimeDto;
import com.example.googledrive.dto.DataBaseDto;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Component
public class H2DataBaseConnection {
    @Value("${h2.datasource.url}")
    private String URL;
    @Value("${h2.datasource.username}")
    private String USERNAME;
    @Value("${h2.datasource.password}")
    private String PASSWORD;

    public void createTableIfExist() throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);

        String sql = "CREATE TABLE IF NOT EXISTS database (name VARCHAR(255) UNIQUE, username VARCHAR(255), password VARCHAR(255), folder_name VARCHAR(255));" +
                "CREATE TABLE IF NOT EXISTS refresh_token (id INT PRIMARY KEY, refresh_token VARCHAR(255), create_time TIMESTAMP);" +
                "CREATE TABLE IF NOT EXISTS cron_time (id INT PRIMARY KEY, cron VARCHAR(255));";

        Statement statement = connection.createStatement();
        statement.executeUpdate(sql);

        statement.close();
        connection.close();
    }

    public List<String> getDatabaseNameList() {
        List<String> result = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "SELECT * FROM database";
            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String elementName = resultSet.getString("name");
                result.add(elementName);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public List<DataBaseDto> getAllSelectedDataBase() {
        List<DataBaseDto> result = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)) {
            String sql = "SELECT * FROM database";
            PreparedStatement statement = connection.prepareStatement(sql);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String dbName = resultSet.getString("name");
                String username = resultSet.getString("username");
                String password = resultSet.getString("password");
                String folderName = resultSet.getString("folder_name");
                DataBaseDto dataBaseDto = new DataBaseDto(dbName, username, password, folderName);
                result.add(dataBaseDto);
            }

            resultSet.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void addDataBase(DataBaseDto dataBaseDto) throws SQLException {
        createTableIfExist();
        List<String> list2 = getDatabaseNameList();
        if (!list2.contains(dataBaseDto.getDataBaseName())) {
            insertDataBase(dataBaseDto);
        } else {
            System.out.println("DataBase already exist");
            updateDataBase(dataBaseDto);
        }
    }

    public void deleteDataBase(String dbName) {
        List<String> databaseList = getDatabaseNameList();
        if (databaseList.contains(dbName)) {
            try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                 PreparedStatement pstmt = conn.prepareStatement("DELETE FROM database WHERE name = ?")) {
                pstmt.setString(1, dbName);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void insertDataBase(DataBaseDto dataBase) throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        String sql = "INSERT INTO database (name, username, password, folder_name) VALUES (?,?,?,?)";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, dataBase.getDataBaseName());
        preparedStatement.setString(2, dataBase.getUsername());
        preparedStatement.setString(3, dataBase.getPassword());
        preparedStatement.setString(4, dataBase.getFolderName());

        int rowsInserted = preparedStatement.executeUpdate();
        System.out.println(rowsInserted + " row(s) inserted.");

        preparedStatement.close();
        connection.close();
    }

    private void updateDataBase(DataBaseDto newDataBase) throws SQLException {
        Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        String sql = "UPDATE database SET username=?, password=?, folder_name=? WHERE name=?";

        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, newDataBase.getUsername());
        preparedStatement.setString(2, newDataBase.getPassword());
        preparedStatement.setString(3, newDataBase.getFolderName());
        preparedStatement.setString(4, newDataBase.getDataBaseName());

        int rowsUpdated = preparedStatement.executeUpdate();
        System.out.println(rowsUpdated + " row(s) updated.");

        preparedStatement.close();
        connection.close();

    }

    public void addRefreshToken(String refreshToken) {
        try (
                Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                PreparedStatement pstmt = conn.prepareStatement("MERGE INTO refresh_token (id, refresh_token, create_time) KEY(id) VALUES (?, ?, ?)")) {
            pstmt.setInt(1, 1);
            pstmt.setString(2, refreshToken);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
            System.out.println("Refresh token inserted or updated successfully");
        } catch (
                SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @SneakyThrows
    public String getRefreshToken() {
        createTableIfExist();
        int id = 1;
        String refreshToken = null;
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement("SELECT refresh_token FROM refresh_token WHERE id = ?")) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                refreshToken = rs.getString("refresh_token");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return refreshToken;
    }

    @SneakyThrows
    public void updateCronTime(CronTimeDto cronTimeDto) {
        createTableIfExist();
        CronTimeDto timeDto = checkTime(cronTimeDto);
        try (
                Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                PreparedStatement pstmt = conn.prepareStatement("MERGE INTO cron_time (id, cron) KEY(id) VALUES (?, ?)")) {
            pstmt.setInt(1, 1);
            pstmt.setString(2, String.format(("0 %s %s * * *"), timeDto.getMin(), timeDto.getHour()));
            pstmt.executeUpdate();
            System.out.println("cron time inserted or updated successfully");
        } catch (
                SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @SneakyThrows
    public String getCronTime() {
        int id = 1;
        String cronTime = null;
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement("SELECT cron FROM cron_time WHERE id = ?")) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                cronTime = rs.getString("cron");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return cronTime == null ? "0 0 1 * * *" : cronTime;
    }

    private CronTimeDto checkTime(CronTimeDto cronTimeDto) {
        String hour = cronTimeDto.getHour();
        String min = cronTimeDto.getMin();

        if (isNumber(hour) && Integer.parseInt(hour) < 0) {
            hour = "0";
        } else if (isNumber(hour) && Integer.parseInt(hour) >= 24) {
            hour = String.valueOf((Integer.parseInt(hour) % 24));
        }
        if (isNumber(min) && Integer.parseInt(min) > 60) {
            if (!hour.contains("*")) {
                hour = String.valueOf(Integer.parseInt(hour) + Integer.parseInt(min) / 60);
            }
            min = String.valueOf(Integer.parseInt(min) % 60);
        } else if (isNumber(min) && Integer.parseInt(min) < 0) {
            min = "0";
        }
        cronTimeDto.setHour(hour);
        cronTimeDto.setMin(min);
        return cronTimeDto;
    }

    private boolean isNumber(String number) {
        try {
            Integer.parseInt(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
