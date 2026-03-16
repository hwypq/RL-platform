package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.EvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationResultRepository extends JpaRepository<EvaluationResult, Long> {

    List<EvaluationResult> findByEvaluationId(Long evaluationId);

    List<EvaluationResult> findByEvaluationIdIn(List<Long> evaluationIds);
}