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

    @PostMapping("/{id}/run")
    public Result<Void> runEvaluation(@PathVariable long id) {
        evaluationService.runEvaluationAsync(id);
        return Result.success();
    }
}
