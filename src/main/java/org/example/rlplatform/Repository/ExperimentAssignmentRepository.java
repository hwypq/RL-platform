package org.example.rlplatform.Repository;

import org.example.rlplatform.entity.ExperimentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExperimentAssignmentRepository extends JpaRepository<ExperimentAssignment,String> {
    Page<ExperimentAssignment> findByStudentClass_IdAndIsDeletedFalse(Integer studentClassId, Pageable pageable);

    Page<ExperimentAssignment> findByTeacherIdAndIsDeletedFalse(Integer teacherId, Pageable pageable);
}
