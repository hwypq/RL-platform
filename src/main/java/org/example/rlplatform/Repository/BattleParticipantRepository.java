package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.BattleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BattleParticipantRepository extends JpaRepository<BattleParticipant, Long> {

    Optional<BattleParticipant> findByEvaluationId(Long evaluationId);

    List<BattleParticipant> findByEvaluationIdIn(List<Long> evaluationIds);

    List<BattleParticipant> findByStudent1IdOrStudent2IdOrderByIdDesc(Long student1Id, Long student2Id);
}