package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

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

    @Column(nullable = false)
    private Double score;

    @Column(name = "agent_name", nullable = false)
    private String agentName;

    @Column(nullable = false)
    private String environment;

    /** 模型名称，用于加载 models/{modelName}。为空时由 agentName + 环境简称推导，如 DDPG_cheetah */
    @Column(name = "model_name")
    private String modelName;

    @Column(nullable = false)
    private Integer episodes;

    @Column(nullable = false)
    private Double avgReward;

    @Column(nullable = false)
    private String status; // PENDING / RUNNING / FINISHED

    @Column(name = "result_path")
    private String resultPath; // 日志文件路径

    @Column(name="create_time")
    private LocalDateTime createTime;

    @Column(name="update_time")
    private LocalDateTime updateTime;
}
