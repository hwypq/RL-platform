package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.entity.User;
import org.example.rlplatform.service.EvaluationService;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
public class ExperimentAssignmentController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExperimentAssignmentService experimentAssignmentService;

    @PostMapping("class/{classId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> create(@PathVariable Integer classId, @RequestBody ExperimentAssignment experimentAssignment) {
        experimentAssignmentService.create(classId, experimentAssignment);
        return Result.success();
    }

    @GetMapping("me/assignments")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<Page<ExperimentAssignment>> listStuAssignments(
        @RequestParam(defaultValue = "0") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(experimentAssignmentService.listStuAssignments(pageNum, pageSize));
    }

    @GetMapping("teacher/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<ExperimentAssignment>> listTeaAssignments(
        @RequestParam(defaultValue = "0") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(experimentAssignmentService.listTeaAssignments(pageNum, pageSize));
    }

    @GetMapping("class/{classId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Page<ExperimentAssignment>> listAssignmentsByClass(
        @PathVariable Integer classId,
        @RequestParam(defaultValue = "0") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(experimentAssignmentService.listAssignmentsByClass(classId, pageNum, pageSize));
    }

    @PostMapping("assignment/{assignmentId}/evaluations")
    public Result<Void> createEvaluation(@PathVariable Integer assignmentId, @RequestBody Evaluation evaluation) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer studentId = (Integer) claims.get("id");
        User user = userService.findByIdAndIsDeletedFalse(studentId);
        evaluation.setStudentId(studentId);
        evaluation.setAssignmentId(assignmentId);
        evaluation.setEnvironment(experimentAssignmentService.getById(assignmentId).getEnvironment());
        evaluationService.createEvaluation(evaluation);
        return Result.success();
    }
}
