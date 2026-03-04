package org.example.rlplatform.service.impl;

import org.example.rlplatform.entity.EvaluationResult;
import org.example.rlplatform.Repository.EvaluationResultRepository;
import org.example.rlplatform.service.EvaluationResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class EvaluationResultServiceImpl implements EvaluationResultService {

    @Autowired
    private EvaluationResultRepository evaluationResultRepository;

    @Value("${evaluation.workspace:}")
    private String workspace;

    @Override
    public void create(EvaluationResult evaluationResult) {
        evaluationResultRepository.save(evaluationResult);
    }

    @Override
    public EvaluationResult getById(Long id) {
        return evaluationResultRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public List<EvaluationResult> listByEvaluationId(Long evaluationId) {
        return evaluationResultRepository.findByEvaluationId(evaluationId);
    }

    @Override
    public List<EvaluationResult> list() {
        return evaluationResultRepository.findAll();
    }

    @Override
    public Resource getVideo(Long id) {
        EvaluationResult er = getById(id);
        if (er.getResultDir() == null || er.getResultDir().isBlank()) {
            throw new IllegalArgumentException("no result_dir for evaluation result id: " + id);
        }
        String base = (workspace != null && !workspace.isBlank()) ? workspace : Paths.get(System.getProperty("user.dir")).toString();
        Path videoPath = Paths.get(base, er.getResultDir() + ".mp4");
        File file = videoPath.toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("video file not found: " + videoPath);
        }
        return new FileSystemResource(file);
    }
}
