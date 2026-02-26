package org.example.rlplatform.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.Repository.EvaluationRepository;
import org.example.rlplatform.service.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static java.time.LocalDateTime.now;

@Service
public class EvaluationServiceImpl implements EvaluationService {

    @Autowired
    private EvaluationRepository evaluationRepository;

    @Value("${evaluation.python:python}")
    private String pythonCmd;
    @Value("${evaluation.script:scripts/evaluate.py}")
    private String scriptPath;
    @Value("${evaluation.workspace:}")
    private String workspaceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createEvaluation(Evaluation evaluation) {
        evaluation.setCreateTime(now());
        evaluation.setUpdateTime(now());
        evaluation.setStudentId((long)5);
        evaluationRepository.save(evaluation);
    }

    @Override
    public Evaluation getEvaluationById(long id) {
        return evaluationRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public Evaluation runEvaluation(long evaluationId) {
        Evaluation evaluation = getEvaluationById(evaluationId);
        evaluation.setStatus("RUNNING");
        evaluation.setUpdateTime(now());
        evaluationRepository.save(evaluation);

        String modelName = deriveModelName(evaluation);
        String agentType = evaluation.getAgentName();

        Path cwd = Paths.get(System.getProperty("user.dir"));

        Path script = cwd.resolve(scriptPath).normalize();
        if (!script.toFile().exists()) {
            evaluation.setStatus("FAILED");
            evaluation.setResultPath("Script not found: " + script);
            evaluation.setUpdateTime(now());
            evaluationRepository.save(evaluation);
            return evaluation;
        }

        ProcessBuilder pb = new ProcessBuilder(
                pythonCmd,
                script.toString(),
                "--env", evaluation.getEnvironment(),
                "--agent", agentType,
                "--model_name", modelName,
                "--episodes", String.valueOf(evaluation.getEpisodes()),
                "--workspace", workspaceConfig
        );
        
        pb.redirectErrorStream(true);  // 合并 stderr 到 stdout，避免管道阻塞
        pb.directory(cwd.toFile());

        StringBuilder output = new StringBuilder();
        try {
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                evaluation.setStatus("FAILED");
                evaluation.setResultPath("Python execution timeout");
                evaluation.setUpdateTime(now());
                evaluationRepository.save(evaluation);
                return evaluation;
            }

            int exitCode = process.exitValue();
            String out = output.toString().trim();

            String jsonLine = extractJsonLine(out);
            JsonNode root = null;
            if (jsonLine != null) {
                try {
                    root = objectMapper.readTree(jsonLine);
                } catch (Exception ignored) { /* 解析失败时 root 仍为 null */ }
            }

            if (exitCode != 0) {
                evaluation.setStatus("FAILED");
                evaluation.setResultPath(root != null && root.has("error")
                        ? root.path("error").asText()
                        : (out.isEmpty() ? "Python script failed (no output)" : out));
                evaluation.setUpdateTime(now());
                evaluationRepository.save(evaluation);
                return evaluation;
            }

            if (root == null) {
                evaluation.setStatus("FAILED");
                evaluation.setResultPath("Invalid JSON from script. Output: " + (out.length() > 500 ? out.substring(0, 500) + "..." : out));
                evaluation.setUpdateTime(now());
                evaluationRepository.save(evaluation);
                return evaluation;
            }

            String status = root.has("status") ? root.path("status").asText() : "FAILED";
            evaluation.setStatus("FINISHED".equals(status) ? "FINISHED" : "FAILED");
            if (root.has("avgReward")) {
                evaluation.setAvgReward(root.path("avgReward").asDouble());
            }
            if (root.has("error")) {
                evaluation.setResultPath(root.path("error").asText());
            }
        } catch (Exception e) {
            evaluation.setStatus("FAILED");
            evaluation.setResultPath(e.getMessage());
        }
        evaluation.setUpdateTime(now());
        evaluationRepository.save(evaluation);
        return evaluation;
    }

    /** 若实体已设置 modelName 则使用，否则由 agentName + 环境简称推导，如 DDPG_cheetah */
    private String deriveModelName(Evaluation e) {
        if (e.getModelName() != null && !e.getModelName().isBlank()) {
            return e.getModelName();
        }
        String env = e.getEnvironment();
        String shortEnv = env.contains("-") ? env.substring(0, env.indexOf('-')).toLowerCase() : env.toLowerCase();
        return e.getAgentName() + "_" + shortEnv;
    }

    /** 从脚本输出中提取 JSON 行（最后一行形如 {...}），避免 gym/mujoco 等日志导致解析失败 */
    private String extractJsonLine(String fullOutput) {
        if (fullOutput == null || fullOutput.isEmpty()) return null;
        String[] lines = fullOutput.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.startsWith("{") && line.endsWith("}")) return line;
        }
        return null;
    }
}
