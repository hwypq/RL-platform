package org.example.rlplatform.service;

import org.example.rlplatform.entity.ExperimentAssignment;

import org.springframework.data.domain.Page;

import javax.naming.CannotProceedException;

public interface ExperimentAssignmentService {

    Page<ExperimentAssignment> listStuAssignments(Integer pageNum, Integer pageSize);

    void create(Integer classId, ExperimentAssignment experimentAssignment);

    void update(Integer assignmentId, ExperimentAssignment experimentAssignment);

    Page<ExperimentAssignment> listTeaAssignments(Integer pageNum, Integer pageSize);

    Page<ExperimentAssignment> listAssignmentsByClass(Integer classId, Integer pageNum, Integer pageSize);

    ExperimentAssignment getById(Integer assignmentId);
}
