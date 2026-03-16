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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class ExperimentAssignmentController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExperimentAssignmentService experimentAssignmentService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @PostMapping("class/{classId}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> create(@PathVariable Integer classId, @RequestBody ExperimentAssignment experimentAssignment) {
        experimentAssignmentService.create(classId, experimentAssignment);
        return Result.success();
    }

    @PatchMapping("assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> update(@PathVariable Integer assignmentId, @RequestBody ExperimentAssignment experimentAssignment) {
        experimentAssignmentService.update(assignmentId, experimentAssignment);
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

    @PostMapping("assignments/{assignmentId}/evaluations")
    public Result<Void> createEvaluation(@PathVariable Integer assignmentId, @RequestBody Evaluation evaluation) {

        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer studentId = (Integer) claims.get("id");
        checkCooldown(studentId, assignmentId.longValue(), 10L); // 10 秒冷却
        User user = userService.findByIdAndIsDeletedFalse(studentId);
        evaluation.setStudentId(studentId);
        evaluation.setAssignmentId(assignmentId);
        evaluation.setEnvironment(experimentAssignmentService.getById(assignmentId).getEnvironment());
        evaluationService.createEvaluation(evaluation);
        evaluationService.runEvaluation(evaluation.getId());
        return Result.success();
    }

    public void checkCooldown(Integer studentId, Long assignmentId, long cooldownSeconds) {
        String key = "eval:cooldown:" + studentId + ":" + assignmentId;
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", cooldownSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(success)) {
            throw new RuntimeException("操作过于频繁，请稍后再试");
        }
    }
}
