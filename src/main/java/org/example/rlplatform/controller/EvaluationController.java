package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @PostMapping
    public Result<Void> submitEvaluation(@RequestBody Evaluation evaluation) {
        evaluationService.createEvaluation(evaluation);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Evaluation> getEvaluationById(@PathVariable long id) {
        return Result.success(evaluationService.getEvaluationById(id));
    }

    /** 触发一次 Python 评测（如 half-cheetah + DDPG），同步执行并返回更新后的评测结果 */
    @PostMapping("/{id}/run")
    public Result<Evaluation> runEvaluation(@PathVariable long id) {
        return Result.success(evaluationService.runEvaluation(id));
    }
}
