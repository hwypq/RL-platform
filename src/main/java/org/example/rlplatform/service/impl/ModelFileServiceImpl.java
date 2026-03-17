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
    public ModelFile getById(Long id) {
        return modelFileRepository.findById(id);
    }

    @Override
    public List<ModelFile> listByStudentId(Integer studentId) {
        return modelFileRepository.findByStudentId(studentId);
    }

    @Override
    public List<ModelFile> list() {
        return modelFileRepository.findAll();
    }

    @Override
    public ModelFile uploadModelFile(MultipartFile file, Integer studentId) throws IOException{

        String originalfileName = file.getOriginalFilename();
        String suffix = originalfileName.substring(originalfileName.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;

        String baseDir = (workspace != null && !workspace.isBlank()) ? workspace : Paths.get(System.getProperty("user.dir")).toString();
        Path modelsDir = Paths.get(baseDir, "models");
        if (!modelsDir.toFile().exists()) {
            modelsDir.toFile().mkdirs();
        }
        // System.out.println(modelsDir.resolve(fileName));
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

    @Override
    public ModelFile uploadModelWithConfig(MultipartFile modelFileMultipart, MultipartFile configFileMultipart, Integer studentId) throws IOException {
        if (modelFileMultipart == null || modelFileMultipart.isEmpty()) {
            throw new IllegalArgumentException("model file is required");
        }
        if (configFileMultipart == null || configFileMultipart.isEmpty()) {
            throw new IllegalArgumentException("config file is required");
        }

        String baseDir = (workspace != null && !workspace.isBlank())
                ? workspace
                : Paths.get(System.getProperty("user.dir")).toString();

        // 保存模型权重到 models 目录
        String modelOriginalName = modelFileMultipart.getOriginalFilename();
        String modelSuffix = modelOriginalName.substring(modelOriginalName.lastIndexOf("."));
        String modelFileName = UUID.randomUUID().toString() + modelSuffix;
        Path modelsDir = Paths.get(baseDir, "models");
        if (!modelsDir.toFile().exists()) {
            modelsDir.toFile().mkdirs();
        }
        Path modelPath = modelsDir.resolve(modelFileName);
        modelFileMultipart.transferTo(modelPath);

        // 保存配置文件到 configs 目录
        String configOriginalName = configFileMultipart.getOriginalFilename();
        String configSuffix = configOriginalName.substring(configOriginalName.lastIndexOf("."));
        String configFileName = UUID.randomUUID().toString() + configSuffix;
        Path configsDir = Paths.get(baseDir, "configs");
        if (!configsDir.toFile().exists()) {
            configsDir.toFile().mkdirs();
        }
        Path configPath = configsDir.resolve(configFileName);
        configFileMultipart.transferTo(configPath);

        ModelFile modelFile = new ModelFile();
        modelFile.setStudentId(studentId);
        modelFile.setFileName(modelFileName);
        modelFile.setFilePath("models/" + modelFileName);
        modelFile.setFileSize(modelFileMultipart.getSize());
        modelFile.setConfigPath("configs/" + configFileName);
        modelFile.setUploadTime(LocalDateTime.now());
        modelFileRepository.save(modelFile);
        return modelFile;
    }
}
