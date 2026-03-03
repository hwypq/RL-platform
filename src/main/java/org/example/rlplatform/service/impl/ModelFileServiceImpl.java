package org.example.rlplatform.service.impl;

import org.example.rlplatform.entity.ModelFile;
import org.example.rlplatform.Repository.ModelFileRepository;
import org.example.rlplatform.service.ModelFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ModelFileServiceImpl implements ModelFileService {

    @Autowired
    private ModelFileRepository modelFileRepository;

    @Value("${evaluation.workspace:}")
    private String workspace;

    @Override
    public void create(ModelFile modelFile) {
        modelFile.setUploadTime(LocalDateTime.now());
        modelFileRepository.save(modelFile);
    }

    @Override
    public ModelFile getById(Integer id) {
        return modelFileRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    @Override
    public List<ModelFile> listByStudentId(Long studentId) {
        return modelFileRepository.findByStudentId(studentId);
    }

    @Override
    public List<ModelFile> list() {
        return modelFileRepository.findAll();
    }

    @Override
    public ModelFile uploadModelFile(MultipartFile file, Long studentId) throws IOException{

        String originalfileName = file.getOriginalFilename();
        String suffix = originalfileName.substring(originalfileName.lastIndexOf("."));
        if (suffix.isEmpty()) {
            throw new IllegalArgumentException("invalid file name");
        }
        String fileName = UUID.randomUUID().toString() + suffix;

        String baseDir = (workspace != null && !workspace.isBlank()) ? workspace : Paths.get(System.getProperty("user.dir")).toString();
        Path modelsDir = Paths.get(baseDir, "models");
        System.out.println(modelsDir.resolve(fileName));
        file.transferTo(modelsDir.resolve(fileName));
        ModelFile modelFile = new ModelFile();
        modelFile.setStudentId(studentId);
        modelFile.setFileName(fileName);
        modelFile.setFilePath("models/" + fileName);
        modelFile.setFileSize(file.getSize());
        modelFile.setUploadTime(LocalDateTime.now());
        modelFileRepository.save(modelFile);
        return modelFile;
    }
}
