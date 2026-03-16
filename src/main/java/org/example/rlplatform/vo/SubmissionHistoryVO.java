package org.example.rlplatform.vo;

import lombok.Data;

@Data
public class SubmissionHistoryVO {

    private Long evaluationId;

    private Integer assignmentId;

    private String taskTitle;

    private String taskMode;

    private Integer studentId;

    private String studentName;

    private String modelName;

    private String submitTime;

    private String status;

    private String opponentName;

    private String resultText;

    private Long evaluationResultId;

    private Boolean hasVideo;
}