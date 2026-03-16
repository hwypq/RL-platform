package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.SubmissionService;
import org.example.rlplatform.vo.SubmissionHistoryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SubmissionController {

    @Autowired
    private SubmissionService submissionService;

    /**
     * 当前登录学生查看自己的所有提交
     */
    @GetMapping("/me/submissions")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<List<SubmissionHistoryVO>> listMySubmissions() {
        return Result.success(submissionService.listMySubmissions());
    }

    /**
     * 教师 / 管理员查看某个任务下所有学生提交
     */
    @GetMapping("/assignments/{assignmentId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<SubmissionHistoryVO>> listAssignmentSubmissions(@PathVariable Integer assignmentId) {
        return Result.success(submissionService.listAssignmentSubmissions(assignmentId));
    }

    /**
     * 教师 / 管理员查看某个学生对所有任务的提交
     */
    @GetMapping("/students/{studentId}/submissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<SubmissionHistoryVO>> listStudentSubmissions(@PathVariable Integer studentId) {
        return Result.success(submissionService.listStudentSubmissions(studentId));
    }
}