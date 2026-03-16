package org.example.rlplatform.vo;

import lombok.Data;

@Data
public class BattleTaskDetailVO {
    private Long evaluationId;
    private String status;
    private Long student1Id;
    private Long student2Id;
    private Integer winner;
    private String winnerText;
    private String resultDir;
}