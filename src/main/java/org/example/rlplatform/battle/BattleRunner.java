package org.example.rlplatform.battle;

import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class BattleRunner {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Autowired
    private BattleParticipantRepository battleParticipantRepository;

    @Autowired
    private BattleExecuter battleExecuter;

    @Async("evaluationExecutor")
    public void runBattleAsync(Long evaluationId) {
        Evaluation evaluation = evaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("evaluation not found: " + evaluationId));

        Optional<BattleParticipant> participantOpt = battleParticipantRepository.findByEvaluationId(evaluationId);
        if (participantOpt.isEmpty()) {
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("battle participant not found");
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);
            return;
        }

        evaluation.setStatus(EvaluationStatus.RUNNING);
        evaluation.setErrorMessage(null);
        evaluation.setUpdateTime(LocalDateTime.now());
        evaluationRepository.save(evaluation);

        battleExecuter.executeBattle(evaluation, participantOpt.get());

        evaluation.setUpdateTime(LocalDateTime.now());
        evaluationRepository.save(evaluation);
    }
}