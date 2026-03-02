package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name="evaluation_result")
public class EvaluationResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(nullable = false)
    private Integer result;

    @Column
    private Integer winner;

    @Column(name = "detailed_results", columnDefinition = "JSON")
    private String detailedResults;

    @Column(name = "result_dir", length = 500)
    private String resultDir;
}
