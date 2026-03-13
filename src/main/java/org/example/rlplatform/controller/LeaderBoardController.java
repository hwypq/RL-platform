package org.example.rlplatform.controller;

import org.example.rlplatform.entity.LeaderBoard;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.LeaderBoardService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
public class LeaderBoardController {

    @Autowired
    private LeaderBoardService leaderBoardService;

    @GetMapping("/assignments/{assignmentId}/leaderboard")
    public Result<Page<LeaderBoard>> list(
            @PathVariable Integer assignmentId,
            @RequestParam(defaultValue = "0") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return Result.success(leaderBoardService.list(assignmentId, pageNum, pageSize));
    }
}
