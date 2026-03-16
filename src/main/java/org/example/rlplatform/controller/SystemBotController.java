package org.example.rlplatform.controller;

import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.SystemBotService;
import org.example.rlplatform.vo.SystemBotStatusVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/assignments")
public class SystemBotController {

    private final SystemBotService systemBotService;

    public SystemBotController(SystemBotService systemBotService) {
        this.systemBotService = systemBotService;
    }

    @PostMapping("/{assignmentId}/system-bots/{difficulty}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<?> uploadBotFiles(
            @PathVariable Integer assignmentId,
            @PathVariable String difficulty,
            @RequestParam("config") MultipartFile config,
            @RequestParam("model") MultipartFile model
    ) {
        return systemBotService.uploadBotFiles(assignmentId, difficulty, config, model);
    }

    @GetMapping("/{assignmentId}/system-bots/status")
    public Result<SystemBotStatusVO> getBotStatus(@PathVariable Integer assignmentId) {
        return Result.success(systemBotService.getBotStatus(assignmentId));
    }
}