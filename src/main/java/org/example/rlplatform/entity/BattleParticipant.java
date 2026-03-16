package org.example.rlplatform.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "battle_participant")
public class BattleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="evaluation_id", nullable = false, unique = true)
    private Long evaluationId;

    @Column(name="student1_id", nullable = false)
    private Long student1Id;

    @Column(name="student2_id")
    private Long student2Id;

    @Column(name="student1_dir_rel", nullable = false, length = 500)
    private String student1DirRel;

    @Column(name="student2_dir_rel", nullable = false, length = 500)
    private String student2DirRel;

    @Column(name = "opponent_type", length = 20)
    private String opponentType; // HUMAN / BOT

    @Column(name = "opponent_name", length = 100)
    private String opponentName;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "student1_model_name", length = 255)
    private String student1ModelName;

    @Column(name = "student2_model_name", length = 255)
    private String student2ModelName;
}