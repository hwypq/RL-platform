package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.battle.BattleRunner;
import org.example.rlplatform.battle.InMemoryBattleQueue;
import org.example.rlplatform.entity.*;
import org.example.rlplatform.service.BattleService;
import org.example.rlplatform.service.SystemBotService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.BattleTaskDetailVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BattleServiceImpl implements BattleService {

    private final InMemoryBattleQueue battleQueue;
    private final EvaluationRepository evaluationRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final BattleParticipantRepository battleParticipantRepository;
    private final BattleRunner battleRunner;
    private final ExperimentAssignmentRepository experimentAssignmentRepository;
    private final SystemBotService systemBotService;

    @Value("${evaluation.workspace:}")
    private String workspace;

    public BattleServiceImpl(
            InMemoryBattleQueue battleQueue,
            EvaluationRepository evaluationRepository,
            EvaluationResultRepository evaluationResultRepository,
            BattleParticipantRepository battleParticipantRepository,
            BattleRunner battleRunner,
            ExperimentAssignmentRepository experimentAssignmentRepository,
            SystemBotService systemBotService
    ) {
        this.battleQueue = battleQueue;
        this.evaluationRepository = evaluationRepository;
        this.evaluationResultRepository = evaluationResultRepository;
        this.battleParticipantRepository = battleParticipantRepository;
        this.battleRunner = battleRunner;
        this.experimentAssignmentRepository = experimentAssignmentRepository;
        this.systemBotService = systemBotService;
    }

    @Override
    public Result<?> submitAndMaybeStart(Integer assignmentId, MultipartFile model, MultipartFile config) {
        try {
            if (assignmentId == null) {
                return Result.error("assignmentId is required");
            }
            if (model == null || model.isEmpty()) {
                return Result.error("model is required");
            }
            if (config == null || config.isEmpty()) {
                return Result.error("config is required");
            }

            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null || claims.get("id") == null) {
                return Result.error("当前登录信息无效，请重新登录");
            }

            Integer currentUserId = (Integer) claims.get("id");
            Long studentId = currentUserId.longValue();

            ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
            if (assignment == null) {
                return Result.error("任务不存在");
            }

            if (assignment.getEvaluationMode() != EvaluationMode.VERSUS) {
                return Result.error("当前任务不是对战模式，不能提交对战");
            }

            String environment = assignment.getEnvironment();
            if (environment == null || environment.isBlank()) {
                return Result.error("任务未配置环境");
            }

            int games = 30;

            String baseDir = (workspace != null && !workspace.isBlank())
                    ? workspace
                    : Paths.get(System.getProperty("user.dir")).toString();

            String uuid = UUID.randomUUID().toString().replace("-", "");
            Path studentDir = Paths.get(baseDir, "uploads", String.valueOf(studentId), uuid);
            Files.createDirectories(studentDir);

            Path configPath = studentDir.resolve("config.json");
            Path modelPath = studentDir.resolve("model.pt");

            config.transferTo(configPath);
            model.transferTo(modelPath);

            String rel = Paths.get("uploads", String.valueOf(studentId), uuid)
                    .toString()
                    .replace("\\", "/");

            Submission submission = new Submission(
                    assignmentId,
                    studentId,
                    environment,
                    games,
                    rel,
                    model.getOriginalFilename(),
                    LocalDateTime.now()
            );

            int queueSize = battleQueue.enqueue(submission);
            Submission[] pair = battleQueue.tryDequeuePair(assignmentId);

            if (pair == null) {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("queued", true);
                data.put("queueSize", queueSize);
                data.put("assignmentId", assignmentId);
                data.put("environment", environment);
                data.put("games", games);
                data.put("message", "已进入匹配队列，等待另一位同学提交。");
                return Result.success(data);
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setStudentId(pair[0].getStudentId().intValue());
            evaluation.setAgentName("BATTLE");
            evaluation.setEnvironment(environment);
            evaluation.setModelId(null);
            evaluation.setAssignmentId(assignmentId);
            evaluation.setEpisodes(games);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setCreateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            BattleParticipant participant = new BattleParticipant();
            participant.setEvaluationId(evaluation.getId());
            participant.setStudent1Id(pair[0].getStudentId());
            participant.setStudent2Id(pair[1].getStudentId());
            participant.setStudent1DirRel(pair[0].getStudentDirRel());
            participant.setStudent2DirRel(pair[1].getStudentDirRel());
            participant.setOpponentType("HUMAN");
            participant.setOpponentName(null);
            participant.setDifficulty(null);
            participant.setStudent1ModelName(pair[0].getModelName());
            participant.setStudent2ModelName(pair[1].getModelName());
            battleParticipantRepository.save(participant);

            battleRunner.runBattleAsync(evaluation.getId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("queued", false);
            data.put("matched", true);
            data.put("evaluationId", evaluation.getId());
            data.put("student1", pair[0].getStudentId());
            data.put("student2", pair[1].getStudentId());
            data.put("assignmentId", assignmentId);
            data.put("environment", environment);
            data.put("games", games);
            data.put("message", "匹配成功，已启动对战评测。");
            return Result.success(data);

        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<?> submitBotAndStart(Integer assignmentId, String difficulty, MultipartFile model, MultipartFile config) {
        try {
            if (assignmentId == null) {
                return Result.error("assignmentId is required");
            }
            if (difficulty == null || difficulty.isBlank()) {
                return Result.error("difficulty is required");
            }
            if (model == null || model.isEmpty()) {
                return Result.error("model is required");
            }
            if (config == null || config.isEmpty()) {
                return Result.error("config is required");
            }

            Map<String, Object> claims = ThreadLocalUtil.get();
            if (claims == null || claims.get("id") == null) {
                return Result.error("当前登录信息无效，请重新登录");
            }

            Integer currentUserId = (Integer) claims.get("id");
            Long studentId = currentUserId.longValue();

            ExperimentAssignment assignment = experimentAssignmentRepository.findByIdAndIsDeletedFalse(assignmentId);
            if (assignment == null) {
                return Result.error("任务不存在");
            }

            if (assignment.getEvaluationMode() != EvaluationMode.VERSUS) {
                return Result.error("当前任务不是对战模式，不能进行人机对战");
            }

            String environment = assignment.getEnvironment();
            if (environment == null || environment.isBlank()) {
                return Result.error("任务未配置环境");
            }

            int games = 30;

            String baseDir = (workspace != null && !workspace.isBlank())
                    ? workspace
                    : Paths.get(System.getProperty("user.dir")).toString();

            String uuid = UUID.randomUUID().toString().replace("-", "");
            Path studentDir = Paths.get(baseDir, "uploads", String.valueOf(studentId), uuid);
            Files.createDirectories(studentDir);

            Path configPath = studentDir.resolve("config.json");
            Path modelPath = studentDir.resolve("model.pt");
            config.transferTo(configPath);
            model.transferTo(modelPath);

            String rel = Paths.get("uploads", String.valueOf(studentId), uuid)
                    .toString()
                    .replace("\\", "/");

            String botAbsDir = systemBotService.getBotAbsoluteDir(assignmentId, difficulty);
            Path botPath = Paths.get(botAbsDir);
            if (!Files.exists(botPath.resolve("config.json")) || !Files.exists(botPath.resolve("model.pt"))) {
                return Result.error("该难度人机模型尚未配置");
            }

            Evaluation evaluation = new Evaluation();
            evaluation.setStudentId(studentId.intValue());
            evaluation.setAgentName("BATTLE_BOT");
            evaluation.setEnvironment(environment);
            evaluation.setModelId(null);
            evaluation.setAssignmentId(assignmentId);
            evaluation.setEpisodes(games);
            evaluation.setStatus(EvaluationStatus.PENDING);
            evaluation.setCreateTime(LocalDateTime.now());
            evaluation.setUpdateTime(LocalDateTime.now());
            evaluationRepository.save(evaluation);

            BattleParticipant participant = new BattleParticipant();
            participant.setEvaluationId(evaluation.getId());
            participant.setStudent1Id(studentId);
            participant.setStudent2Id(null);
            participant.setStudent1DirRel(rel);
            participant.setStudent2DirRel(botAbsDir);
            participant.setOpponentType("BOT");
            participant.setOpponentName(mapDifficultyName(difficulty));
            participant.setDifficulty(difficulty.toLowerCase(Locale.ROOT));
            participant.setStudent1ModelName(model.getOriginalFilename());
            participant.setStudent2ModelName("系统模型");
            battleParticipantRepository.save(participant);

            battleRunner.runBattleAsync(evaluation.getId());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("queued", false);
            data.put("matched", true);
            data.put("evaluationId", evaluation.getId());
            data.put("assignmentId", assignmentId);
            data.put("environment", environment);
            data.put("games", games);
            data.put("message", "人机对战已启动。");
            return Result.success(data);

        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public List<BattleTaskDetailVO> listBattleTasks() {
        List<Evaluation> evaluations = new ArrayList<>();
        evaluations.addAll(evaluationRepository.findByAgentNameOrderByIdDesc("BATTLE"));
        evaluations.addAll(evaluationRepository.findByAgentNameOrderByIdDesc("BATTLE_BOT"));
        evaluations.sort((a, b) -> Long.compare(b.getId(), a.getId()));

        List<BattleTaskDetailVO> result = new ArrayList<>();

        for (Evaluation evaluation : evaluations) {
            BattleTaskDetailVO vo = new BattleTaskDetailVO();
            vo.setEvaluationId(evaluation.getId());
            vo.setStatus(buildStatusText(evaluation.getStatus()));

            Optional<BattleParticipant> participantOpt = battleParticipantRepository.findByEvaluationId(evaluation.getId());
            if (participantOpt.isPresent()) {
                BattleParticipant participant = participantOpt.get();
                vo.setStudent1Id(participant.getStudent1Id());
                vo.setStudent2Id(participant.getStudent2Id());
            }

            List<EvaluationResult> evaluationResults = evaluationResultRepository.findByEvaluationId(evaluation.getId());
            if (evaluationResults != null && !evaluationResults.isEmpty()) {
                EvaluationResult er = evaluationResults.get(0);
                vo.setWinner(er.getWinner());
                vo.setResultDir(er.getResultDir());
                vo.setWinnerText("已出结果");
            } else {
                vo.setWinner(null);
                vo.setWinnerText(buildStatusText(evaluation.getStatus()));
            }

            result.add(vo);
        }

        return result;
    }

    private String buildStatusText(EvaluationStatus status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case PENDING -> "排队中";
            case RUNNING -> "测评中";
            case FAILED -> "测评失败";
            case FINISHED -> "已完成";
        };
    }

    private String mapDifficultyName(String difficulty) {
        String d = difficulty == null ? "" : difficulty.toLowerCase(Locale.ROOT);
        return switch (d) {
            case "easy" -> "简单人机";
            case "medium" -> "中等人机";
            case "hard" -> "困难人机";
            default -> "人机";
        };
    }
}