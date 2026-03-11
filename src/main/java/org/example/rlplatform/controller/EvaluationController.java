package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    /**
     * 仅供测试，Evaluation创建现需通过AssignmentController的createEvaluation方法完成
     */
    @PostMapping
    public Result<Void> submitEvaluation(@RequestBody Evaluation evaluation) {
        evaluationService.createEvaluation(evaluation);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Evaluation> getEvaluationById(@PathVariable Long id) {
        return Result.success(evaluationService.getEvaluationById(id));
    }

    @PostMapping("/{id}/run")
    public Result<Void> runEvaluation(@PathVariable Long id) {
        evaluationService.runEvaluationAsync(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<Page<Evaluation>> list(
        @RequestParam(defaultValue = "0") Integer pageNum,
        @RequestParam(defaultValue = "10") Integer pageSize,
        @RequestParam(required = false) Integer assignmentId,
        @RequestParam(required = false) Integer studentId,
        @RequestParam(required = false) String status
    ){
        return Result.success(evaluationService.list(pageNum, pageSize, assignmentId, studentId, status));
    }
}
