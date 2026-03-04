package org.example.rlplatform.evaluation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.rlplatform.entity.Evaluation;
import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.service.ModelFileService;
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
public class EvaluationExecuter {

    @Value("${evaluation.python:python}")
    private String pythonCmd;

    @Value("${evaluation.script:scripts/evaluate.py}")
    private String scriptPath;

    @Value("${evaluation.workspace:}")
    private String workspaceConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Autowired
    private ModelFileService modelFileService;

    public void execute(Evaluation evaluation) {
        String modelName = evaluation.getModelId() != null ? modelFileService.getById(evaluation.getModelId()).getFileName() : null;
        String agentType = evaluation.getAgentName();

        Path cwd = Paths.get(System.getProperty("user.dir"));
        Path script = cwd.resolve(scriptPath).normalize();
        if (!script.toFile().exists()) {
            evaluation.setStatus("FAILED");
            evaluation.setErrorMessage("Script not found: " + script);
            saveEvaluationResult(evaluation, null, null);
            return ;
        }

        ProcessBuilder pb = new ProcessBuilder(
                pythonCmd, script.toString(),
                "--env", evaluation.getEnvironment(),
                "--agent", agentType,
                "--model_name", modelName,
                "--episodes", String.valueOf(evaluation.getEpisodes()),
                "--workspace", workspaceConfig,
                "--render_video"
                // "--realtime_render"
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
                evaluation.setStatus("FAILED");
                evaluation.setErrorMessage("Python execution timeout");
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
                    // 解析失败时 root 仍为 null
                }
            }

            if (exitCode != 0) {
                evaluation.setStatus("FAILED");
                evaluation.setErrorMessage(root != null && root.has("error")
                        ? root.path("error").asText()
                        : (out.isEmpty() ? "Python script failed (no output)" : out));
                saveEvaluationResult(evaluation, root, jsonLine);
                return;
            }

            if (root == null) {
                evaluation.setStatus("FAILED");
                evaluation.setErrorMessage("Invalid JSON from script. Output: " +
                        (out.length() > 500 ? out.substring(0, 500) + "..." : out));
                saveEvaluationResult(evaluation, null, jsonLine);
                return;
            }

            String status = root.has("status") ? root.path("status").asText() : "FAILED";
            evaluation.setStatus("FINISHED".equals(status) ? "FINISHED" : "FAILED");
            if (root.has("error")) {
                evaluation.setErrorMessage(root.path("error").asText());
            }
            saveEvaluationResult(evaluation, root, jsonLine);
        } catch (Exception e) {
            evaluation.setStatus("FAILED");
            evaluation.setErrorMessage(e.getMessage());
            saveEvaluationResult(evaluation, null, null);
        }
    }


    private void saveEvaluationResult(Evaluation evaluation, JsonNode root, String jsonLine) {
        try {
            EvaluationResult er = new EvaluationResult();
            er.setEvaluationId(evaluation.getId());
            er.setResult("FINISHED".equals(evaluation.getStatus()) ? 1 : 0);
//            System.out.println("Saving EvaluationResult..." + root.toString());
            if (jsonLine != null) {
                er.setDetailedResults(jsonLine);
            } else if (root != null) {
                er.setDetailedResults(root.toString());
            }
            if (root != null && root.has("result_dir")) {
                er.setResultDir(root.path("result_dir").asText());
            }
            evaluationResultRepository.save(er);
        } catch (Exception e) {
            evaluation.setStatus("FAILED");
            if (evaluation.getErrorMessage() == null || evaluation.getErrorMessage().isBlank()) {
                evaluation.setErrorMessage("Save result failed: " + e.getMessage());
            }
        }
    }

    /** 从脚本输出中提取 JSON 行 */
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
