package com.example.googledrive.service;

import com.example.googledrive.entity.DbEntity;
import com.example.googledrive.repository.DataBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataBaseService {
    private final DataBaseRepository dataBaseRepository;

    public List<String> getAllDatabaseList() {
        List<String> result = new ArrayList<>();
        try {
            Class.forName("org.postgresql.Driver");
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "postgres", "0887");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT datname FROM pg_database;");
            while (rs.next()) {
                String databaseName = rs.getString("datname");
                result.add(databaseName);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public void addDataBase(Map<String, List<String>> dataBaseList) {
        List<String> list = dataBaseList.get("dataBases");
        list.stream().filter((d) ->
                dataBaseRepository.findByDatabaseName(d).isEmpty()).forEach(this::create);
    }

    private void create(String dataBaseName) {
        DbEntity build = DbEntity.builder()
                .databaseName(dataBaseName)
                .addedTime(LocalDateTime.now())
                .build();
        dataBaseRepository.save(build);
    }

    public List<DbEntity> getSelectedDataBase() {
        return dataBaseRepository.findAll();
    }

    public void deleteDataBase(Integer id) {
        DbEntity dbEntity = dataBaseRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        dataBaseRepository.delete(dbEntity);
    }
}
