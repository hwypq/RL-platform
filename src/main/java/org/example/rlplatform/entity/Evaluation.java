package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.w3c.dom.Text;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name="evaluation")
public class Evaluation {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String environment;

    @Column(name = "model_id", nullable = false)
    private Integer modelId;

    @Column(nullable = false)
    private Integer episodes;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING / RUNNING / FINISHED

    @Column(name = "error_message", columnDefinition = "Text")
    private String errorMessage;

    @Column(name="create_time")
    private LocalDateTime createTime;

    @Column(name="update_time")
    private LocalDateTime updateTime;
}
