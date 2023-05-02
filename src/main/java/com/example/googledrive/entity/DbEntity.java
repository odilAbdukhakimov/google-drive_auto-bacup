package com.example.googledrive.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class DbEntity {
    @Id
    @GeneratedValue
    private int id;
    @Column(unique = true)
    private String databaseName;
    private LocalDateTime addedTime;
}
