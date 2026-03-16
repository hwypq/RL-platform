package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.BattleService;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/battle")
@CrossOrigin
public class BattleController {

    private final BattleService battleService;

    public BattleController(BattleService battleService) {
        this.battleService = battleService;
    }

    @PostMapping("/submit/{assignmentId}")
    public Result<?> submit(
            @PathVariable Integer assignmentId,
            @RequestParam("model") MultipartFile model,
            @RequestParam("config") MultipartFile config
    ) {
        return battleService.submitAndMaybeStart(assignmentId, model, config);
    }

    @PostMapping("/bot-submit/{assignmentId}")
    public Result<?> botSubmit(
            @PathVariable Integer assignmentId,
            @RequestParam("difficulty") String difficulty,
            @RequestParam("model") MultipartFile model,
            @RequestParam("config") MultipartFile config
    ) {
        return battleService.submitBotAndStart(assignmentId, difficulty, model, config);
    }

    @GetMapping("/tasks")
    public Result<List<BattleTaskDetailVO>> listTasks() {
        return Result.success(battleService.listBattleTasks());
    }
}