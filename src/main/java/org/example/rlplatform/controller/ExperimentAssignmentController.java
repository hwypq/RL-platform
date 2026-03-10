package org.example.rlplatform.controller;

import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.ExperimentAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
public class ExperimentAssignmentController {

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
}
