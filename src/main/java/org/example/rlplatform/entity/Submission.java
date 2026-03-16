package org.example.rlplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {
    private Integer assignmentId;
    private Long studentId;
    private String environment;
    private Integer games;
    private String studentDirRel;
    private String modelName;
    private LocalDateTime submitTime;
}