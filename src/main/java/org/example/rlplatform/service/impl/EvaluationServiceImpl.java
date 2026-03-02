package org.example.rlplatform.service.impl;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.evaluation.EvaluationExecuter;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static java.time.LocalDateTime.now;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private EvaluationExecuter evaluationExecuter;

    @Override
    public void createEvaluation(Evaluation evaluation) {
        evaluation.setCreateTime(now());
        evaluation.setUpdateTime(now());
        evaluation.setStudentId((long)5);
        evaluationRepository.save(evaluation);
    }

    @Override
    public Evaluation getEvaluationById(long id) {
        return evaluationRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public void runEvaluation(Long evaluationId) {
        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.setStatus("RUNNING");
        evaluation.setUpdateTime(now());
        evaluationRepository.save(evaluation);

        evaluationExecuter.execute(evaluation);

        evaluation.setUpdateTime(now());
        evaluationRepository.save(evaluation);

    }

    @Override
    @Async
    public void runEvaluationAsync(long evaluationId) {
        runEvaluation(evaluationId);
    }

}
