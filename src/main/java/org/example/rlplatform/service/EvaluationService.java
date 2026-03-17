package org.example.rlplatform.service;

import org.example.rlplatform.entity.Evaluation;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

public interface EvaluationService {
    void createEvaluation(Evaluation evaluation);

    Evaluation getEvaluationById(long id);

    void runEvaluation(Long evaluationId);

    void runEvaluationAsync(long evaluationId);

    void runEvaluationByConfig(Integer assignmentId, MultipartFile model, MultipartFile config);

    Page<Evaluation> list(Integer pageNum, Integer pageSize, Integer assignmentId, Integer studentId, String status);
}
