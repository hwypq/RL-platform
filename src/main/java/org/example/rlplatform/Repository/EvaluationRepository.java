package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;


public interface EvaluationRepository extends JpaRepository<Evaluation, Long>, JpaSpecificationExecutor<Evaluation> {
    
    @Query(value = """
         SELECT
          e.student_id AS studentId,
          MAX(
            CAST(
              JSON_UNQUOTE(JSON_EXTRACT(er.detailed_results, '$."avgReward"'))
              AS DECIMAL(10, 4)
            )
          ) AS bestScore
        FROM evaluation e
        JOIN evaluation_result er ON er.evaluation_id = e.id
        WHERE e.assignment_id = :assignmentId
          AND e.status = 'FINISHED'
        GROUP BY e.student_id
        ORDER BY bestScore DESC
        LIMIT :limit OFFSET :offset
        """, nativeQuery = true)
    List<LeaderBoardRow> leaderboardByAssignmentId(
        @Param("assignmentId") Integer assignmentId,
        @Param("limit") Integer limit,
        @Param("offset") Integer offset
    );

    @Query(value = """
        SELECT COUNT(DISTINCT e.student_id)
        FROM evaluation e
        JOIN evaluation_result er ON er.evaluation_id = e.id
        WHERE e.assignment_id = :assignmentId
          AND e.status = 'FINISHED'
        """, nativeQuery = true)
    Integer countLeaderboardByAssignment(@Param("assignmentId") Integer assignmentId);
}
