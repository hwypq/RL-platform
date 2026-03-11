package org.example.rlplatform.service.impl;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.evaluation.EvaluationExecuter;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.criteria.Predicate;

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
        evaluationRepository.save(evaluation);
    }

    @Override
    public Evaluation getEvaluationById(long id) {
        return evaluationRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public void runEvaluation(Long evaluationId) {
        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.setErrorMessage(null);
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

    @Override
    public Page<Evaluation> list(Integer pageNum, Integer pageSize, Integer assignmentId, Integer studentId, String status) {
        Specification<Evaluation> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (assignmentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("assignmentId"), assignmentId));
            }
            if (studentId != null) {
                predicates.add(criteriaBuilder.equal(root.get("studentId"), studentId));
            }
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return evaluationRepository.findAll(spec, PageRequest.of(pageNum, pageSize));
    }

}
