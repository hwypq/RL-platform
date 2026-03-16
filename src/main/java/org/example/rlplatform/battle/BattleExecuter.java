package org.example.rlplatform.battle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.entity.BattleParticipant;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.entity.EvaluationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Component
public class BattleExecuter {

    @Value("${battle.python:python}")
    private String pythonCmd;

    @Value("${battle.script:scripts/battle_evaluator.py}")
    private String battleScriptPath;

    @Value("${evaluation.workspace:}")
    private String workspaceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    public void executeBattle(Evaluation evaluation, BattleParticipant participant) {

        String finalPythonCmd = (pythonCmd == null || pythonCmd.isBlank()) ? "python" : pythonCmd;
        String base = (workspaceConfig != null && !workspaceConfig.isBlank())
                ? workspaceConfig
                : Paths.get(System.getProperty("user.dir")).toString();

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path script = cwd.resolve(battleScriptPath).normalize();
        if (!script.toFile().exists()) {
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage("Battle script not found: " + script);
            saveEvaluationResult(evaluation, null, null);
            return;
        }

        String resultBase = Paths.get("results", String.valueOf(evaluation.getId()), "video_0")
                .toString()
                .replace("\\", "/");

        ProcessBuilder pb = new ProcessBuilder(
                finalPythonCmd, script.toString(),
                "--workspace", base,
                "--student1_dir", participant.getStudent1DirRel(),
                "--student2_dir", participant.getStudent2DirRel(),
                "--env", evaluation.getEnvironment(),
                "--games", String.valueOf(evaluation.getEpisodes()),
                "--result_base", resultBase
        );

        pb.redirectErrorStream(true);
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
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Battle python execution timeout");
                saveEvaluationResult(evaluation, null, null);
                return;
            }

            int exitCode = process.exitValue();
            String out = output.toString().trim();

            String jsonLine = extractJsonLine(out);
            JsonNode root = null;
            if (jsonLine != null) {
                try {
                    root = objectMapper.readTree(jsonLine);
                } catch (Exception ignored) {
                }
            }

            if (exitCode != 0) {
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage(root != null && root.has("error")
                        ? root.path("error").asText()
                        : (out.isEmpty() ? "Battle script failed (no output)" : out));
                saveEvaluationResult(evaluation, root, jsonLine);
                return;
            }

            if (root == null) {
                evaluation.setStatus(EvaluationStatus.FAILED);
                evaluation.setErrorMessage("Invalid JSON from battle script. Output: " +
                        (out.length() > 500 ? out.substring(0, 500) + "..." : out));
                saveEvaluationResult(evaluation, null, jsonLine);
                return;
            }

            String status = root.has("status") ? root.path("status").asText() : EvaluationStatus.FAILED.name();
            evaluation.setStatus(EvaluationStatus.valueOf(status.toUpperCase()));

            if (root.has("error")) {
                evaluation.setErrorMessage(root.path("error").asText());
            }

            saveEvaluationResult(evaluation, root, jsonLine);

        } catch (Exception e) {
            evaluation.setStatus(EvaluationStatus.FAILED);
            evaluation.setErrorMessage(e.getMessage());
            saveEvaluationResult(evaluation, null, null);
        }
    }

    private void saveEvaluationResult(Evaluation evaluation, JsonNode root, String jsonLine) {
        try {
            EvaluationResult er = new EvaluationResult();
            er.setEvaluationId(evaluation.getId());
            er.setResult(evaluation.getStatus() == EvaluationStatus.FINISHED ? 0 : 1);

            if (jsonLine != null) {
                er.setDetailedResults(jsonLine);
            } else if (root != null) {
                er.setDetailedResults(root.toString());
            }

            if (root != null && root.has("winner")) {
                er.setWinner(root.path("winner").asInt());
            }

            if (root != null && root.has("result_dir")) {
                er.setResultDir(root.path("result_dir").asText());
            }

            evaluationResultRepository.save(er);
        } catch (Exception e) {
            evaluation.setStatus(EvaluationStatus.FAILED);
            if (evaluation.getErrorMessage() == null || evaluation.getErrorMessage().isBlank()) {
                evaluation.setErrorMessage("Save battle result failed: " + e.getMessage());
            }
        }
    }

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