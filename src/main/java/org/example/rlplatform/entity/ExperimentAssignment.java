package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "experiment_assignment")
public class ExperimentAssignment {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private StudentClass studentClass;

    @Column(name = "teacher_id", nullable = false)
    private Integer teacherId;

    @Column(nullable = false)
    private String title;

    @Transient
    private ExperimentConfig config;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "evaluation_mode", nullable = false)
    @Enumerated(EnumType.STRING)
    private EvaluationMode evaluationMode = EvaluationMode.SINGLE;

    @Column(name = "agent_name")
    private String agentName;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name="create_time")
    private LocalDateTime createTime;

    @Column(name="update_time")
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;
}
