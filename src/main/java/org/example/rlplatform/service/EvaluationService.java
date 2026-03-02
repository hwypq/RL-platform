package org.example.rlplatform.service;

import org.example.rlplatform.entity.Evaluation;

public interface EvaluationService {
    void createEvaluation(Evaluation evaluation);

    Evaluation getEvaluationById(long id);

    void runEvaluation(Long evaluationId);

    void runEvaluationAsync(long evaluationId);
}
