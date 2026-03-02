package org.example.rlplatform.controller;

import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.EvaluationResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/evaluation-results")
public class EvaluationResultController {

    @Autowired
    private EvaluationResultService evaluationResultService;

    @PostMapping
    public Result<Void> create(@RequestBody EvaluationResult evaluationResult) {
        evaluationResultService.create(evaluationResult);
        return Result.success();
    }

    @GetMapping("/id/{id}")
    public Result<EvaluationResult> getById(@PathVariable Long id) {
        return Result.success(evaluationResultService.getById(id));
    }

    @GetMapping
    public Result<List<EvaluationResult>> list(@RequestParam(required = false) Long evaluationId) {
        if (evaluationId != null) {
            return Result.success(evaluationResultService.listByEvaluationId(evaluationId));
        }
        return Result.success(evaluationResultService.list());
    }

    /** 直接返回第一个视频文件流，前端可用 <video src="/evaluation-results/1/video"> 播放 */
    @GetMapping(value = "/{id}/video", produces = "video/mp4")
    public ResponseEntity<Resource> getVideo(@PathVariable Long id) {
        Resource resource = evaluationResultService.getVideo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(resource);
    }

    @GetMapping("/evaluation/{evaluationId}")
    public Result<List<EvaluationResult>> getByEvaluationId(@PathVariable Long evaluationId) {
        return Result.success(evaluationResultService.listByEvaluationId(evaluationId));
    }

}
