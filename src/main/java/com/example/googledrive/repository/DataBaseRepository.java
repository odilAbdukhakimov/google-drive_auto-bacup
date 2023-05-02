package com.example.googledrive.repository;

import com.example.googledrive.entity.DbEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DataBaseRepository extends JpaRepository<DbEntity, Integer> {
    Optional<DbEntity> findByDatabaseName(String dbName);
}
