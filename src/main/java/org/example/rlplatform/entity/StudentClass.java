package org.example.rlplatform.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="student_class")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
