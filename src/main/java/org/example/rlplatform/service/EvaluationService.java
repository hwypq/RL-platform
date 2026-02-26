package org.example.rlplatform.service;

import org.example.rlplatform.entity.Evaluation;

public interface EvaluationService {
    void createEvaluation(Evaluation evaluation);

    Evaluation getEvaluationById(long id);

    /** 调用 Python 脚本执行评测（如 half-cheetah + DDPG），更新结果并保存。 */
    Evaluation runEvaluation(long evaluationId);
}
