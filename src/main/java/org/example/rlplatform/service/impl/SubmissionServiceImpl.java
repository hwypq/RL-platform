package org.example.rlplatform.service.impl;

import org.example.rlplatform.Repository.BattleParticipantRepository;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.Repository.ExperimentAssignmentRepository;
import org.example.rlplatform.Repository.ModelFileRepository;
import org.example.rlplatform.entity.*;
import org.example.rlplatform.service.SubmissionService;
import org.example.rlplatform.service.UserService;
import org.example.rlplatform.utils.ThreadLocalUtil;
import org.example.rlplatform.vo.SubmissionHistoryVO;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private final EvaluationRepository evaluationRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final ExperimentAssignmentRepository experimentAssignmentRepository;
    private final ModelFileRepository modelFileRepository;
    private final UserService userService;
    private final BattleParticipantRepository battleParticipantRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public SubmissionServiceImpl(
            EvaluationRepository evaluationRepository,
            EvaluationResultRepository evaluationResultRepository,
            ExperimentAssignmentRepository experimentAssignmentRepository,
            ModelFileRepository modelFileRepository,
            UserService userService,
            BattleParticipantRepository battleParticipantRepository
    ) {
        this.evaluationRepository = evaluationRepository;
        this.evaluationResultRepository = evaluationResultRepository;
        this.experimentAssignmentRepository = experimentAssignmentRepository;
        this.modelFileRepository = modelFileRepository;
        this.userService = userService;
        this.battleParticipantRepository = battleParticipantRepository;
    }

    @Override
    public List<SubmissionHistoryVO> listMySubmissions() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer studentId = (Integer) claims.get("id");
        return buildByStudentId(studentId);
    }

    @Override
    public List<SubmissionHistoryVO> listStudentSubmissions(Integer studentId) {
        return buildByStudentId(studentId);
    }

    @Override
    public List<SubmissionHistoryVO> listAssignmentSubmissions(Integer assignmentId) {
        List<Evaluation> evaluations = evaluationRepository.findByAssignmentIdOrderByCreateTimeDesc(assignmentId);
        return buildVos(evaluations, null);
    }

    private List<SubmissionHistoryVO> buildByStudentId(Integer studentId) {
        List<Evaluation> directEvaluations = evaluationRepository.findByStudentIdOrderByCreateTimeDesc(studentId);

        List<BattleParticipant> relatedBattles = battleParticipantRepository
                .findByStudent1IdOrStudent2IdOrderByIdDesc(studentId.longValue(), studentId.longValue());

        Set<Long> evaluationIdSet = new LinkedHashSet<>();
        for (Evaluation evaluation : directEvaluations) {
            evaluationIdSet.add(evaluation.getId());
        }
        for (BattleParticipant participant : relatedBattles) {
            evaluationIdSet.add(participant.getEvaluationId());
        }

        List<Evaluation> merged = evaluationRepository.findAllById(evaluationIdSet);
        merged.sort((a, b) -> {
            if (a.getCreateTime() == null && b.getCreateTime() == null) return 0;
            if (a.getCreateTime() == null) return 1;
            if (b.getCreateTime() == null) return -1;
            return b.getCreateTime().compareTo(a.getCreateTime());
        });

        return buildVos(merged, studentId);
    }

    private List<SubmissionHistoryVO> buildVos(List<Evaluation> evaluations, Integer currentStudentId) {
        if (evaluations == null || evaluations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> assignmentIds = evaluations.stream()
                .map(Evaluation::getAssignmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Integer> modelIds = evaluations.stream()
                .map(Evaluation::getModelId)
                .filter(Objects::nonNull)
                .map(Long::intValue)
                .distinct()
                .toList();

        List<Long> evaluationIds = evaluations.stream()
                .map(Evaluation::getId)
                .filter(Objects::nonNull)
                .toList();

        Map<Integer, ExperimentAssignment> assignmentMap = experimentAssignmentRepository.findAllById(assignmentIds)
                .stream()
                .collect(Collectors.toMap(ExperimentAssignment::getId, item -> item));

        Map<Integer, ModelFile> modelFileMap = modelFileRepository.findAllById(modelIds)
                .stream()
                .collect(Collectors.toMap(item -> item.getId().intValue(), item -> item));

        Map<Long, EvaluationResult> resultMap = new HashMap<>();
        for (EvaluationResult result : evaluationResultRepository.findByEvaluationIdIn(evaluationIds)) {
            resultMap.put(result.getEvaluationId(), result);
        }

        Map<Long, BattleParticipant> participantMap = battleParticipantRepository.findByEvaluationIdIn(evaluationIds)
                .stream()
                .collect(Collectors.toMap(BattleParticipant::getEvaluationId, item -> item));

        List<SubmissionHistoryVO> voList = new ArrayList<>();

        for (Evaluation evaluation : evaluations) {
            SubmissionHistoryVO vo = new SubmissionHistoryVO();
            vo.setEvaluationId(evaluation.getId());
            vo.setAssignmentId(evaluation.getAssignmentId());
            vo.setStudentId(evaluation.getStudentId());

            User student = userService.findByIdAndIsDeletedFalse(evaluation.getStudentId());
            vo.setStudentName(student != null ? student.getUsername() : "未知学生");

            ExperimentAssignment assignment = assignmentMap.get(evaluation.getAssignmentId());
            if (assignment != null) {
                vo.setTaskTitle(assignment.getTitle());
                vo.setTaskMode(mapTaskMode(assignment.getEvaluationMode()));
            } else {
                vo.setTaskTitle("未知任务");
                vo.setTaskMode("未知模式");
            }

            ModelFile modelFile = null;
            if (evaluation.getModelId() != null) {
                modelFile = modelFileMap.get(evaluation.getModelId().intValue());
            }

            BattleParticipant participant = participantMap.get(evaluation.getId());
            vo.setModelName(buildModelName(evaluation, modelFile, participant, currentStudentId));

            if (evaluation.getCreateTime() != null) {
                vo.setSubmitTime(evaluation.getCreateTime().format(DATETIME_FORMATTER));
            } else {
                vo.setSubmitTime("--");
            }

            vo.setStatus(mapStatus(evaluation.getStatus()));
            vo.setOpponentName(buildOpponentName(participant, currentStudentId));

            EvaluationResult result = resultMap.get(evaluation.getId());
            if (result != null) {
                vo.setEvaluationResultId(result.getId());
                vo.setHasVideo(result.getResultDir() != null && !result.getResultDir().isBlank());
                vo.setResultText(buildResultText(evaluation, result, participant, currentStudentId));
            } else {
                vo.setEvaluationResultId(null);
                vo.setHasVideo(false);
                vo.setResultText("-");
            }

            voList.add(vo);
        }

        return voList;
    }

    private String mapTaskMode(EvaluationMode mode) {
        if (mode == null) return "未知模式";
        return switch (mode) {
            case SINGLE -> "单人模式";
            case VERSUS -> "对战模式";
            case TEAM -> "团队锦标赛";
        };
    }

    private String mapStatus(EvaluationStatus status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case PENDING -> "待开始";
            case RUNNING -> "测评中";
            case FINISHED -> "已完成";
            case FAILED -> "测评失败";
        };
    }

    private String buildModelName(Evaluation evaluation, ModelFile modelFile, BattleParticipant participant, Integer currentStudentId) {
        if (modelFile != null && modelFile.getFileName() != null && !modelFile.getFileName().isBlank()) {
            return modelFile.getFileName();
        }

        if (participant == null) {
            return "--";
        }

        if (currentStudentId != null) {
            if (participant.getStudent1Id() != null && participant.getStudent1Id().intValue() == currentStudentId) {
                return participant.getStudent1ModelName() == null || participant.getStudent1ModelName().isBlank()
                        ? "model.pt"
                        : participant.getStudent1ModelName();
            }

            if (participant.getStudent2Id() != null && participant.getStudent2Id().intValue() == currentStudentId) {
                return participant.getStudent2ModelName() == null || participant.getStudent2ModelName().isBlank()
                        ? "model.pt"
                        : participant.getStudent2ModelName();
            }
        }

        if (participant.getStudent1ModelName() != null && !participant.getStudent1ModelName().isBlank()) {
            return participant.getStudent1ModelName();
        }

        if (participant.getStudent2ModelName() != null && !participant.getStudent2ModelName().isBlank()) {
            return participant.getStudent2ModelName();
        }

        return "--";
    }

    private String buildOpponentName(BattleParticipant participant, Integer currentStudentId) {
        if (participant == null) {
            return "无";
        }

        if ("BOT".equalsIgnoreCase(participant.getOpponentType())) {
            return participant.getOpponentName() == null ? "人机" : participant.getOpponentName();
        }

        if ("HUMAN".equalsIgnoreCase(participant.getOpponentType())) {
            if (currentStudentId == null) {
                return "真人对手";
            }

            if (participant.getStudent1Id() != null && participant.getStudent1Id().intValue() == currentStudentId) {
                if (participant.getStudent2Id() == null) return "真人对手";
                User other = userService.findByIdAndIsDeletedFalse(participant.getStudent2Id().intValue());
                return other != null ? other.getUsername() : "真人对手";
            }

            if (participant.getStudent2Id() != null && participant.getStudent2Id().intValue() == currentStudentId) {
                User other = userService.findByIdAndIsDeletedFalse(participant.getStudent1Id().intValue());
                return other != null ? other.getUsername() : "真人对手";
            }

            return "真人对手";
        }

        return "无";
    }

    private String buildResultText(Evaluation evaluation, EvaluationResult result, BattleParticipant participant, Integer currentStudentId) {
        if (evaluation.getStatus() == EvaluationStatus.PENDING || evaluation.getStatus() == EvaluationStatus.RUNNING) {
            return "-";
        }

        if (evaluation.getStatus() == EvaluationStatus.FAILED ||
                (result != null && result.getResult() != null && result.getResult() == 1)) {
            return "失败";
        }

        if (participant == null) {
            return "已完成";
        }

        if (result == null || result.getWinner() == null) {
            return "已完成";
        }

        if (result.getWinner() == 0) {
            return "平局";
        }

        if (currentStudentId == null) {
            return "已出结果";
        }

        if (participant.getStudent1Id() != null && participant.getStudent1Id().intValue() == currentStudentId) {
            return result.getWinner() == 1 ? "获胜" : "失败";
        }

        if (participant.getStudent2Id() != null && participant.getStudent2Id().intValue() == currentStudentId) {
            return result.getWinner() == 2 ? "获胜" : "失败";
        }

        return "已出结果";
    }
}