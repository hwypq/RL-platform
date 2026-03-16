package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.entity.ExperimentAssignment;
import org.example.rlplatform.entity.Result;
import org.example.rlplatform.service.SystemBotService;
import org.example.rlplatform.vo.SystemBotStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@Service
public class SystemBotServiceImpl implements SystemBotService {

    @Value("${system.bot-root:E:/system_model}")
    private String systemBotRoot;

    private final ExperimentAssignmentRepository experimentAssignmentRepository;

    public SystemBotServiceImpl(ExperimentAssignmentRepository experimentAssignmentRepository) {
        this.experimentAssignmentRepository = experimentAssignmentRepository;
    }

    @Override
    public Result<?> uploadBotFiles(Integer assignmentId, String difficulty, MultipartFile config, MultipartFile model) {
        try {
            if (assignmentId == null) {
                return Result.error("assignmentId不能为空");
            }
            if (difficulty == null || difficulty.isBlank()) {
                return Result.error("difficulty不能为空");
            }
            if (config == null || config.isEmpty()) {
                return Result.error("config不能为空");
            }
            if (model == null || model.isEmpty()) {
                return Result.error("model不能为空");
            }

            ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
            if (assignment == null) {
                return Result.error("任务不存在");
            }

            String normalized = normalizeDifficulty(difficulty);
            Path botDir = Paths.get(systemBotRoot, String.valueOf(assignmentId), normalized);
            Files.createDirectories(botDir);

            Path configPath = botDir.resolve("config.json");
            Path modelPath = botDir.resolve("model.pt");

            config.transferTo(configPath);
            model.transferTo(modelPath);

            return Result.success("上传成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public SystemBotStatusVO getBotStatus(Integer assignmentId) {
        SystemBotStatusVO vo = new SystemBotStatusVO();
        vo.setEasy(existsBotPair(assignmentId, "easy"));
        vo.setMedium(existsBotPair(assignmentId, "medium"));
        vo.setHard(existsBotPair(assignmentId, "hard"));
        return vo;
    }

    @Override
    public String getBotAbsoluteDir(Integer assignmentId, String difficulty) {
        String normalized = normalizeDifficulty(difficulty);
        return Paths.get(systemBotRoot, String.valueOf(assignmentId), normalized).toString();
    }

    private boolean existsBotPair(Integer assignmentId, String difficulty) {
        Path dir = Paths.get(systemBotRoot, String.valueOf(assignmentId), difficulty);
        return Files.exists(dir.resolve("config.json")) && Files.exists(dir.resolve("model.pt"));
    }

    private String normalizeDifficulty(String difficulty) {
        String d = difficulty.toLowerCase(Locale.ROOT).trim();
        if (!d.equals("easy") && !d.equals("medium") && !d.equals("hard")) {
            throw new IllegalArgumentException("difficulty只支持 easy / medium / hard");
        }
        return d;
    }
}