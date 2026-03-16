package org.example.rlplatform.service;

import org.example.rlplatform.entity.Evaluation;
import org.springframework.data.domain.Page;

public interface EvaluationService {
    void createEvaluation(Evaluation evaluation);

    Evaluation getEvaluationById(long id);

    void runEvaluation(Long evaluationId);

    void runEvaluationAsync(long evaluationId);

    Page<Evaluation> list(Integer pageNum, Integer pageSize, Integer assignmentId, Integer studentId, String status);
}
